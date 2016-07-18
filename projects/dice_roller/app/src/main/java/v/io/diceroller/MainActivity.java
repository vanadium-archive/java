// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package v.io.diceroller;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import io.v.syncbase.Collection;
import io.v.syncbase.Database;
import io.v.syncbase.Syncbase;
import io.v.syncbase.WatchChange;
import io.v.syncbase.exception.SyncbaseException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DiceRoller";
    private static final String RESULT_KEY = "result";

    private static final String CLOUD_NAME =
            "/(dev.v.io:r:vprod:service:mounttabled)@ns.dev.v.io:8101/sb/syncbased-24204641";
    private static final String CLOUD_ADMIN = "dev.v.io:r:allocator:us:x:syncbased-24204641";
    private static final String MOUNT_POINT = "/ns.dev.v.io:8101/tmp/diceroller/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String rootDir = getDir("syncbase", Context.MODE_PRIVATE).getAbsolutePath();
            Syncbase.Options options =
                    Syncbase.Options.cloudBuilder(rootDir, CLOUD_NAME, CLOUD_ADMIN)
                            .setMountPoint(MOUNT_POINT).build();
            Syncbase.init(options);
        } catch (SyncbaseException e) {
            Log.e(TAG, e.toString());
        }

        Syncbase.loginAndroid(this, new LoginCallback());
    }

    @Override
    protected void onDestroy() {
        Syncbase.shutdown();
        super.onDestroy();
    }

    private class LoginCallback implements Syncbase.LoginCallback {
        @Override
        public void onSuccess() {
            Log.i(TAG, "LoginCallback: onSuccess");

            try {
                final Collection userdata = Syncbase.database().getUserdataCollection();

                // On dice roll, put a random number into the userdata collection under RESULT_KEY.
                final Button button = (Button) findViewById(R.id.buttonRoll);
                if (button == null) {
                    Log.e(TAG, "Resource not found: " + R.id.buttonRoll);
                } else {
                    button.setEnabled(true);
                    button.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            int randomNumber = new Random().nextInt(6) + 1;
                            try {
                                userdata.put(RESULT_KEY, randomNumber);
                            } catch (SyncbaseException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }

                Syncbase.database().addWatchChangeHandler(new Database.WatchChangeHandler() {
                    @Override
                    public void onInitialState(Iterator<WatchChange> values) {
                        onChange(values);
                    }

                    @Override
                    public void onChangeBatch(Iterator<WatchChange> changes) {
                        onChange(changes);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.toString());
                    }

                    private void onChange(Iterator<WatchChange> changes) {
                        while (changes.hasNext()) {
                            WatchChange watchChange = changes.next();
                            Log.i(TAG, watchChange.toString());
                            if (watchChange.getCollectionId().getName().equals(
                                    Syncbase.USERDATA_NAME) &&
                                    watchChange.getEntityType() == WatchChange.EntityType.ROW &&
                                    watchChange.getChangeType() == WatchChange.ChangeType.PUT &&
                                    watchChange.getRowKey().equals(RESULT_KEY)) {
                                try {
                                    updateResult(watchChange.getValue(Integer.class));
                                } catch (SyncbaseException e) {
                                    Log.e(TAG, e.toString());
                                }
                            }
                        }
                    }
                });
            } catch (SyncbaseException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public void onError(Throwable e) {
            Log.e(TAG, "LoginCallback: onError: " + e.toString());
        }
    }

    private void updateResult(final int newValue) {
        Log.i(TAG, "newValue: " + newValue);
        final TextView result = (TextView) findViewById(R.id.textViewResult);
        result.setText(String.valueOf(newValue));
    }
}

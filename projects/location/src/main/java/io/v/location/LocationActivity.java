package io.v.location;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LocationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        startService(new Intent(this, LocationService.class));
    }
}

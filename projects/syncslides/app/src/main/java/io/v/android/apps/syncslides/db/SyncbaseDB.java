// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.impl.google.naming.NamingUtil;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.DatabaseCore;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class SyncbaseDB implements DB {

    private static final String TAG = "SyncbaseDB";
    /**
     * The intent result code for when we get blessings from the account manager.
     * The value must not conflict with any other blessing result codes.
     */
    private static final int BLESSING_REQUEST = 200;
    private static final String SYNCBASE_APP = "syncslides";
    private static final String SYNCBASE_DB = "syncslides";
    private static final String DECKS_TABLE = "Decks";
    private static final String NOTES_TABLE = "Notes";

    // If SyncbaseDB needs to start the AccountManager to get blessings, it will not
    // finish its initialization, but the fragment that is trying to initialize
    // DB will continue to load and use DB.  That fragment will reload when the
    // AccountManager is finished, so if mInitialized is false, any DB methods should
    // return noop values.
    private boolean mInitialized = false;
    private Permissions mPermissions;
    private Context mContext;
    private VContext mVContext;
    private Database mDB;
    private Table mDecks;
    private Table mNotes;

    SyncbaseDB(Context context) {
        mContext = context;
    }

    @Override
    public void init(Activity activity) {
        if (mInitialized) {
            return;
        }
        mVContext = V.init(mContext);
        try {
            mVContext = V.withListenSpec(
                    mVContext, V.getListenSpec(mVContext).withProxy("proxy"));
        } catch (VException e) {
            handleError("Couldn't setup vanadium proxy: " + e.getMessage());
        }
        // TODO(kash): Set proper ACLs.
        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        mPermissions = new Permissions(ImmutableMap.of(
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));
        getBlessings(activity);
    }

    private void getBlessings(Activity activity) {
        Blessings blessings = null;
        try {
            // See if there are blessings stored in shared preferences.
            blessings = BlessingsManager.getBlessings(mContext);
        } catch (VException e) {
            handleError("Error getting blessings from shared preferences " + e.getMessage());
        }
        if (blessings == null) {
            // Request new blessings from the account manager via an intent.  This intent
            // will call back to onActivityResult() which will continue with
            // configurePrincipal().
            refreshBlessings(activity);
            return;
        }
        configurePrincipal(blessings);
    }

    private void refreshBlessings(Activity activity) {
        Intent intent = BlessingService.newBlessingIntent(mContext);
        activity.startActivityForResult(intent, BLESSING_REQUEST);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLESSING_REQUEST) {
            try {
                byte[] blessingsVom = BlessingService.extractBlessingReply(resultCode, data);
                Blessings blessings = (Blessings) VomUtil.decode(blessingsVom, Blessings.class);
                BlessingsManager.addBlessings(mContext, blessings);
                Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show();
                configurePrincipal(blessings);
            } catch (BlessingCreationException e) {
                handleError("Couldn't create blessing: " + e.getMessage());
            } catch (VException e) {
                handleError("Couldn't derive blessing: " + e.getMessage());
            }
            return true;
        }
        return false;
    }

    private void configurePrincipal(Blessings blessings) {
        // TODO(kash): Probably better to do this not in the UI thread.
        try {
            VPrincipal p = V.getPrincipal(mVContext);
            p.blessingStore().setDefaultBlessings(blessings);
            p.blessingStore().set(blessings, new BlessingPattern("..."));
            p.addToRoots(blessings);
        } catch (VException e) {
            handleError(String.format(
                    "Couldn't set local blessing %s: %s", blessings, e.getMessage()));
            return;
        }
        setupSyncbase(blessings);
    }

    // TODO(kash): Run this in an AsyncTask so it doesn't block the UI.
    private void setupSyncbase(Blessings blessings) {
        // Prepare the syncbase storage directory.
        File storageDir = new File(mContext.getFilesDir(), "syncbase");
        storageDir.mkdirs();

        try {
            mVContext = SyncbaseServer.withNewServer(mVContext, new SyncbaseServer.Params()
                    .withPermissions(mPermissions)
                    .withStorageRootDir(storageDir.getAbsolutePath()));
        } catch (SyncbaseServer.StartException e) {
            handleError("Couldn't start syncbase server");
            return;
        }
        try {
            Server syncbaseServer = V.getServer(mVContext);
            String serverName = "/" + syncbaseServer.getStatus().getEndpoints()[0];
            SyncbaseService service = Syncbase.newService(serverName);
            SyncbaseApp app = service.getApp(SYNCBASE_APP);
            if (!app.exists(mVContext)) {
                app.create(mVContext, mPermissions);
            }
            mDB = app.getNoSqlDatabase(SYNCBASE_DB, null);
            if (!mDB.exists(mVContext)) {
                mDB.create(mVContext, mPermissions);
            }
            mDecks = mDB.getTable(DECKS_TABLE);
            if (!mDecks.exists(mVContext)) {
                mDecks.create(mVContext, mPermissions);
            }
            mNotes = mDB.getTable(NOTES_TABLE);
            if (!mNotes.exists(mVContext)) {
                mNotes.create(mVContext, mPermissions);
            }
            importDecks();
        } catch (VException e) {
            handleError("Couldn't setup syncbase service: " + e.getMessage());
            return;
        }
        mInitialized = true;
    }

    private byte[] getThumbnailBytes(int resourceId) {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        return stream.toByteArray();
    }

    @Override
    public void askQuestion(String identity) {

    }

    @Override
    public void getQuestionerList(String deckId, QuestionerListener callback) {

    }

    @Override
    public DBList<io.v.android.apps.syncslides.model.Deck> getDecks() {
        if (!mInitialized) {
            return new NoopList<io.v.android.apps.syncslides.model.Deck>();
        }
        return new DeckList(mVContext, mDB);
    }

    private static class NoopList<E> implements DBList<E> {
        @Override
        public int getItemCount() {
            return 0;
        }

        @Override
        public E get(int i) {
            return null;
        }

        @Override
        public void setListener(Listener listener) {
        }

        @Override
        public void discard() {
        }
    }

    private static class DeckList implements DBList {

        private final VContext mVContext;
        private final Database mDB;
        private final Handler mHandler;
        private final Thread mThread;
        private List<SyncbaseDeck> mDecks;
        private Listener mListener;

        public DeckList(VContext vContext, Database db) {
            mVContext = vContext;
            mDB = db;
            mHandler = new Handler(Looper.getMainLooper());
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchData();
                }
            });
            mThread.start();
        }

        private void fetchData() {
            // TODO(kash): Uncomment this once we've figured out why performance is so bad.
            //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            try {
                final List<SyncbaseDeck> decks = Lists.newArrayList();
                DatabaseCore.ResultStream stream = mDB.exec(mVContext,
                        "SELECT k, v FROM Decks WHERE Type(v) like \"%Deck\"");
                // TODO(kash): Abort execution if interrupted.  Perhaps we should derive
                // a new VContext so it can be cancelled.
                for (List<VdlAny> row : stream) {
                    if (row.size() != 2) {
                        throw new VException("Wrong number of columns: " + row.size());
                    }
                    String key = (String) row.get(0).getElem();
                    Log.i(TAG, "Fetched deck " + key);
                    Deck deck = (Deck) row.get(1).getElem();
                    decks.add(new SyncbaseDeck(key, deck.getTitle(), deck.getThumbnail()));
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDecks = decks;
                        // TODO(kash): It would be better to notify that the whole
                        // data set changed.  Let's look at performance first.  Maybe we want to
                        // post each item as it arrives from syncbase instead of collecting
                        // them all before posting.
                        mListener.notifyItemInserted(0);
                    }
                });
            } catch (VException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public int getItemCount() {
            if (mDecks != null) {
                return mDecks.size();
            }
            return 0;
        }

        @Override
        public io.v.android.apps.syncslides.model.Deck get(int i) {
            if (mDecks != null) {
                return mDecks.get(i);
            }
            return null;
        }

        @Override
        public void setListener(Listener listener) {
            mListener = listener;
        }

        @Override
        public void discard() {
            mThread.interrupt();
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private static class SyncbaseDeck implements io.v.android.apps.syncslides.model.Deck {

        private final String mKey;
        private final String mTitle;
        private final Bitmap mThumbnail;

        public SyncbaseDeck(String key, String title, byte[] thumbnail) {
            mKey = key;
            mTitle = title;
            mThumbnail = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
        }

        @Override
        public Bitmap getThumb() {
            return mThumbnail;
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        public String getId() {
            return mKey;
        }
    }

    @Override
    public DBList<io.v.android.apps.syncslides.model.Slide> getSlides(String deckId) {
        if (!mInitialized) {
            return new NoopList<>();
        }
        return new SlideList(mVContext, mDB, deckId);
    }

    private static class SlideList implements DBList {

        private final VContext mVContext;
        private final Database mDB;
        private final Handler mHandler;
        private final Thread mThread;
        private final String mDeckId;
        private List<SyncbaseSlide> mSlides;
        private Listener mListener;

        public SlideList(VContext vContext, Database db, String deckId) {
            Log.i(TAG, "Fetching slides for " + deckId);
            mVContext = vContext;
            mDB = db;
            mDeckId = deckId;
            mSlides = Lists.newArrayList();
            mHandler = new Handler(Looper.getMainLooper());
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchData();
                }
            });
            mThread.start();
        }

        private void fetchData() {
            // TODO(kash): Uncomment this once we've figured out why performance is so bad.
            //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            try {
                BatchDatabase batch = mDB.beginBatch(mVContext, null);
                Table table = batch.getTable(NOTES_TABLE);

                String query = "SELECT k, v FROM Decks WHERE Type(v) LIKE \"%Slide\" " +
                        "AND k LIKE \"" + NamingUtil.join(mDeckId, "slides") + "%\"";
                DatabaseCore.ResultStream stream = batch.exec(mVContext, query);
                // TODO(kash): Abort execution if interrupted.  Perhaps we should derive
                // a new VContext so it can be cancelled.
                for (List<VdlAny> row : stream) {
                    if (row.size() != 2) {
                        throw new VException("Wrong number of columns: " + row.size());
                    }
                    String key = (String) row.get(0).getElem();
                    Log.i(TAG, "Fetched slide " + key);
                    Slide slide = (Slide) row.get(1).getElem();
                    Note note = (Note) table.get(mVContext, key, Note.class);
                    final SyncbaseSlide newSlide = new SyncbaseSlide(
                            key, slide.getThumbnail(), note.getText());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSlides.add(newSlide);
                            mListener.notifyItemInserted(mSlides.size() - 1);
                        }
                    });
                }
            } catch (VException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return mSlides.size();
        }

        @Override
        public io.v.android.apps.syncslides.model.Slide get(int i) {
            return mSlides.get(i);
        }

        @Override
        public void setListener(Listener listener) {
            mListener = listener;
        }

        @Override
        public void discard() {
            mThread.interrupt();
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private static class SyncbaseSlide implements io.v.android.apps.syncslides.model.Slide {


        private final Bitmap mThumbnail;
        private final String mNotes;

        public SyncbaseSlide(String key, byte[] thumbnail, String notes) {
            mThumbnail = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
            mNotes = notes;
        }

        @Override
        public Bitmap getImage() {
            return mThumbnail;
        }

        @Override
        public String getNotes() {
            return mNotes;
        }
    }


    @Override
    public void getSlides(String deckId, SlidesCallback callback) {

    }

    private void handleError(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }

    private void importDecks() {
        importDeck("deckId1", "Car Business", R.drawable.thumb_deck1);
        importDeck("deckId2", "Baku Discovery", R.drawable.thumb_deck2);
        importDeck("deckId3", "Vanadium", R.drawable.thumb_deck3);
    }

    private static final int[] SLIDEDRAWABLES = new int[]{
            R.drawable.slide1_thumb,
            R.drawable.slide2_thumb,
            R.drawable.slide3_thumb,
            R.drawable.slide4_thumb,
            R.drawable.slide5_thumb,
            R.drawable.slide6_thumb,
            R.drawable.slide7_thumb,
            R.drawable.slide8_thumb,
            R.drawable.slide9_thumb,
            R.drawable.slide10_thumb,
            R.drawable.slide11_thumb
    };
    private final String[] SLIDENOTES = {
            "This is the teaser slide. It should be memorable and descriptive of what your " +
                    "company is trying to do", "",
            "The bigger the pain, the better",
            "How do you solve this problem? How is it better or different from existing solutions?",
            "Demo the product", "", "[REDACTED]",
            "They may have tractor traction, but we still have the competitive advantage",
            "I'm not a businessman. I'm a business, man", "There is no 'i' on this slide",
            "Sqrt(all evil)"};

    private void importDeck(String prefix, String title, int resourceId) {
        Log.i(TAG, String.format("importing %s, %s, %d", prefix, title, resourceId));
        try {
            mDecks.deleteRange(mVContext, RowRange.prefix(prefix));
            if (!mDecks.getRow(prefix).exists(mVContext)) {
                mDecks.put(
                        mVContext,
                        prefix,
                        new Deck(title, getThumbnailBytes(resourceId)),
                        Deck.class);
            }
            for (int i = 0; i < SLIDENOTES.length; i++) {
                String key = NamingUtil.join(prefix, "slides", String.format("%04d", i));
                Log.i(TAG, "Adding slide " + key);
                if (!mDecks.getRow(key).exists(mVContext)) {
                    Log.i(TAG, "Putting bytes: " + getThumbnailBytes(SLIDEDRAWABLES[i]).length);
                    mDecks.put(
                            mVContext,
                            key,
                            new Slide(getThumbnailBytes(SLIDEDRAWABLES[i])),
                            Slide.class);
                }
                Log.i(TAG, "Adding notes");
                mNotes.put(mVContext, key, new Note(SLIDENOTES[i]), Note.class);
                mNotes.put(
                        mVContext,
                        NamingUtil.join(prefix, "LastViewed"),
                        System.currentTimeMillis(),
                        Long.class);
            }
        } catch (VException e) {
            handleError(e.toString());
        }
    }

}

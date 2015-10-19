// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.joda.time.Duration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.discovery.V23Manager;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckImpl;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.apps.syncslides.model.NoopList;
import io.v.android.apps.syncslides.model.Slide;
import io.v.android.apps.syncslides.model.SlideImpl;
import io.v.android.v23.V;
import io.v.impl.google.naming.NamingUtil;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncgroupPrefix;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.DatabaseCore;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Stream;
import io.v.v23.syncbase.nosql.Syncgroup;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.Errors;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class SyncbaseDB implements DB {

    private static final String TAG = "SyncbaseDB";
    private static final String SYNCBASE_APP = "syncslides";
    private static final String SYNCBASE_DB = "syncslides";
    private static final String DECKS_TABLE = "Decks";
    private static final String NOTES_TABLE = "Notes";
    static final String PRESENTATIONS_TABLE = "Presentations";
    static final String CURRENT_SLIDE = "CurrentSlide";
    static final String QUESTIONS = "questions";
    private static final String SYNCGROUP_PRESENTATION_DESCRIPTION = "Live Presentation";

    private boolean mInitialized = false;
    private Handler mHandler;
    private Permissions mPermissions;
    private Context mContext;
    private VContext mVContext;
    private Database mDB;
    private Table mDecks;
    private Table mNotes;
    private Table mPresentations;
    private final Map<String, CurrentSlideWatcher> mCurrentSlideWatchers;
    private final Map<String, QuestionWatcher> mQuestionWatchers;
    private Server mSyncbaseServer;

    SyncbaseDB(Context context) {
        mContext = context;
        mCurrentSlideWatchers = Maps.newHashMap();
        mQuestionWatchers = Maps.newHashMap();
    }

    @Override
    public void init(Activity activity) {
        Log.d(TAG, "init");
        if (mInitialized) {
            Log.d(TAG, "already initialized");
            return;
        }
        mHandler = new Handler(Looper.getMainLooper());
        // TODO(kash): Set proper ACLs.
        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        mPermissions = new Permissions(ImmutableMap.of(
                Constants.RESOLVE.getValue(), acl,
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));

        // If blessings aren't in place, the fragment that called this
        // initialization may continue to load and use DB, but nothing will
        // work so DB methods should return noop values.  It's assumed that
        // the calling fragment will send the user to the AccountManager,
        // accept blessings on return, then re-call this init.
        if (V23Manager.Singleton.get().isBlessed()) {
            Log.d(TAG, "no blessings.");
            return;
        }
        mVContext = V23Manager.Singleton.get().getVContext();
        setupSyncbase();
    }

    // TODO(kash): Run this in an AsyncTask so it doesn't block the UI.
    private void setupSyncbase() {
        // Prepare the syncbase storage directory.
        File storageDir = new File(mContext.getFilesDir(), "syncbase");
        storageDir.mkdirs();

        try {
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            String id = tm.getDeviceId();
            if (id == null) {
                // NOTE(spetrovic): on a tablet, there is no TelephonyManager, so we try something
                // else.
                id = Settings.Secure.getString(
                        mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            mVContext = SyncbaseServer.withNewServer(mVContext, new SyncbaseServer.Params()
                    .withPermissions(mPermissions)
                    .withName(V23Manager.syncName(id))
                    .withStorageRootDir(storageDir.getAbsolutePath()));
        } catch (SyncbaseServer.StartException e) {
            handleError("Couldn't start syncbase server");
            return;
        }
        try {
            mSyncbaseServer = V.getServer(mVContext);
            Log.i(TAG, "Endpoints: " + Arrays.toString(mSyncbaseServer.getStatus().getEndpoints()));
            String serverName = "/" + mSyncbaseServer.getStatus().getEndpoints()[0];
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
            mPresentations = mDB.getTable(PRESENTATIONS_TABLE);
            if (!mPresentations.exists(mVContext)) {
                mPresentations.create(mVContext, mPermissions);
            }
            importDecks();
        } catch (VException e) {
            handleError("Couldn't setup syncbase service: " + e.getMessage());
            return;
        }
        mInitialized = true;
    }

    @Override
    public void createPresentation(final String deckId,
                                   final Callback<CreatePresentationResult> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                createPresentationRunnable(deckId, callback);
            }
        }).start();
    }

    private void createPresentationRunnable(final String deckId,
                                            final Callback<CreatePresentationResult> callback) {
        //final String presentationId = UUID.randomUUID().toString();
        final String presentationId = "randomPresentationId1";
        String prefix = NamingUtil.join(deckId, presentationId);
        try {
            // Add rows to Presentations table.
            VPresentation presentation = new VPresentation();  // Empty for now.
            mPresentations.put(mVContext, prefix, presentation, VPresentation.class);
            VCurrentSlide current = new VCurrentSlide(0);
            mPresentations.put(mVContext, NamingUtil.join(prefix, CURRENT_SLIDE),
                    current, VCurrentSlide.class);

            mPresentations.setPrefixPermissions(mVContext, RowRange.prefix(prefix),
                    mPermissions);
            mDecks.setPrefixPermissions(mVContext, RowRange.prefix(deckId), mPermissions);

            // Create the syncgroup.
            final String syncgroupName = NamingUtil.join(
                    mSyncbaseServer.getStatus().getMounts()[0].getName(),
                    "%%sync/syncslides",
                    prefix);
            //final String syncgroupName = STATIC_SYNCGROUP;
            Log.i(TAG, "Creating syncgroup " + syncgroupName);
            Syncgroup syncgroup = mDB.getSyncgroup(syncgroupName);
            CancelableVContext context = mVContext.withTimeout(Duration.millis(5000));
            try {
                syncgroup.create(
                        context,
                        new SyncgroupSpec(
                                SYNCGROUP_PRESENTATION_DESCRIPTION,
                                // TODO(kash): Use real permissions.
                                mPermissions,
                                Arrays.asList(
                                        new SyncgroupPrefix(PRESENTATIONS_TABLE, prefix),
                                        new SyncgroupPrefix(DECKS_TABLE, deckId)),
                                Arrays.asList(V23Manager.syncName("sg")),
                                false
                        ),
                        new SyncgroupMemberInfo((byte) 10));
            } catch (VException e) {
                if (e.is(Errors.EXIST)) {
                    Log.i(TAG, "Syncgroup already exists");
                } else {
                    throw e;
                }
            }
            Log.i(TAG, "Finished creating syncgroup");

            V23Manager.Singleton.get().scan("...");

            // TODO(kash): Create a syncgroup for Notes?  Not sure if we should do that
            // here or somewhere else.  We're not going to demo sync across a user's
            // devices right away, so we'll figure this out later.

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.done(new CreatePresentationResult(presentationId, syncgroupName));
                }
            });
        } catch (VException e) {
            // TODO(kash): Change Callback to take an error parameter.
            handleError(e.toString());
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO(kash): fix me!
                    callback.done(new CreatePresentationResult(presentationId, "dummy name"));
                }
            });
        }
    }

    @Override
    public void joinPresentation(final String syncgroupName, final Callback<Void> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Syncgroup syncgroup = mDB.getSyncgroup(syncgroupName);
                    syncgroup.join(mVContext, new SyncgroupMemberInfo((byte) 1));
                    for (String member : syncgroup.getMembers(mVContext).keySet()) {
                        Log.i(TAG, "Member: " + member);
                    }
                    V23Manager.Singleton.get().scan("...");
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(null);
                        }
                    });
                } catch (VException e) {
                    handleError(e.toString());
                }
            }
        }).start();
    }

    @Override
    public DBList<Deck> getDecks() {
        if (!mInitialized) {
            return new NoopList<>();
        }
        return new DeckList(mVContext, mDB);
    }

    private static class DeckList implements DBList {

        private final CancelableVContext mVContext;
        private final Database mDB;
        private final Handler mHandler;
        private ResumeMarker mWatchMarker;
        private volatile boolean mIsDiscarded;
        private volatile Listener mListener;
        private List<Deck> mDecks;

        public DeckList(VContext vContext, Database db) {
            mVContext = vContext.withCancel();
            mDB = db;
            mIsDiscarded = false;
            mDecks = Lists.newArrayList();
            mHandler = new Handler(Looper.getMainLooper());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchExistingDecks();
                }
            }).start();
        }

        private void fetchExistingDecks() {
            // TODO(kash): Uncomment this once we've figured out why performance is so bad.
            //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                BatchDatabase batch = mDB.beginBatch(mVContext, null);
                mWatchMarker = batch.getResumeMarker(mVContext);
                DatabaseCore.ResultStream stream = batch.exec(mVContext,
                        "SELECT k, v FROM Decks WHERE Type(v) like \"%VDeck\"");
                for (List<VdlAny> row : stream) {
                    if (row.size() != 2) {
                        throw new VException("Wrong number of columns: " + row.size());
                    }
                    String key = (String) row.get(0).getElem();
                    Log.i(TAG, "Fetched deck " + key);
                    VDeck vDeck = (VDeck) row.get(1).getElem();
                    final Deck deck = new DeckImpl(
                            vDeck.getTitle(),
                            BitmapFactory.decodeByteArray(
                                    vDeck.getThumbnail(), 0, vDeck.getThumbnail().length),
                            key);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            put(deck);
                        }
                    });
                }
                watchForDeckChanges();
            } catch (VException e) {
                Log.e(TAG, e.toString());
            }
        }

        private void watchForDeckChanges() {
            Stream<WatchChange> changeStream = null;
            try {
                changeStream = mDB.watch(mVContext, DECKS_TABLE, "", mWatchMarker);
            } catch (VException e) {
                Log.e(TAG, "Couldn't watch for changes to the Decks table: " + e.toString());
                return;
            }
            for (WatchChange change : changeStream) {
                if (!change.getTableName().equals(DECKS_TABLE)) {
                    Log.e(TAG, "Wrong change table name: " + change.getTableName() + ", wanted: " +
                            DECKS_TABLE);
                    continue;
                }
                final String key = change.getRowName();
                // Ignore slide changes.
                if (NamingUtil.split(key).size() != 1) {
                    Log.d(TAG, "Ignoring slide change: " + key);
                    continue;
                }
                Log.d(TAG, "Processing change to deck: " + key);
                if (change.getChangeType().equals(ChangeType.PUT_CHANGE)) {
                    // New deck or change to an existing deck.
                    VDeck vDeck = null;
                    try {
                        vDeck = (VDeck) VomUtil.decode(change.getVomValue(), VDeck.class);
                    } catch (VException e) {
                        Log.e(TAG, "Couldn't decode deck: " + e.toString());
                    }
                    final Deck deck =
                            new DeckImpl(vDeck.getTitle(),
                                    BitmapFactory.decodeByteArray(
                                            vDeck.getThumbnail(), 0, vDeck.getThumbnail().length),
                                    key);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            put(deck);
                        }
                    });
                } else {  // ChangeType.DELETE_CHANGE
                    // Existing deck deleted.
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            delete(key);
                        }
                    });
                }
            }
            Log.i(TAG, "Deck change thread exiting");
        }

        @Override
        public int getItemCount() {
            if (mDecks != null) {
                return mDecks.size();
            }
            return 0;
        }

        @Override
        public Deck get(int i) {
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
        public synchronized void discard() {
            Log.i(TAG, "Discarding deck list.");
            mVContext.cancel();  // this will cause the watcher thread to exit
            mHandler.removeCallbacksAndMessages(null);
            // We've canceled all the pending callbacks, but the handler might be just about
            // to execute put()/get() and those messages wouldn't get canceled.  So we mark
            // the list as discarded and count on put()/get() checking for it. (Note that
            // put()/get() are synchronized along with this method.)
            mIsDiscarded = true;
        }

        private synchronized void put(Deck deck) {
            if (mIsDiscarded) {
                return;
            }
            // Keep the same sorted order as the Syncbase table, otherwise decks will shuffle
            // whenever we refresh them.
            int idx = 0;
            for (; idx < mDecks.size(); ++idx) {
                int comp = mDecks.get(idx).getId().compareTo(deck.getId());
                if (comp == 0) {
                    // Existing deck.
                    mDecks.set(idx, deck);
                    if (mListener != null) {
                        mListener.notifyItemChanged(idx);
                    }
                    return;
                } else if (comp > 0) {
                    break;
                }
            }
            // New deck.
            mDecks.add(idx, deck);
            if (mListener != null) {
                mListener.notifyItemInserted(idx);
            }
        }

        private synchronized void delete(String deckId) {
            if (mIsDiscarded) {
                return;
            }
            for (int i = 0; i < mDecks.size(); ++i) {
                if (mDecks.get(i).getId().equals(deckId)) {
                    mDecks.remove(i);
                    if (mListener != null) {
                        mListener.notifyItemRemoved(i);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void getSlides(final String deckId, final Callback<List<Slide>> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // We could probably share this block of code with SlideList.fetchData(),
                // but they are just different enough to make that annoying.  Not sure
                // that we'd really save anything in the end.
                try {
                    BatchDatabase batch = mDB.beginBatch(mVContext, null);
                    Table table = batch.getTable(NOTES_TABLE);

                    String query = "SELECT k, v FROM Decks WHERE Type(v) LIKE \"%VSlide\" " +
                            "AND k LIKE \"" + NamingUtil.join(deckId, "slides") + "%\"";
                    DatabaseCore.ResultStream stream = batch.exec(mVContext, query);
                    // TODO(kash): Abort execution if interrupted.  Perhaps we should derive
                    // a new VContext so it can be cancelled.
                    final List<Slide> slides = Lists.newArrayList();
                    for (List<VdlAny> row : stream) {
                        if (row.size() != 2) {
                            throw new VException("Wrong number of columns: " + row.size());
                        }
                        String key = (String) row.get(0).getElem();
                        Log.i(TAG, "Fetched slide " + key);
                        VSlide slide = (VSlide) row.get(1).getElem();
                        VNote note = (VNote) table.get(mVContext, key, VNote.class);
                        slides.add(new SlideImpl(
                                BitmapFactory.decodeByteArray(
                                        slide.getThumbnail(), 0, slide.getThumbnail().length),
                                note.getText()));
                    }
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(slides);
                        }
                    });
                } catch (VException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    @Override
    public DBList<Slide> getSlides(String deckId) {
        if (!mInitialized) {
            return new NoopList<>();
        }
        return new SlideList(mVContext, mDB, deckId);
    }

    private static class SlideList implements DBList {
        private final CancelableVContext mVContext;
        private final Database mDB;
        private final Handler mHandler;
        private final String mDeckId;
        private ResumeMarker mWatchMarker;
        private volatile boolean mIsDiscarded;
        // Storage for slides, mirroring the slides in the Syncbase.  Since slide numbers can
        // have "holes" in them (e.g., 1, 2, 4, 6, 8), we maintain a map from slide key
        // to the slide, as well as an ordered list which is returned to the caller.
        private TreeMap<String, Slide> mSlidesMap;
        private List<Slide> mSlides;
        private Listener mListener;

        public SlideList(VContext vContext, Database db, String deckId) {
            Log.i(TAG, "Fetching slides for " + deckId);
            mVContext = vContext.withCancel();
            mDB = db;
            mDeckId = deckId;
            mIsDiscarded = false;
            mSlidesMap = new TreeMap<>();
            mSlides = Lists.newArrayList();
            mHandler = new Handler(Looper.getMainLooper());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchExistingSlides();
                }
            }).start();
        }

        private void fetchExistingSlides() {
            // TODO(kash): Uncomment this once we've figured out why performance is so bad.
            //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            try {
                BatchDatabase batch = mDB.beginBatch(mVContext, null);
                mWatchMarker = batch.getResumeMarker(mVContext);
                Table notesTable = batch.getTable(NOTES_TABLE);

                String query = "SELECT k, v FROM Decks WHERE Type(v) LIKE \"%VSlide\" " +
                        "AND k LIKE \"" + NamingUtil.join(mDeckId, "slides") + "%\"";
                DatabaseCore.ResultStream stream = batch.exec(mVContext, query);
                for (List<VdlAny> row : stream) {
                    if (row.size() != 2) {
                        throw new VException("Wrong number of columns: " + row.size());
                    }
                    final String key = (String) row.get(0).getElem();
                    Log.i(TAG, "Fetched slide " + key);
                    VSlide slide = (VSlide) row.get(1).getElem();
                    String notes = notesForSlide(notesTable, key);
                    final SlideImpl newSlide = new SlideImpl(
                            BitmapFactory.decodeByteArray(
                                    slide.getThumbnail(), 0, slide.getThumbnail().length),
                            notes);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            put(key, newSlide);
                        }
                    });
                }
                watchForSlideChanges();
            } catch (VException e) {
                Log.e(TAG, e.toString());
            }
        }

        private void watchForSlideChanges() {
            Table notesTable = mDB.getTable(NOTES_TABLE);
            Stream<WatchChange> changeStream;
            try {
                changeStream = mDB.watch(mVContext, DECKS_TABLE, "", mWatchMarker);
            } catch (VException e) {
                Log.e(TAG, "Couldn't watch for changes to the Decks table: " + e.toString());
                return;
            }

            for (WatchChange change : changeStream) {
                if (!change.getTableName().equals(DECKS_TABLE)) {
                    Log.e(TAG, "Wrong change table name: " + change.getTableName() + ", wanted: " +
                            DECKS_TABLE);
                    continue;
                }
                final String key = change.getRowName();
                // Ignore deck changes.
                if (NamingUtil.split(key).size() <= 1) {
                    Log.d(TAG, "Ignoring deck change: " + key);
                    continue;
                }
                if (change.getChangeType().equals(ChangeType.PUT_CHANGE)) {
                    // New slide or change to an existing slide.
                    VSlide vSlide = null;
                    try {
                        vSlide = (VSlide) VomUtil.decode(change.getVomValue(), VSlide.class);
                    } catch (VException e) {
                        Log.e(TAG, "Couldn't decode slide: " + e.toString());
                    }
                    String notes = notesForSlide(notesTable, key);
                    final SlideImpl slide = new SlideImpl(
                            BitmapFactory.decodeByteArray(
                                    vSlide.getThumbnail(), 0, vSlide.getThumbnail().length),
                            notes);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            put(key, slide);
                        }
                    });
                } else {  // ChangeType.DELETE_CHANGE
                    // Existing slide deleted.
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            delete(key);
                        }
                    });
                }
            }
            Log.i(TAG, "Slides watcher thread exiting");
        }

        @Override
        public int getItemCount() {
            return mSlides.size();
        }

        @Override
        public Slide get(int i) {
            return mSlides.get(i);
        }

        @Override
        public void setListener(Listener listener) {
            mListener = listener;
        }

        @Override
        public synchronized void discard() {
            Log.i(TAG, "Discarding slides list");
            mVContext.cancel();  // this will cause the watcher thread to exit
            mHandler.removeCallbacksAndMessages(null);
            // We've canceled all the pending callbacks, but the handler might be just about
            // to execute put()/get() and those messages wouldn't get canceled.  So we mark
            // the list as discarded and count on put()/get() checking for it. (Note that
            // put()/get() are synchronized along with this method.)
            mIsDiscarded = true;
        }

        private synchronized void put(String key, Slide slide) {
            if (mIsDiscarded) {
                return;
            }
            Slide oldSlide = mSlidesMap.put(key, slide);
            mSlides = Lists.newArrayList(mSlidesMap.values());
            int idx = mSlides.indexOf(slide);
            if (idx == -1) {
                // Should never happen.
                throw new RuntimeException("Can't find a newly inserted slide");
            }
            if (mListener != null) {
                if (oldSlide != null) {  // Update to an existing slide.
                    mListener.notifyItemChanged(idx);
                } else {
                    mListener.notifyItemInserted(idx);
                }
            }
        }

        private synchronized void delete(String key) {
            if (mIsDiscarded) {
                return;
            }
            Slide deletedSlide = mSlidesMap.remove(key);
            if (deletedSlide == null) {
                Log.e(TAG, "Deleting a slide that doesn't exist: " + key);
                return;
            }
            int idx = mSlides.indexOf(deletedSlide);
            if (idx == -1) {
                // Should never happen.
                throw new RuntimeException("Couldn't find a deleted slide in the list");
            }
            mSlides.remove(idx);
            if (mListener != null) {
                mListener.notifyItemRemoved(idx);
            }
        }

        private String notesForSlide(Table notesTable, String key) {
            try {
                return ((VNote) notesTable.get(mVContext, key, VNote.class)).getText();
            } catch (VException e) {
                return "";
            }
        }
    }

    @Override
    public void setCurrentSlide(final String deckId, final String presentationId,
                                final int slideNum) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String rowKey = NamingUtil.join(deckId, presentationId, CURRENT_SLIDE);
                    Log.i(TAG, "Writing row " + rowKey + " with " + slideNum);
                    mPresentations.put(mVContext, rowKey, new VCurrentSlide(slideNum),
                            VCurrentSlide.class);
                } catch (VException e) {
                    handleError(e.toString());
                }
            }
        }).start();
    }

    @Override
    public void addCurrentSlideListener(String deckId, String presentationId,
                                        CurrentSlideListener listener) {
        String key = NamingUtil.join(deckId, presentationId);
        Log.i(TAG, "addCurrentSlideListener " + key);
        CurrentSlideWatcher watcher = mCurrentSlideWatchers.get(key);
        if (watcher == null) {
            watcher = new CurrentSlideWatcher(mVContext, mDB, deckId, presentationId);
            mCurrentSlideWatchers.put(key, watcher);
        }
        watcher.addListener(listener);
    }

    @Override
    public void removeCurrentSlideListener(String deckId, String presentationId,
                                           CurrentSlideListener listener) {
        String key = NamingUtil.join(deckId, presentationId);
        CurrentSlideWatcher watcher = mCurrentSlideWatchers.get(key);
        if (watcher == null) {
            return;
        }
        watcher.removeListener(listener);
        if (!watcher.hasListeners()) {
            mCurrentSlideWatchers.remove(key);
        }
    }

    @Override
    public void setQuestionListener(String deckId, String presentationId,
                                    QuestionListener listener) {
        String key = NamingUtil.join(deckId, presentationId);
        QuestionWatcher oldWatcher = mQuestionWatchers.get(key);
        if (oldWatcher != null) {
            oldWatcher.discard();
        }
        QuestionWatcher watcher = new QuestionWatcher(
                new WatcherState(mVContext, mDB, deckId, presentationId),
                listener);
        mQuestionWatchers.put(key, watcher);
    }

    @Override
    public void removeQuestionListener(String deckId, String presentationId, QuestionListener listener) {
        String key = NamingUtil.join(deckId, presentationId);
        QuestionWatcher oldWatcher = mQuestionWatchers.get(key);
        if (oldWatcher != null) {
            mQuestionWatchers.remove(oldWatcher);
            oldWatcher.discard();
        }
    }

    @Override
    public void askQuestion(final String deckId, final String presentationId,
                            final String firstName, final String lastName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String rowKey = NamingUtil.join(deckId, presentationId, QUESTIONS,
                            UUID.randomUUID().toString());
                    Log.i(TAG, "Writing row " + rowKey + " with " + firstName + " " + lastName);
                    VQuestion question = new VQuestion(
                            new VPerson(firstName, lastName),
                            System.currentTimeMillis(),
                            false // Not yet answered.
                    );
                    mPresentations.put(mVContext, rowKey, question, VQuestion.class);
                    // TODO(kash): Set the ACL so nobody else can modify the question.
                } catch (VException e) {
                    handleError(e.toString());
                }
            }
        }).start();
    }

    private void handleError(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }

    private void importDecks() {
        importDeckFromResources("deckId1", "Car Business", R.drawable.thumb_deck1);
        // These slow down app startup, so skip these for now.
//        importDeckFromResources("deckId2", "Baku Discovery", R.drawable.thumb_deck2);
//        importDeckFromResources("deckId3", "Vanadium", R.drawable.thumb_deck3);
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

    private static final String[] SLIDENOTES = {
            "This is the teaser slide. It should be memorable and descriptive of what your " +
                    "company is trying to do", "",
            "The bigger the pain, the better",
            "How do you solve this problem? How is it better or different from existing solutions?",
            "Demo the product", "", "[REDACTED]",
            "They may have tractor traction, but we still have the competitive advantage",
            "I'm not a businessman. I'm a business, man", "There is no 'i' on this slide",
            "Sqrt(all evil)"};

    private void importDeckFromResources(String prefix, String title, int resourceId) {
        try {
            putDeck(prefix, title, getImageBytes(resourceId));
            for (int i = 0; i < SLIDENOTES.length; i++) {
                putSlide(prefix, i, getImageBytes(SLIDEDRAWABLES[i]), SLIDENOTES[i]);
            }
        } catch (VException e) {
            handleError(e.toString());
        }
    }

    @Override
    public void importDeck(io.v.android.apps.syncslides.model.Deck deck,
                           io.v.android.apps.syncslides.model.Slide[] slides) {
        try {
            putDeck(deck.getId(), deck.getTitle(), getImageBytes(deck.getThumb()));
            for (int i = 0; i < slides.length; ++i) {
                io.v.android.apps.syncslides.model.Slide slide = slides[i];
                putSlide(deck.getId(), i, getImageBytes(slide.getImage()), slide.getNotes());
            }
        } catch (VException e) {
            handleError(e.toString());
        }
    }

    @Override
    public void importDeck(final io.v.android.apps.syncslides.model.Deck deck,
                           final io.v.android.apps.syncslides.model.Slide[] slides,
                           final Callback<Void> callback) {
        new Thread() {
            @Override
            public void run() {
                importDeck(deck, slides);
                if (callback != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(null);
                        }
                    });
                }
            }
        }.start();
    }

    private void putDeck(String prefix, String title, byte[] thumbData) throws VException {
        Log.i(TAG, String.format("Adding deck %s, %s", prefix, title));
        if (!mDecks.getRow(prefix).exists(mVContext)) {
            mDecks.put(
                    mVContext,
                    prefix,
                    new VDeck(title, thumbData),
                    VDeck.class);
        }
    }

    private void putSlide(String prefix, int idx, byte[] thumbData, String note) throws VException {
        String key = NamingUtil.join(prefix, "slides", String.format("%04d", idx));
        Log.i(TAG, "Adding slide " + key);
        if (!mDecks.getRow(key).exists(mVContext)) {
            mDecks.put(
                    mVContext,
                    key,
                    new VSlide(thumbData),
                    VSlide.class);
        }
        Log.i(TAG, "Adding note: " + note);
        mNotes.put(mVContext, key, new VNote(note), VNote.class);
        mNotes.put(
                mVContext,
                NamingUtil.join(prefix, "LastViewed"),
                System.currentTimeMillis(),
                Long.class);
    }

    @Override
    public void deleteDeck(String deckId) {
        try {
            mDecks.deleteRange(mVContext, RowRange.prefix(deckId));
        } catch (VException e) {
            handleError(e.toString());
        }
    }

    @Override
    public void deleteDeck(final String deckId, final Callback<Void> callback) {
        new Thread() {
            @Override
            public void run() {
                deleteDeck(deckId);
                if (callback != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(null);
                        }
                    });
                }
            }
        }.start();
    }

    private byte[] getImageBytes(int resourceId) {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId);
        return getImageBytes(bitmap);
    }

    private byte[] getImageBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        return stream.toByteArray();
    }
}
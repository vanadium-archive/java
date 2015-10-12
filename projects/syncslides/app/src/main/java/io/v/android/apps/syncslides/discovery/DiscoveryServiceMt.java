// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.v.android.apps.syncslides.model.Participant;

/**
 * An app.Service to coordinate discovery of V23 services.
 *
 * OPERATION
 *
 * This implemenation uses a V23 mounttable (aka MT, aka namespaceRoot), and
 * does not use BLE, MDNS, etc. It's similar to BLE in that there's a repeated
 * "scan" of mounttables, and an occasional "advertise" (mounting a name in an
 * MT), ala android's BluetoothLeScanner and BluetoothLeAdvertiser.
 *
 * SCAN
 *
 * ** s1) Determine which MT to use
 *
 * The MT address is either hardcoded or gathered from the user via more
 * flexible technique, e.g. user prompting or web scraping.
 *
 * ** s2) Obtain initial participant list from MT
 *
 * Queries MT to get a list of names of all V23 services with a name in the form
 * "${rootPath}/*". The part matching the wildcard is the unique part of the
 * name.
 *
 * These V23 services are called 'participant' services.
 *
 * ** s3) Grab data
 *
 * A participant service has a method Get() that returns what's needed to answer
 * any questions about the participant.
 *
 * Call Get() on each participant, and inform any local app clients connected to
 * this app.Service (in local android app space) of any interesting new data via
 * a ServiceConnection.
 *
 * The above sequence s1-s2-s3, or at least s2-s3, is repeated with some period
 * for the lifetime of the app.Service.
 *
 *
 * ADVERTISE
 *
 * If a message comes in from the encapsulating app (meaning a local
 * ServiceConnection message, not a V23 RPC), this app.Service should advertise
 * it to all participants, per these points:
 *
 * ** m1) Determine selfName
 *
 * Given the list of participant names, and a strategy injected to this
 * app.Service, this app.Service determines what it's own participant name
 * should be in the context of the MT to avoid a collision.  A user name isn't
 * sufficient, since a user might be using more than one device.
 *
 * ** m2) Create a new participant
 *
 * This app.Service starts its own V23 participant service and mounts in the MT
 * at selfName.
 *
 * Other participants will see the new service by virtue of their own scanning
 * (MT polling), and are expected to call its Get(), retrieving the data this
 * app instance wants to deliver.
 *
 * ** m3) Destroy the participant when the event is over.
 *
 * When the data served by Get() is no longer valid or useful, the app.Service
 * should unmount from the MT.
 */
public class DiscoveryServiceMt extends Service implements Moderator.Observer {

    private static final String TAG = "DiscoveryServiceMt";
    // TODO(jregan): Provide means to launch without demanding a MT.
    private static final boolean USE_MOUNT_TABLE = false;
    // Accepts incoming messages from whatever binds to this service.
    private final Messenger mMsgReceiver = new Messenger(new IncomingMessHandler());
    // Clients to notify when something interesting happens.
    private final List<Messenger> mMsgSenders = new ArrayList<>();
    // Runs the scanner repeatedly.
    private final PeriodicTasker mTasker = new PeriodicTasker();
    // Manages creation, mounting and unmounting of V23 services.
    private V23Manager mV23Manager;
    // In pool of presenters on the network, knows who's new (freshman),
    // old (senior) and gone (graduated).
    private Moderator mModerator;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "###### onCreate");
        mV23Manager = new V23Manager(this);
        mV23Manager.init();
        mModerator = new Moderator(this, makeScanner());
        mTasker.start(mModerator);
    }

    private ParticipantScanner makeScanner() {
        return USE_MOUNT_TABLE ?
                new ParticipantScannerMt(mV23Manager) :
                new ParticipantScannerFake();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTasker.stop();
        Log.d(TAG, "###### onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMsgReceiver.getBinder();
    }

    /**
     * Called periodically, whenever the mModerator finishes a scan. Sweeps out
     * dead listeners.
     */
    public void onTaskDone() {
        Log.d(TAG, "onTaskDone");
        List<Messenger> dead = new ArrayList<>();
        for (Messenger client : mMsgSenders) {
            try {
                notifyClients(client);
            } catch (RemoteException e) {
                dead.add(client);
            }
        }
        for (Messenger observer : dead) {
            mMsgSenders.remove(observer);
        }
    }

    private void notifyClients(Messenger observer) throws RemoteException {
        Log.d(TAG, "notifyClients: " +
                " graduated=" + mModerator.getGraduated().size() +
                " freshman=" + mModerator.getFreshman().size());
        for (Participant p : mModerator.getGraduated()) {
            sendMessage(observer, p, MsgType.PRESO_END);
        }
        for (Participant p : mModerator.getFreshman()) {
            sendMessage(observer, p, MsgType.PRESO_BEGIN);
        }
    }

    private void sendMessage(Messenger observer, Participant p, MsgType kind)
            throws RemoteException {
        Log.d(TAG, "sendMessage " + kind);
        Message msg = Message.obtain(null, kind.ordinal());
        msg.setData(p.toBundle());
        observer.send(msg);
    }

    /**
     * Message types used between this service and its clients.
     */
    public enum MsgType {
        UNKNOWN, REGISTER, UNREGISTER, PRESO_BEGIN, PRESO_END;

        private static final DiscoveryServiceMt.MsgType[] stupid =
                DiscoveryServiceMt.MsgType.values();

        public static MsgType fromInt(int k) {
            return k >= 0 && k < stupid.length ? stupid[k] : UNKNOWN;
        }
    }

    // Handler of incoming messages from clients.
    private class IncomingMessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MsgType msgType = MsgType.fromInt(msg.what);
            Log.d(TAG, "handleMessage: " + msgType);
            switch (msgType) {
                case REGISTER:
                    Log.d(TAG, "got REGISTER");
                    if (mMsgSenders.size() > 0) {
                        // Possible indication of dead wood listening, e.g. an
                        // activity that's been 'destroyed' (replaced) but...
                        Log.e(TAG, "Why do we have > 1 listener?");
                        Log.e(TAG, "   OLD: ");
                        for (Messenger m : mMsgSenders) {
                            Log.e(TAG, "      m = " + m.getBinder());
                        }
                        Log.e(TAG, "  NEW: " + msg.replyTo.getBinder());
                    }
                    mMsgSenders.add(msg.replyTo);
                    break;
                case UNREGISTER:
                    Log.d(TAG, "got UNREGISTER");
                    mMsgSenders.remove(msg.replyTo);
                    break;
                case PRESO_BEGIN:
                    // TODO:(jregan) Move this handler to PresentationActivity
                    Log.d(TAG, "got PRESO_BEGIN");
                    mV23Manager.mount("someId", null);
                    break;
                case PRESO_END:
                    // TODO:(jregan) Move this handler to PresentationActivity
                    Log.d(TAG, "got PRESO_END");
                    mV23Manager.unmount();
                    break;
                default:
                    Log.d(TAG, "Unknown message: " + msg);
                    super.handleMessage(msg);
            }
        }
    }
}

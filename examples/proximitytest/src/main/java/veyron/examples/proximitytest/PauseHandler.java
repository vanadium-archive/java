package com.veyron2.services.proximity.scanner;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;

/**
 * PauseHandler is Handler that can be paused. Any new messages added to the
 * handler while it's paused will be enqueued, to be executed only after the
 * handler is unpaused.
 * Note that the messages currently in execution will not be stopped.  However,
 * should they result in more messages being inserted into the same handler,
 * those new messages will not be executed (they will be enqueued for later
 * execution).
 */
public class PauseHandler extends Handler {
    private boolean isPaused;
    private ArrayList<Message> messages;

    public PauseHandler() {
        super();
        isPaused = false;
        messages = new ArrayList<Message>();
    }

    // Pauses the handler.  All message added to the handler after this method
    // returns will be enqueued onto the message queue, to be executed after the
    // subsequent call to resume().
    public synchronized void pause() {
        isPaused = true;
    }

    // Unpauses the handler.  All messages enqueued during the paused state will
    // be executed.
    public synchronized void resume() {
        isPaused = false;
        // Re-insert all of the messages inserted during the paused state.
        // NOTE(spetrovic): It is possible that dispatchMessage() below will produce
        // a new message which may get executed before the messages in the rest of
        // the queue, but we can live with that.
        for (int i = 0; i < messages.size(); ++i) {
            super.dispatchMessage(messages.get(i));
        }
        messages.clear();
    }

    @Override
    public synchronized void dispatchMessage(Message msg) {
        if (isPaused) {  // handler is paused - enqueue the message.
            // Copy the message because it gets recycled by the Looper.
            messages.add(Message.obtain(msg));
        } else {
            super.dispatchMessage(msg);
        }
    }
}
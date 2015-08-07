// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.jtransforms.fft.DoubleFFT_1D;

public class UltrasoundDetectorAsyncTask extends AsyncTask<Context, Void, Boolean> {
    private static final String TAG = UltrasoundDetectorAsyncTask.class.getSimpleName();
    private static final int SAMPLING_RATE = 44100;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MIN_BUFF_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO, ENCODING);
    private static final int MIN_INDEX = 2500;
    private static final int THRESHOLD = 200000;
    private AudioRecord mAudioRecord;
    private Context mContext;

    @Override
    protected Boolean doInBackground(Context... params) {
        mContext = params[0];
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, ENCODING, MIN_BUFF_SIZE);
        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d(TAG, "Can't initialize AudioRecord");
            return null;
        }
        short[] data = new short[MIN_BUFF_SIZE];
        Log.d(TAG, "MIN_BUFF_SIZE: " + MIN_BUFF_SIZE);
        while (!this.isCancelled()) {
            mAudioRecord.startRecording();
            Log.d(TAG, "Started recording at " + String.valueOf(System.nanoTime()));
            int lengthRead = mAudioRecord.read(data, 0, MIN_BUFF_SIZE);
            while (lengthRead < MIN_BUFF_SIZE) {
                int newRead = mAudioRecord.read(data, lengthRead, MIN_BUFF_SIZE - lengthRead);
                lengthRead += newRead;
            }
            mAudioRecord.stop();
            Log.d(TAG, "Finished recording at " + String.valueOf(System.nanoTime()));
            double[] fft_data = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                fft_data[i] = data[i];
            }
            DoubleFFT_1D fft = new DoubleFFT_1D(data.length);
            fft.realForward(fft_data);
            for (int i = MIN_INDEX; i < fft_data.length; i++) {
                if (fft_data[i] > THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(mContext,  "Ultrasound detected.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Ultrasound detected.");
        }
    }
}

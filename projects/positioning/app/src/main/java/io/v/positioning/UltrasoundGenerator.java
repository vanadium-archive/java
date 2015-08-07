// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class UltrasoundGenerator {
    private static final int FREQUENCY = 20000;
    private static final int AMPLITUDE = 32767;
    // Duration of the tone in seconds
    private static final int DURATION = 1;
    private static final int STEP_AMOUNT = 100;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int LOOP_COUNT = 10;
    private static final int SAMPLE_RATE = AudioTrack.getNativeOutputSampleRate(
            AudioTrack.MODE_STATIC);
    private static final byte[] SOUND_BYTES = createSoundArray(FREQUENCY);
    private static final String TAG = UltrasoundGenerator.class.getSimpleName();
    private AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
            SAMPLE_RATE *STEP_AMOUNT * 2 * DURATION, AudioTrack.MODE_STATIC);

    UltrasoundGenerator() {
        setAudioTrack();
    }

    public long playUltrasound() {
        long startTime = System.nanoTime();
        audioTrack.play();
        return startTime;
    }

    public void stopUltrasound() {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack.release();
        }
    }

    private void setAudioTrack() {
        int bytes = audioTrack.write(SOUND_BYTES, 0, SOUND_BYTES.length);
        if (AudioTrack.ERROR_INVALID_OPERATION == bytes || AudioTrack.ERROR_BAD_VALUE == bytes) {
            Log.d(TAG, "Failed writing to the audioTrack " + bytes);
        }
        audioTrack.setLoopPoints(0, SAMPLE_RATE * DURATION, LOOP_COUNT);
        audioTrack.setNotificationMarkerPosition(SOUND_BYTES.length / 2);
        audioTrack.setPlaybackPositionUpdateListener(
                new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.d(TAG, "Ultrasound sent at " + System.nanoTime());
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
            }
        });
    }

    /**
     * Create a 16-bit PCM sinusoidal tone.
     *
     * @return A byte array containing the encoded tone.
     */
    private static byte[] createSoundArray(double frequencyHz) {
        int numSamples = DURATION * SAMPLE_RATE;
        byte[] soundBytes = new byte[BYTES_PER_SAMPLE * numSamples];
        double[] sampleTones = new double[numSamples];
        for (int i = 0; i < numSamples; i++) {
            sampleTones[i] = Math.sin(2 * Math.PI * (i / (SAMPLE_RATE / frequencyHz)));
        }
        int idx = 0;
        for (final double dVal : sampleTones) {
            final short val = (short) ((dVal * AMPLITUDE));
            soundBytes[idx++] = (byte) (val & 0x00ff);
            soundBytes[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return soundBytes;
    }
}

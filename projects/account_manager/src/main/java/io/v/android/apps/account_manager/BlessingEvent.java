// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import io.v.v23.vom.VomUtil;
import io.v.v23.security.Caveat;


/** A ({@link io.v.v23.security.Blessings}, timestamp) blessing event. */
class BlessingEvent implements Serializable {
    String   mBlessingsVom;
    DateTime mTimeStamp;
    List<Caveat> mCaveats;
    String   mExtension;

    BlessingEvent(String blessingsVom, DateTime timeStamp, List<Caveat> caveats, String extension) {
        mBlessingsVom = blessingsVom;
        mTimeStamp = timeStamp;
        mCaveats = caveats;
        mExtension = extension;
    }

    /**
     * Returns the  timestamp of the blessing event.
     */
    public DateTime getTimeStamp() {
        return mTimeStamp;
    }

    /**
     * Returns the VOM encoded {@link io.v.v23.security.Blessings} of the
     * certificate chain that was extended in this blessing event.
     */
    public String getBlessingsVom() {
        return mBlessingsVom;
    }

    /**
     * Returns the {@link  Caveat} list of the blessing event.
     */
    public List<Caveat> getCaveats() {
        return mCaveats;
    }

    /**
     * Returns the name that the blessee was given when the certificate
     * chain was extended.
     */
    public String getNameExtension() {
        return mExtension;
    }

    /**
     * Encodes this blessing event.
     *
     * @return                encoded blessing event
     * @throws Exception      if there was an encoding error
     */
    public String encodeToString() throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
        objOut.writeObject(this);
        objOut.close();
        byte[] byteArray = byteOut.toByteArray();
        String stringEncoding = VomUtil.bytesToHexString(byteArray);
        return stringEncoding;
    }

    /**
     * Decodes the blessing event.
     *
     * @param  encoded         blessing event encoded using {@link #encodeToString}
     * @return                 decoded blessing event
     * @throws Exception       if there was an error decoding the blessing event
     */
    public static BlessingEvent decode(String encoded) throws Exception {
        byte[] byteArray = VomUtil.hexStringToBytes(encoded);
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteArray);
        ObjectInputStream objectIn = new ObjectInputStream(byteIn);
        objectIn.close();
        return (BlessingEvent) (objectIn.readObject());
    }
}

// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.gae;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ServletPostAsyncTask extends AsyncTask<Context, Void, String> {
    // Google App Engine URL where the "backend" is deployed
    // Servlets are located in Positioning/backend/src/main/java/io.v.positioning
    private static final String GAE_URL = "festive-cirrus-100020.appspot.com";
    private URL mUrl;
    private JSONObject mData;
    private Context mContext;

    public ServletPostAsyncTask(String urlString, JSONObject data) throws MalformedURLException {
        mUrl = new URL("http", GAE_URL, urlString);
        this.mData = data;
    }

    @Override
    protected String doInBackground(Context... params) {
        mContext = params[0];
        DataOutputStream os = null;
        InputStream is = null;
        try {
            URLConnection conn = mUrl.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.connect();
            os = new DataOutputStream(conn.getOutputStream());
            os.write(mData.toString().getBytes("UTF-8"));
            is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            return br.readLine();
        } catch (IOException e) {
            return "IOException while contacting GEA: " + e.getMessage();
        } catch (Exception e) {
            return "Exception while contacting GEA: " + e.getLocalizedMessage();
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
                return "IOException closing os: " + e.getMessage();
            }
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                return "IOException closing is: " + e.getMessage();
            }
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
    }
}

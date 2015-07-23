// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that is deployed on GAE,handles GET and POST requests from android
 * Proximity app to save/retrieve bluetooth properties of nearby devices to/from Datastore.
 */
public class BluetoothProximityServlet extends HttpServlet {

    /**
     * Processes a GET request for the nearby bluetooth records
     *
     * @param req  The servlet request that includes <b>"androidId"</b> parameter
     * @param resp The servlet response with the number of nearby devices
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter propertyFilter = new Query.FilterPredicate("androidId", Query.FilterOperator.EQUAL, req.getParameter("androidId"));
        Query q = new Query("bluetooth").setFilter(propertyFilter);
        List<Entity> list = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
        // for now, send the count.
        resp.getWriter().println("Number of nearby devices: " + list.size());
    }

    /**
     * Processes a POST request to save nearby devices scanned using Bluetooth
     *
     * @param req  The servlet request that includes JSONObject with properties to be saved
     * @param resp The servlet response informing a user if the request was successful or not
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        BufferedReader br = req.getReader();
        try {
            JSONObject jo = new JSONObject(br.readLine());
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Entity entity = new Entity("bluetooth");
            entity.setProperty("androidId", jo.get("androidId"));
            entity.setProperty("myMacAddress", jo.get("myMacAddress"));
            entity.setProperty("remoteName", jo.get("remoteName"));
            entity.setProperty("remoteAddress", jo.get("remoteAddress"));
            entity.setProperty("remoteRssi", jo.get("remoteRssi"));
            entity.setProperty("deviceTime", jo.get("deviceTime"));
            entity.setProperty("serverTime", System.currentTimeMillis());
            datastore.put(entity);
            resp.getWriter().println("New bluetooth device added.");
        } catch (Exception e) {
            resp.getWriter().println("Recording failed. " + e);
        }
    }
}
// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that is deployed on GAE,handles GET and POST requests from android
 * Proximity app to save/retrieve device's GPS location to/from Datastore.
 */
public class GpsLocationServlet extends HttpServlet {

    /**
     * Processes a GET request and returns device's GPS location
     *
     * @param req  The servlet request that includes <b>"androidId"</b> parameter
     * @param resp The servlet response with lat/long information
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter propertyFilter = new Query.FilterPredicate("androidId", Query.FilterOperator.EQUAL, req.getParameter("androidId"));
        Query q = new Query("gps").setFilter(propertyFilter);
        Entity e = datastore.prepare(q).asSingleEntity();
        if (e == null) {
            resp.getWriter().println("Missing location for the given androidId.");
        } else {
            resp.getWriter().println("(" + e.getProperty("latitude") + ", " + e.getProperty("longitude") + ")");
        }
    }

    /**
     * Processes a POST request to save device's GPS location
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
            Entity entity = new Entity("gps");
            entity.setProperty("androidId", jo.get("androidId"));
            entity.setProperty("latitude", jo.get("latitude"));
            entity.setProperty("longitude", jo.get("longitude"));
            entity.setProperty("deviceTime", jo.get("deviceTime"));
            entity.setProperty("serverTime", System.currentTimeMillis());
            datastore.put(entity);
            resp.getWriter().println("New GPS location added.");
        } catch (Exception e) {
            resp.getWriter().println("Recording failed. " + e.getMessage());
        }
    }
}

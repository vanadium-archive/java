// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import java.util.List;

import io.v.v23.discovery.Service;

/**
 * Makes objects that support advertising.
 */
public interface AdSupporter {
    /**
     * Makes an instance of a service that will be run during the life of the
     * advertisement.
     */
    Object makeServer();

    /**
     * Name at which the service should be mounted. Can be empty.
     */
    String getMountName();

    /**
     * Makes an instance of 'Service', which is actually a service description,
     * i.e. an advertisement.  The argument is the list of real addresses at
     * which the service can be found (presumes no mount name).
     */
    Service makeAdvertisement(List<String> addresses);
}

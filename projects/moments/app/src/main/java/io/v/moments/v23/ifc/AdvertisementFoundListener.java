// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import io.v.v23.discovery.Advertisement;

/**
 * Something that handles recently found advertisements.
 */
public interface AdvertisementFoundListener {
    void handleFoundAdvertisement(Advertisement advertisement);
}

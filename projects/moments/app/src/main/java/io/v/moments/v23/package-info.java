// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

/**
 * Below this point sits code that wraps the evolving v23 API.
 *
 * None of it depends on any 'moments' code/data.
 *
 * The wrapper facilitates the use of the underlying API, and makes it easier to
 * test classes that use the API.
 */
package io.v.moments.v23;

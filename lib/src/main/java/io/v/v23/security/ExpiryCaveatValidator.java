// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import org.joda.time.DateTime;

import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

public class ExpiryCaveatValidator implements CaveatValidator {
    @Override
    public void validate(VContext context, Object param) throws VException {
        Call call = Security.getCall(context);
        if (param == null) param = new DateTime(0);
        if (!(param instanceof DateTime)) {
            throw new VException(String.format(
                    "Caveat param %s of wrong type: want %s", param, DateTime.class));
        }
        final DateTime expiry = (DateTime) param;
        final DateTime now = call.timestamp();
        if (now.isAfter(expiry)) {
            throw new VException(String.format(
                "ExpiryCaveat(%s) failed validation at %s", expiry, now));
        }
    }
}

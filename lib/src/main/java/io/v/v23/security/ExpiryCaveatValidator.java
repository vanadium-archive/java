package io.v.v23.security;

import org.joda.time.DateTime;

import io.v.v23.verror.VException;

public class ExpiryCaveatValidator implements CaveatValidator {
    @Override
    public void validate(Call call, Object param) throws VException {
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

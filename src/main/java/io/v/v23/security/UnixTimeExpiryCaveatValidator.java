package io.v.v23.security;

import org.joda.time.DateTime;

import io.v.v23.verror.VException;

public class UnixTimeExpiryCaveatValidator implements CaveatValidator {
    @Override
    public void validate(Call call, Object param) throws VException {
        if (param == null) param = Long.valueOf(0);
        if (!(param instanceof Long)) {
            throw new VException(String.format(
                    "Caveat param %s of wrong type: want %s", param, Long.class));
        }
        final long expirySeconds = (Long) param;
        final DateTime now = call.timestamp();
        final DateTime expiry = new DateTime(expirySeconds * 1000L);
        if (now.isAfter(expiry)) {
            throw new VException(String.format(
                "UnixTimeExpiryCaveat(%s) failed validation at %s", expiry, now));
        }
    }
}
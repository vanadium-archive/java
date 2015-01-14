package io.v.core.veyron2.security;

import org.joda.time.DateTime;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.vdl.VdlValue;

class UnixTimeExpiryCaveatValidator implements CaveatValidator {
	private final UnixTimeExpiryCaveat wire;

	UnixTimeExpiryCaveatValidator(UnixTimeExpiryCaveat wire) {
		this.wire = wire;
	}

	@Override
	public void validate(VContext context) throws VeyronException {
		final DateTime now = context.timestamp();
		final DateTime expiry = new DateTime(this.wire.getValue());
		if (now.isAfter(expiry)) {
			throw new VeyronException(String.format(
				"UnixTimeExpiryCaveat(%s) failed validation at %s", expiry, now));
		}
	}

	@Override
	public VdlValue getWire() {
		return this.wire;
	}

	@Override
	public String toString() {
		return String.format(
			"%d = %s", this.wire.getValue(), new DateTime(this.wire.getValue()));
	}
}
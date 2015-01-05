package io.v.core.veyron2.security;

import org.joda.time.DateTime;

import io.v.core.veyron2.VeyronException;

class UnixTimeExpiryCaveatWrapper implements CaveatValidator {
	private final UnixTimeExpiryCaveat caveat;

	static UnixTimeExpiryCaveatWrapper wrap(UnixTimeExpiryCaveat caveat) {
		return new UnixTimeExpiryCaveatWrapper(caveat);
	}

	private UnixTimeExpiryCaveatWrapper(UnixTimeExpiryCaveat caveat) {
		this.caveat = caveat;
	}

	@Override
	public void validate(Context context) throws VeyronException {
		final DateTime now = context.timestamp();
		final DateTime expiry = new DateTime(this.caveat.getValue());
		if (now.isAfter(expiry)) {
			throw new VeyronException(String.format(
				"UnixTimeExpiryCaveat(%s) failed validation at %s", expiry, now));
		}
	}

	@Override
	public String toString() {
		return String.format(
			"%d = %s", this.caveat.getValue(), new DateTime(this.caveat.getValue()));
	}

	public UnixTimeExpiryCaveat getCaveat() {
		return this.caveat;
	}
}
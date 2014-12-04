package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.VeyronException;

import java.util.Arrays;

class MethodCaveatWrapper implements CaveatValidator {
	private MethodCaveat caveat;

	static MethodCaveatWrapper wrap(MethodCaveat caveat) {
		return new MethodCaveatWrapper(caveat);
	}

	private MethodCaveatWrapper(MethodCaveat caveat) {
		this.caveat = caveat;
	}

	@Override
	public void validate(Context context) throws VeyronException {
		if (context.method().isEmpty() && this.caveat.isEmpty()) {
			return;
		}
		for (String method : this.caveat) {
			if (context.method().equals(method)) {
				return;
			}
		}
		throw new VeyronException(String.format(
			"MethodCaveat(%s) failed validation for method %s", this, context.method()));
	}

	@Override
	public String toString() {
		return Arrays.toString(this.caveat.toArray());
	}

	public MethodCaveat getCaveat() {
		return this.caveat;
	}
}


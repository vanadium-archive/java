package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.VeyronException;

import java.util.Arrays;

class PeerBlessingCaveatWrapper implements CaveatValidator {
	private final PeerBlessingsCaveat caveat;

	static PeerBlessingCaveatWrapper wrap(PeerBlessingsCaveat caveat) {
		return new PeerBlessingCaveatWrapper(caveat);
	}

	private PeerBlessingCaveatWrapper(PeerBlessingsCaveat caveat) {
		this.caveat = caveat;
	}

	@Override
	public void validate(Context context) throws VeyronException {
		if (context.localBlessings() == null) {
			throw new VeyronException(String.format(
				"PeerBlessingCaveat(%s) failed validation since context.localBlessings() is null",
				this));
		}
		final String[] self = context.localBlessings().forContext(context);
		for (BlessingPattern pattern : this.caveat) {
			if (BlessingPatternWrapper.wrap(pattern).isMatchedBy(self)) {
				return;
			}
		}
		throw new VeyronException(String.format(
			"PeerBlessingCaveat(%s) failed validation for peer with blessings %s",
			this, Arrays.toString(self)));
	}

	@Override
	public String toString() {
		return Arrays.toString(this.caveat.toArray());
	}

	public PeerBlessingsCaveat getCaveat() {
		return this.caveat;
	}
}
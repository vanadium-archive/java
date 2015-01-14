package io.v.core.veyron2.security;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.vdl.VdlValue;

import java.util.Arrays;

class PeerBlessingCaveatValidator implements CaveatValidator {
	private final PeerBlessingsCaveat wire;

	PeerBlessingCaveatValidator(PeerBlessingsCaveat wire) {
		this.wire = wire;
	}

	@Override
	public void validate(VContext context) throws VeyronException {
		if (context.localBlessings() == null) {
			throw new VeyronException(String.format(
				"PeerBlessingCaveat(%s) failed validation since context.localBlessings() is null",
				this));
		}
		final String[] self = context.localBlessings().forContext(context);
		for (BlessingPattern pattern : this.wire) {
			if (BlessingPatternWrapper.wrap(pattern).isMatchedBy(self)) {
				return;
			}
		}
		throw new VeyronException(String.format(
			"PeerBlessingCaveat(%s) failed validation for peer with blessings %s",
			this, Arrays.toString(self)));
	}

	@Override
	public VdlValue getWire() {
		return this.wire;
	}

	@Override
	public String toString() {
		return Arrays.toString(this.wire.toArray());
	}

}
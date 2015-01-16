package io.v.core.veyron2.security;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.vdl.VdlValue;

import java.util.Arrays;

public class MethodCaveatValidator implements CaveatValidator {
	private MethodCaveat wire;

	public MethodCaveatValidator(MethodCaveat wire) {
		this.wire = wire;
	}

	@Override
	public void validate(VContext context) throws VeyronException {
		if (context.method().isEmpty() && this.wire.isEmpty()) {
			return;
		}
		for (String method : this.wire) {
			if (context.method().equals(method)) {
				return;
			}
		}
		throw new VeyronException(String.format(
			"MethodCaveat(%s) failed validation for method %s", this, context.method()));
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


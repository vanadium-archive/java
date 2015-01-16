package io.v.jni.test.security;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.security.CaveatValidator;
import io.v.core.veyron2.security.VContext;
import io.v.core.veyron2.vdl.VdlValue;

/**
 * Validator for {@code TestCaveat} that validates the context if its veyron name
 * matches the provided name.
 */
public class TestCaveatValidator implements CaveatValidator {
	private final TestCaveat wire;

	public TestCaveatValidator(TestCaveat wire) {
		this.wire = wire;
	}

	@Override
	public void validate(VContext context) throws VeyronException {
		if (!this.wire.getValue().equals(context.name())) {
			throw new VeyronException(String.format("Got name %s, want %s",
					context.name(), this.wire.getValue()));
		}
	}

	@Override
	public VdlValue getWire() {
		return this.wire;
	}
}

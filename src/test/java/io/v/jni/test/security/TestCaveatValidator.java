package io.v.jni.test.security;

import io.v.core.veyron2.security.CaveatValidator;
import io.v.core.veyron2.security.VContext;
import io.v.core.veyron2.verror.VException;

/**
 * Validator for {@code TestCaveat} that validates the context if its veyron suffix
 * matches the provided suffix.
 */
public class TestCaveatValidator implements CaveatValidator {
	@Override
	public void validate(VContext context, Object param) throws VException {
		if (param == null) param = "";
		if (!(param instanceof String)) {
			throw new VException(String.format(
					"Caveat param %s of wrong type: want %s", param, String.class)); 
		}
		if (!param.equals(context.suffix())) {
			throw new VException(String.format("Got name %s, want %s",	context.suffix(), param));
		}
	}
}

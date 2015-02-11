package io.v.core.veyron2.security;

import io.v.core.veyron2.verror.VException;

import java.util.ArrayList;
import java.util.List;

public class MethodCaveatValidator implements CaveatValidator {
	@Override
	public void validate(VContext context, Object param) throws VException {
		if (param == null) {
			param = new ArrayList<String>();
		}
		if (!(param instanceof List<?>)) {
			throw new VException(String.format(
					"Caveat param %s of wrong type: want List<?>", param));
		}
		final List<?> methods = (List<?>) param;
		if (context.method().isEmpty() && methods.size() == 0) {
			return;
		}
		for (Object method : methods) {
			if (!(method instanceof String)) {
				throw new VException(String.format(
						"Caveat param %s element %s of wrong type: want String", param, method)); 
						
			}
			if (context.method().equals(method)) {
				return;
			}
		}
		throw new VException(String.format(
			"MethodCaveat(%s) failed validation for method %s", param, context.method()));
	}
}


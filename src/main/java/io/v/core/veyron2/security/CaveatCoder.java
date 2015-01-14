package io.v.core.veyron2.security;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.util.VomUtil;
import io.v.core.veyron2.vdl.VdlValue;

import java.lang.reflect.Constructor;

public class CaveatCoder {
	private static final String VALIDATOR_SUFFIX = "Validator";

	public static Caveat encode(CaveatValidator validator) throws VeyronException {
		final VdlValue wire = validator.getWire();
		final byte[] data = VomUtil.encode(wire);
		return new Caveat(data);
	}

	public static CaveatValidator decode(Caveat caveat) throws VeyronException {
		final Object data = VomUtil.decode(caveat.getValidatorVOM());
		if (data == null) {
			throw new VeyronException("Decoded null validator data.");
		}
		final String validatorClassName = data.getClass().getName() + VALIDATOR_SUFFIX;
		Class<?> validatorClass = null;
		try {
			validatorClass = Class.forName(validatorClassName);
		} catch (ClassNotFoundException e) {
			throw new VeyronException(String.format("Couldn't find validator class %s: %s",
					validatorClassName, e.getMessage()));
		}
		if (validatorClass == null) {
			throw new VeyronException("Couldn't find validator class: " + validatorClassName);
		}
		if (!validatorClass.isAssignableFrom(CaveatValidator.class)) {
			throw new VeyronException(String.format(
				"Class %s doesn't implement CaveatValidator interface", validatorClass.getName()));
		}
		Constructor<?> constr = null;
		try {
			constr = validatorClass.getConstructor(data.getClass());
		} catch (NoSuchMethodException e) {
			throw new VeyronException(String.format("Couldn't find constructor %s(%s) : %s",
					validatorClass.getName(), data.getClass().getName(), e.getMessage()));
		}
		if (constr == null) {
			throw new VeyronException(String.format("Couldn't find constructor %s(%s).",
					validatorClass.getName(), data.getClass().getName()));
		}
		Object validator = null;
		try {
			validator = constr.newInstance(data);
		} catch (Exception e) {
			throw new VeyronException("Couldn't create new instance of validator class: " +
				validatorClass.getName());
		}
		if (validator == null) {
			throw new VeyronException(String.format("Got null instance of validator class: ",
				validatorClass.getName()));
		}
		return (CaveatValidator) validator;
	}
}
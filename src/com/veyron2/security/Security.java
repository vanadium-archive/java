package com.veyron2.security;

public class Security {
	// Set of all valid Labels for IPC methods.
	public static Label[] VALID_LABELS =
		{ VeyronConsts.READ_LABEL, VeyronConsts.WRITE_LABEL, VeyronConsts.ADMIN_LABEL,
		  VeyronConsts.DEBUG_LABEL, VeyronConsts.MONITORING_LABEL };

	public static boolean IsValidLabel(Label label) {
		for (Label validLabel : VALID_LABELS) {
			if (validLabel.equals(label)) {
				return true;
			}
		}
		return false;
	}
}
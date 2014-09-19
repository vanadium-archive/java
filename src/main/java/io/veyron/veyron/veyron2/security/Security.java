package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.util.Map.Entry;

public class Security {
	// Set of all valid Labels for IPC methods.
	public static Label[] VALID_LABELS =
		{ SecurityConstants.READ_LABEL, SecurityConstants.WRITE_LABEL, SecurityConstants.ADMIN_LABEL,
		  SecurityConstants.DEBUG_LABEL, SecurityConstants.MONITORING_LABEL };

	/**
	 * Returns true iff the provided label is among the set of valid labels.
	 *
	 * @param  label the label being checked for validity.
	 * @return       true iff the label is valid.
	 */
	public static boolean IsValidLabel(Label label) {
		for (Label validLabel : VALID_LABELS) {
			if (validLabel.equals(label)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates an authorizer for the provided ACL. The authorizer authorizes a request iff the
	 * identity at the remote end has a name authorized by the provided ACL for the request's label,
	 * or the request corresponds to a self-RPC.
	 *
	 * @param  acl ACL used for authorization checks.
	 * @return     Authorizer that performs authorization checks.
	 */
	public static Authorizer newACLAuthorizer(ACL acl) {
		return new ACLAuthorizer(acl);
	}

	private static class ACLAuthorizer implements Authorizer {
		private final ACL acl;

		ACLAuthorizer(ACL acl) {
			this.acl = acl;
		}

		@Override
		public void authorize(Context context) throws VeyronException {
			final PublicID localID = context.localID();
			final PublicID remoteID = context.remoteID();
			final Label label = context.label();
			if (localID != null && remoteID != null && localID.equals(remoteID)) {  // self-RPC.
				return;
			}
			if (localID == null) throw new VeyronException("Identity being matched in null.");
			if (this.acl == null) throw new VeyronException("ACL is null.");
			if (label == null) throw new VeyronException("Label is null.");
			return;
			/*
			for (Entry<BlessingPattern, LabelSet> e : this.acl.getValue().entrySet()) {
				final Label other = e.getValue().getValue();
				if (label.equals(other) && localID.match(e.getKey())) {
					return;
				}
			}*/
			//throw new VeyronException("No matching ACL entry found");
		}
	}
}

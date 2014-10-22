package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

public class Security {
	/**
	 * Mints a new private key and generates a Principal based on this key, storing its
	 * BlessingRoots and BlessingStore in memory.
	 *
	 * @return                 in-memory Principal using the newly minted private key.
	 * @throws VeyronException if the Principal couldn't be created.
	 */
	public static Principal newPrincipal() throws VeyronException {
		return PrincipalImpl.create();
	}

	/**
	 * Creates a Principal using the provided signer, storing its BlessingRoots and BlessingStore
	 * in memory.
	 *
	 * @param  signer          Signer to be used by the new Principal.
	 * @return                 in-memory Principal using the provided Signer.
	 * @throws VeyronException if the Principal couldn't be created.
	 */
	public static Principal newPrincipal(Signer signer) throws VeyronException {
		return PrincipalImpl.create(signer);
	}

	/**
	 * Creates a Principal using the provided Signer, BlessingStore, and BlessingRoots.
	 *
	 * @param  signer Signer to be used by the Principal.
	 * @param  store  BlessingStore to be used by the Principal.
	 * @param  roots  BlessingRoots to be used by the Principal.
	 * @return        newly created Principal.
	 */
	public static Principal newPrincipal(Signer signer, BlessingStore store, BlessingRoots roots)
		throws VeyronException {
		return PrincipalImpl.create(signer, store, roots);
	}

	/**
	 * Reads the entire state for a Principal (i.e., private key, BlessingRoots, BlessingStore) from
	 * the provided directory <code>dir</code> and commits all state changes to the same directory.
	 *
	 * If the directory does not contain state, a new private key is minted and all state of the
	 * Principal is committed to <code>dir</code>. If the directory does not exist, it is created.
	 *
	 * @param  passphrase      passphrase used to encrypt the private key.  If empty, no encryption
	 *                         takes place.
	 * @param  dir             directory where the state for a principal is to be persisted.
	 * @return                 Principal whose state is persisted in the provided directory.
	 * @throws VeyronException if the Principal couldn't be created.
	 */
	public static Principal newPersistentPrincipal(String passphrase, String dir)
			throws VeyronException {
		return PrincipalImpl.createPersistent(passphrase, dir);
	}

	/**
	 * Creates a new principal using the provided Signer and a partial state (i.e., BlessingRoots,
	 * BlessingStore) that is read from the provided directory <code>dir</code>.  Changes to the
	 * partial state are persisted and commited to the same directory; the provided signer isn't
	 * persisted: the caller is expected to persist it separately or use the
	 * <code>newPersistentPrincipal</code> method instead.
	 *
	 * If the directory does not contain any partial state, a new partial state instances are
	 * created and subsequently commited to <code>dir</code>.  If the directory does not exist, it
	 * is created.
	 *
	 * @param  signer          Signer to be used by the new Principal.
	 * @param  dir             directory where the partial state for a principal is to be persisted.
	 * @return                 Principal whose partial state is persisted in the provided directory.
	 * @throws VeyronException if the Principal couldn't be created.
	 */
	public static Principal newPersistentPrincipal(Signer signer, String dir)
		throws VeyronException {
		return PrincipalImpl.createPersistent(signer, dir);
	}

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

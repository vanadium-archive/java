package io.veyron.veyron.veyron2.services.security.access;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.Authorizer;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.Context;

import java.util.Arrays;

public class TaggedACLAuthorizer implements Authorizer {
	public static final String TAG = "Veyron runtime";
	private final TaggedACLMap acls;
	private final Class<?> type;

	public TaggedACLAuthorizer(TaggedACLMap acls, Class<?> type) {
		this.acls = acls;
		this.type = type;
	}

	@Override
	public void authorize(Context context) throws VeyronException {
		final Blessings local = context.localBlessings();
		final Blessings remote = context.remoteBlessings();
		final String[] blessings = remote != null ? remote.forContext(context) : new String[0];
		if (this.acls == null || this.type == null) {
			errorACLMatch(blessings);
		}
		if (local == null) {
			throw new VeyronException("Got null local blessings.");
		}
		if (local.publicKey() == null) {
			throw new VeyronException("Got null local public key.");
		}
		// Self-RPCs are always authorized.
		if (remote != null && remote.publicKey() != null &&
			Arrays.equals(local.publicKey().getEncoded(), remote.publicKey().getEncoded())) {
			return;
		}
		boolean grant = false;
		for (Object tag : context.methodTags()) {
			if (tag == null || !tag.getClass().equals(this.type)) {
				continue;
			}
			final ACL acl = this.acls.get(tag.toString());
			if (acl == null || !ACLWrapper.wrap(acl).includes(blessings)) {
				errorACLMatch(blessings);
			}
			grant = true;
		}
		if (!grant) {
			errorACLMatch(blessings);
		}
	}

	private void errorACLMatch(String[] blessings) throws VeyronException {
		throw new VeyronException(String.format("Blessings %s don't match ACL",
			Arrays.asList(blessings).toString()));
	}
}
package io.v.core.veyron2.security;

import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import io.v.core.veyron2.verror.VException;
import io.v.core.veyron2.services.security.access.TaggedACLAuthorizer;
import io.v.core.veyron2.services.security.access.TaggedACLMap;
import io.v.core.veyron2.util.VomUtil;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * Security class implements various functions used for creating and managing Veyron security
 * primitives.
 */
public class Security {

	/**
	 * Mints a new private key and creates a signer based on this key.  The key is stored
	 * in the clear in memory of the running process.
	 */
	public static Signer newInMemorySigner() throws VException {
		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
			keyGen.initialize(256);
			final KeyPair keyPair = keyGen.generateKeyPair();
		 	final PrivateKey privKey = keyPair.getPrivate();
		 	final ECPublicKey pubKey = (ECPublicKey) keyPair.getPublic();
		 	return new ECDSASigner(privKey, pubKey);
		} catch (NoSuchAlgorithmException e) {
			throw new VException("Couldn't mint private key: " + e.getMessage());
		}
	}

	/**
	 * Mints a new private key and generates a principal based on this key, storing its
	 * BlessingRoots and BlessingStore in memory.
	 *
	 * @return                 in-memory principal using the newly minted private key
	 * @throws VException      if the principal couldn't be created
	 */
	public static Principal newPrincipal() throws VException {
		return PrincipalImpl.create();
	}

	/**
	 * Creates a principal using the provided signer, storing its BlessingRoots and BlessingStore
	 * in memory.
	 *
	 * @param  signer          signer to be used by the new principal.
	 * @return                 in-memory principal using the provided signer.
	 * @throws VException      if the principal couldn't be created.
	 */
	public static Principal newPrincipal(Signer signer) throws VException {
		return PrincipalImpl.create(signer);
	}

	/**
	 * Creates a principal using the provided signer, BlessingStore, and BlessingRoots.
	 *
	 * @param  signer signer to be used by the principal.
	 * @param  store  BlessingStore to be used by the principal.
	 * @param  roots  BlessingRoots to be used by the principal.
	 * @return        newly created principal.
	 */
	public static Principal newPrincipal(Signer signer, BlessingStore store, BlessingRoots roots)
		throws VException {
		return PrincipalImpl.create(signer, store, roots);
	}

	/**
	 * Reads the entire state for a principal (i.e., private key, BlessingRoots, BlessingStore) from
	 * the provided directory {@code dir} and commits all state changes to the same directory.
	 *
	 * If the directory does not contain state, a new private key is minted and all state of the
	 * principal is committed to {@code dir}. If the directory does not exist, it is created.
	 *
	 * @param  passphrase      passphrase used to encrypt the private key.  If empty, no encryption
	 *                         is done.
	 * @param  dir             directory where the state for a principal is to be persisted.
	 * @return                 principal whose state is persisted in the provided directory.
	 * @throws VException      if the principal couldn't be created.
	 */
	public static Principal newPersistentPrincipal(String passphrase, String dir)
			throws VException {
		return PrincipalImpl.createPersistent(passphrase, dir);
	}

	/**
	 * Creates a new principal using the provided signer and a partial state (i.e., BlessingRoots,
	 * BlessingStore) that is read from the provided directory {@code dir}.  Changes to the
	 * partial state are persisted and commited to the same directory.  The provided signer isn't
	 * persisted: the caller is expected to persist it separately or use the
	 * {@code newPersistentPrincipal()} method instead.
	 *
	 * If the directory does not contain any partial state, a new partial state instances are
	 * created and subsequently commited to {@code dir}.  If the directory does not exist, it
	 * is created.
	 *
	 * @param  signer          signer to be used by the new principal.
	 * @param  dir             directory where the partial state for a principal is to be persisted.
	 * @return                 principal whose partial state is persisted in the provided directory.
	 * @throws VException      if the principal couldn't be created.
	 */
	public static Principal newPersistentPrincipal(Signer signer, String dir)
		throws VException {
		return PrincipalImpl.createPersistent(signer, dir);
	}

	/**
	 * Creates new blessings using the provided wire-encoded blessings.
	 *
	 * @param  wire            wire-encoded blessings.
	 * @return                 new blessings based on the wire-encoded blessings.
	 * @throws VException      if the blessings couldn't be created.
	 */
	public static Blessings newBlessings(WireBlessings wire) throws VException {
		return BlessingsImpl.create(wire);
	}

	/**
	 * Returns a {@code Blessings} object that carries the union of the provided blessings.
	 * All provided blessings must have the same public key.  Returns {@code null} if invoked
	 * without arguments.
	 *
	 * @param  blessings       blessings that will be merged.
	 * @return                 the union of the provided blessings.
	 * @throws VException      if there was an error creating an union.
	 */
	public static Blessings unionOfBlessings(Blessings... blessings) throws VException {
		return BlessingsImpl.createUnion(blessings);
	}

	/**
	 * Returns a caveat that requires validation by the validator corresponding to the
	 * given descriptor and uses the provided parameter.
	 *
	 * @param  desc            caveat descriptor
	 * @param  param           caveat parameter used by the associated validator
	 * @return                 caveat that requires validation by the validator corresponding to the
	 *                         given descriptor and uses the provided parameter
	 * @throws VException      if the caveat couldn't be created
	 */
	public static Caveat newCaveat(CaveatDescriptor desc, Object param) throws VException {
		final byte[] paramVOM = VomUtil.encode(param, desc.getParamType().getTypeObject());
		return new Caveat(desc.getId(), paramVOM);
	}

	/**
	 * Returns a caveat that validates iff the current time is before the provided time.
	 *
	 * @param  time            time before which the caveat validates
	 * @return                 caveat that validates if the current time is before the provided time
	 * @throws VException      if the caveat couldn't be created
	 */
	public static Caveat newExpiryCaveat(DateTime time) throws VException {
		return newCaveat(Constants.UNIX_TIME_EXPIRY_CAVEAT_X, time.getMillis() / 1000L);
	}

	/**
	 * Returns a caveat that validates iff the method being invoked by the peer is listed in an
	 * argument to this function.
	 *
	 * @param  method            name of the method for which this caveat should validate
	 * @param  additionalMethods additional method names for which this caveat should validate
	 * @return                   caveat that validates iff the method being invoked by the peer is
	 *                           one of the provided methods
	 * @throws VException        if the caveat couldn't be created
	 */
	public static Caveat newMethodCaveat(String method, String... additionalMethods)
			throws VException {
		final List<String> methods = ImmutableList.<String>builder()
				.add(method)
				.add(additionalMethods)
				.build();
		return newCaveat(Constants.METHOD_CAVEAT_X, methods);
	}

	/**
	 * Returns a caveat that never fails to validate. This is useful only for providing
	 * unconstrained blessings to another principal.
	 *
	 * @return a caveat that never fails to validate.
	 */
	public static Caveat newUnconstrainedUseCaveat() {
		return new Caveat(null, null);
	}

	/**
	 * Returns a new security context that uses the provided params.
	 *
	 * @param params context params
	 * @return       new security context that uses the provided params.
	 */
	public static VContext newContext(VContextParams params) {
		return new VContextParamImpl(params);
	}

	/**
	 * Returns an authorizer that subscribes to an authorization policy where access is granted if
	 * the remote end presents blessings included in the Access Control Lists (ACLs) associated with
	 * the set of relevant tags.
	 *
	 * The set of relevant tags is the subset of tags associated with the method
	 * ({@link io.v.core.veyron2.security.VContext#methodTags()}) that have the same type as
	 * the provided one.
	 * Currently, tagType.Kind must be reflect.String, i.e., only tags that are
	 * named string types are supported.
	 *
	 * If multiple tags of the provided type are associated with the method, then access is granted
	 * if the peer presents blessings that match the ACLs of each one of those tags. If no tags of
	 * the provided are associated with the method, then access is denied.
	 *
	 * If the TaggedACLMap provided is {@code null}, then an authorizer that rejects all remote
	 * ends is returned.
	 *
	 * Sample usage:
	 *
	 * (1) Attach tags to methods in the VDL (eg. myservice.vdl)
	 * <code>
	 *   package myservice
	 *
	 *   type MyTag string
	 *   const (
	 *     ReadAccess  = MyTag("R")
	 *     WriteAccess = MyTag("W")
	 *   )
	 *
	 *   type MyService interface {
	 *     Get() ([]string, error)       {ReadAccess}
	 *     GetIndex(int) (string, error) {ReadAccess}
	 *
	 *     Set([]string) error           {WriteAccess}
	 *     SetIndex(int, string) error   {WriteAccess}
	 *
	 *     GetAndSet([]string) ([]string, error) {ReadAccess, WriteAccess}
	 *   }
	 * </code>
	 * (2) Setup the dispatcher to use the {@code TaggedACLAuthorizer}
	 * <code>
	 *   public class MyDispatcher implements io.v.core.veyron2.ipc.Dispatcher {
	 *     @Override
	 *     public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
	 *       final TaggedACLMap acls = new TaggedACLMap(ImmutableMap.of(
	 *         "R", new ACL(ImmutableList.of(new BlessingPattern("alice/friends/..."),
	 *                                       new BlessingPattern("alice/family/...")),
	 *                      null),
	 *           "W", new ACL(ImmutableList.of(new BlessingPattern("alice/family/..."),
	 *                                         new BlessingPattern("alice/colleagues/...")),
	 *                      null)));
	 *       return new ServiceObjectWithAuthorizer(
	 *          newInvoker(), Security.newTaggedACLAuthorizer(acls, MyTag.class));
	 *   }
	 * </code>
	 *
	 * With the above dispatcher, the server will grant access to a peer with the blessing
	 * {@code "alice/friend/bob"} access only to the {@code Get} and {@code GetIndex} methods.
	 * A peer presenting the blessing "alice/colleague/carol" will get access only to the
	 * {@code Set} and {@code SetIndex} methods. A peer presenting {@code "alice/family/mom"} will
	 * get access to all methods, even {@code GetAndSet} - which requires that the blessing appear
	 * in the ACLs for both the {@code ReadAccess} and {@code WriteAccess} tags.
	 *
	 * @param  acls            ACLs containing authorization rules.
	 * @param  type            type of the method tags this authorizer checks.
	 * @return                 an above-described authorizer.
	 * @throws VException      if the authorizer couldn't be created.
	 */
	public static Authorizer newTaggedACLAuthorizer(TaggedACLMap acls, Class<?> type)
		throws VException {
		return new TaggedACLAuthorizer(acls, type);
	}

	/**
	 * Returns an authorizer that accepts all requests.
	 *
	 * @return an authorizer that accepts all requests.
	 */
	public static Authorizer newAcceptAllAuthorizer() {
		return new Authorizer() {
			@Override
			public void authorize(VContext context) throws VException {
				// do nothing
			}
		};
	}

	/**
	 * Verifies the provides signature of the given message, using the supplied public key.
	 *
	 * @param  sig             signature in the veyron format.
	 * @param  key             public key.
	 * @param  message         message whose signature is verified.
	 * @throws VException      iff the signature doesn't verify.
	 */
	public static void verifySignature(Signature sig, ECPublicKey key, byte[] message)
		throws VException {
		final String hashAlgorithm = sig.getHash().getValue();
		final String verifyAlgorithm = hashAlgorithm + "withECDSA";
		try {
			message = CryptoUtil.messageDigest(hashAlgorithm, message, sig.getPurpose());
			final byte[] jSig = CryptoUtil.javaSignature(sig);
			final java.security.Signature verifier = java.security.Signature.getInstance(hashAlgorithm + "withECDSA");
			verifier.initVerify(key);
			verifier.update(message);
			if (!verifier.verify(jSig)) {
				throw new VException("Signature doesn't verify.");
			}
		} catch (NoSuchAlgorithmException e) {
			throw new VException("Verifying algorithm " + verifyAlgorithm +
					" not supported by the runtime: " + e.getMessage());
		} catch (InvalidKeyException e) {
			throw new VException("Invalid private key: " + e.getMessage());
		} catch (SignatureException e) {
			throw new VException(
				"Invalid signing data [ " + Arrays.toString(message) + " ]: " + e.getMessage());
		}
	}
}
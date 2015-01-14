package io.v.core.veyron2.security;

import com.google.common.collect.ImmutableMap;

import android.test.AndroidTestCase;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.android.V;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Map;

/**
 * Tests the default {@code BlessingRoot} implementation.
 */
public class BlessingRootsTest extends AndroidTestCase {
	public void testRecognized() {
		try {
			V.init(getContext(), null);
			final Principal principal = Security.newPrincipal();
			final BlessingRoots roots = principal.roots();
			final ECPublicKey[] keys = { mintPublicKey(), mintPublicKey(), mintPublicKey() };
			roots.add(keys[0], new BlessingPattern("veyron/..."));
			roots.add(keys[1], new BlessingPattern("google/foo/..."));
			roots.add(keys[0], new BlessingPattern("google"));

			final Map<ECPublicKey, String[]> recognized =
					ImmutableMap.<ECPublicKey, String[]>builder()
					.put(keys[0],
							new String[]{ "veyron", "veyron/foo", "veyron/foo/bar", "google" })
					.put(keys[1], new String[]{ "google", "google/foo", "google/foo/bar" })
					.put(keys[2], new String[]{ })
					.build();
			final Map<ECPublicKey, String[]> notRecognized =
					ImmutableMap.<ECPublicKey, String[]>builder()
					.put(keys[0], new String[]{ "google/foo", "foo", "foo/bar" })
					.put(keys[1],
							new String[]{ "google/bar", "veyron", "veyron/foo", "foo", "foo/bar" })
					.put(keys[2], new String[] { "veyron", "veyron/foo", "veyron/bar", "google",
							"google/foo", "google/bar", "foo", "foo/bar" })
					.build();
			for (Map.Entry<ECPublicKey, String[]> entry : recognized.entrySet()) {
				final ECPublicKey key = entry.getKey();
				for (String blessing : entry.getValue()) {
					try {
						roots.recognized(key, blessing);
					} catch (VeyronException e) {
						fail("Didn't recognize root: " + entry.getKey() +
								" as an authority for blessing: " + blessing);
					}
				}
			}
			for (Map.Entry<ECPublicKey, String[]> entry : notRecognized.entrySet()) {
				final ECPublicKey key = entry.getKey();
				for (String blessing : entry.getValue()) {
					try {
						roots.recognized(key, blessing);
						fail("Shouldn't recognize root: " + entry.getKey() +
								" as an authority for blessing: " + blessing);
					} catch (VeyronException e) {
						// OK
					}
				}
			}
		} catch (VeyronException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public ECPublicKey mintPublicKey() throws VeyronException {
		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
			keyGen.initialize(256);
			return (ECPublicKey) keyGen.generateKeyPair().getPublic();
		} catch (NoSuchAlgorithmException e) {
			throw new VeyronException("Couldn't mint private key: " + e.getMessage());
		}
	}
}

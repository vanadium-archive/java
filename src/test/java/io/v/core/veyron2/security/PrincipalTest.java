package io.v.core.veyron2.security;

import com.google.common.collect.ImmutableMap;

import android.test.AndroidTestCase;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.android.V;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;


/**
 * Tests the default {@code Principal} implementation.
 */
public class PrincipalTest extends AndroidTestCase {
	public void testBlessingsInfo() {
		try {
			V.init(getContext(), null);
			final Principal p1 = Security.newPrincipal();
			final Principal p2 = Security.newPrincipal();
			final Blessings alice = p1.blessSelf("alice");
			p2.addToRoots(alice);

			final Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
					alice, "work/friend", Security.newUnconstrainedUseCaveat());
			final Blessings aliceGymFriend = p1.bless(p2.publicKey(),
					alice, "gym/friend", Security.newUnconstrainedUseCaveat());
			final Blessings aliceAllFriends = Security.unionOfBlessings(
				aliceWorkFriend, aliceGymFriend);
			assertInfoMapsEqual(ImmutableMap.<String, Caveat[]>builder()
					.put("alice/work/friend", new Caveat[0])
					.build(), p2.blessingsInfo(aliceWorkFriend));
			assertInfoMapsEqual(ImmutableMap.<String, Caveat[]>builder()
					.put("alice/gym/friend", new Caveat[0])
					.build(), p2.blessingsInfo(aliceGymFriend));
			assertInfoMapsEqual(ImmutableMap.<String, Caveat[]>builder()
					.put("alice/work/friend", new Caveat[0])
					.put("alice/gym/friend", new Caveat[0])
					.build(), p2.blessingsInfo(aliceAllFriends));
		} catch (VeyronException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	private void assertInfoMapsEqual(Map<String, Caveat[]> want, Map<String, Caveat[]> got) {
		assertEquals(want.size(), got.size());
		final String[] keysWant = want.keySet().toArray(new String[0]);
		final String[] keysGot = got.keySet().toArray(new String[0]);
		Arrays.sort(keysWant);
		Arrays.sort(keysGot);
		android.util.Log.e("Test", String.format("Comparing %s with %s", Arrays.toString(keysWant), Arrays.toString(keysGot)));
		assertTrue(Arrays.equals(keysWant, keysGot));
		for (String key : keysWant) {
			final Caveat[] caveatsWant = want.get(key);
			final Caveat[] caveatsGot = got.get(key);
			assertTrue(Arrays.equals(caveatsWant, caveatsGot));
		}
	}

	public void testBlessingsByName() {
		try {
			V.init(getContext(), null);
			final Principal p1 = Security.newPrincipal();
			final Principal p2 = Security.newPrincipal();
			final Principal p3 = Security.newPrincipal();
			final Blessings alice = p1.blessSelf("alice");
			final Blessings fake = p3.blessSelf("alice");

			final Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
					alice, "work/friend", Security.newUnconstrainedUseCaveat());
			final Blessings aliceGymFriend = p1.bless(p2.publicKey(),
					alice, "gym/friend", Security.newUnconstrainedUseCaveat());
			final Blessings aliceWorkBoss = p1.bless(p2.publicKey(),
					alice, "work/boss", Security.newUnconstrainedUseCaveat());
			final Blessings fakeFriend = p3.bless(p2.publicKey(),
					fake, "work/friend", Security.newUnconstrainedUseCaveat());

			p2.addToRoots(alice);
			p2.blessingStore().set(aliceWorkFriend, new BlessingPattern("alice/work/friend"));
			p2.blessingStore().set(aliceGymFriend, new BlessingPattern("alice/gym/friend"));
			p2.blessingStore().set(aliceWorkBoss, new BlessingPattern("alice/work/boss"));
			p2.blessingStore().set(fakeFriend, new BlessingPattern("fake/work/friend"));

			final Map<Blessings[], BlessingPattern> testdata =
					ImmutableMap.<Blessings[], BlessingPattern>builder()
					.put(new Blessings[]{ aliceWorkFriend, aliceWorkBoss },
							new BlessingPattern("alice/work/..."))
					.put(new Blessings[] { aliceWorkFriend },
							new BlessingPattern("alice/work/friend"))
					.put(new Blessings[] { aliceGymFriend },
							new BlessingPattern("alice/gym/friend"))
					.put(new Blessings[] { aliceWorkFriend, aliceGymFriend, aliceWorkBoss },
							new BlessingPattern("alice/..."))
					.put(new Blessings[] { aliceWorkFriend, aliceGymFriend, aliceWorkBoss },
							new BlessingPattern("..."))
					.put(new Blessings[] {}, new BlessingPattern("alice/school/..."))
					.build();
			for (Map.Entry<Blessings[], BlessingPattern> entry : testdata.entrySet()) {
				final Blessings[] want = entry.getKey();
				final Blessings[] got = p2.blessingsByName(entry.getValue());
				final Comparator<Blessings> comp = new Comparator<Blessings>() {
					@Override
					public int compare(Blessings b1, Blessings b2) {
						final int h1 = b1.hashCode(),  h2 = b2.hashCode();
						return h1 < h2 ? -1 : (h1 == h2 ? 0 : 1);
					}
				};
				Arrays.sort(want, comp);
				Arrays.sort(got, comp);
				assertTrue(Arrays.equals(want, got));
			}
		} catch (VeyronException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}
}
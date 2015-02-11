package io.v.core.veyron2.verror2;

import android.test.AndroidTestCase;

import io.v.core.veyron2.android.V;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.context.VContextImpl;
import io.v.core.veyron2.i18n.Catalog;
import io.v.core.veyron2.i18n.Language;

/**
 * VExceptionTest tests the {@code VException} implementation.
 */
public class VExceptionTest extends AndroidTestCase {
	// Some languages
	private static final String EN = "en";
	private static final String FR = "fr";
	private static final String DE = "de";

	private static final VException.IDAction idActionA;
	private static final VException.IDAction idActionB;
	private static final VException.IDAction idActionC;

	private static final VException aEN0;
	private static final VException aEN1;
	private static final VException aFR0;
	private static final VException aFR1;
	private static final VException aDE0;
	private static final VException aDE1;

	private static final VException bEN0;
	private static final VException bEN1;
 	private static final VException bFR0;
 	private static final VException bFR1;
	private static final VException bDE0;
	private static final VException bDE1;

	private static final VException nEN0;
	private static final VException nEN1;
	private static final VException nFR0;
	private static final VException nFR1;
	private static final VException nDE0;
	private static final VException nDE1;

	static {
		V.init();

		idActionA = VException.register(
			"A", VException.ActionCode.NO_RETRY, "{1} {2} error A {_}");
		idActionB = VException.register(
			"B", VException.ActionCode.RETRY_BACKOFF, "{1} {2} problem B {_}");
		idActionC = VException.register(
			"C", VException.ActionCode.NO_RETRY, "");

		{
			VContext ctx = VContextImpl.create();
			ctx = Language.contextWithLanguage(ctx, EN);
			ctx = VException.contextWithComponentName(ctx, "VExceptionTest");
			VException.setDefaultContext(ctx);
		}

		final Catalog cat = Language.getDefaultCatalog();
		// Add messages for French.  Do not set messages for German, to test the case where the
		// messages are not present.
		cat.set(FR, idActionA.getID(), "{1} {2} erreur A {_}");
		cat.set(FR, idActionB.getID(), "{1} {2} problème B {_}");

		// Set English and French messages for UNKNOWN and NO_EXIST to ones the test can predict.
		// Delete any German messages that may be present.
		cat.set(EN, Errors.UNKNOWN.getID(), "{1} {2} unknown error {_}");
		cat.set(FR, Errors.UNKNOWN.getID(), "{1} {2} erreur inconnu {_}");
		cat.set(DE, Errors.UNKNOWN.getID(), "");

		cat.set(EN, Errors.NO_EXIST.getID(), "{1} {2} not found {_}");
		cat.set(FR, Errors.NO_EXIST.getID(), "{1} {2} pas trouvé {_}");
		cat.set(DE, Errors.NO_EXIST.getID(), "");

		VContext ctx = VContextImpl.create();
		ctx = Language.contextWithLanguage(ctx, FR);
		ctx = VException.contextWithComponentName(ctx, "FooServer");

		// A first IDAction in various languages.
		aEN0 = VException.explicitMake(idActionA, EN, "server", "aEN0", 0);
		aEN1 = VException.explicitMake(idActionA, EN, "server", "aEN1", 1, 2);
		aFR0 = VException.explicitMake(idActionA, FR, "server", "aFR0", 0);
		aFR1 = VException.make(idActionA, ctx, 1, 2);
		aDE0 = VException.explicitMake(idActionA, DE, "server", "aDE0", 0);
		aDE1 = VException.explicitMake(idActionA, DE, "server", "aDE1", 1, 2);

		// A second IDAction in various languages.
		bEN0 = VException.explicitMake(idActionB, EN, "server", "bEN0", 0);
		bEN1 = VException.explicitMake(idActionB, EN, "server", "bEN1", 1, 2);
		bFR0 = VException.explicitMake(idActionB, FR, "server", "bFR0", 0);
		bFR1 = VException.explicitMake(idActionB, FR, "server", "bFR1", 1, 2);
		bDE0 = VException.explicitMake(idActionB, DE, "server", "bDE0", 0);
		bDE1 = VException.explicitMake(idActionB, DE, "server", "bDE1", 1, 2);

		// The NoExist error in various languages.
		nEN0 = VException.explicitMake(Errors.NO_EXIST, EN, "server", "nEN0", 0);
		nEN1 = VException.explicitMake(Errors.NO_EXIST, EN, "server", "nEN1", 1, 2);
		nFR0 = VException.explicitMake(Errors.NO_EXIST, FR, "server", "nFR0", 0);
		nFR1 = VException.explicitMake(Errors.NO_EXIST, FR, "server", "nFR1", 1, 2);
		nDE0 = VException.explicitMake(Errors.NO_EXIST, DE, "server", "nDE0", 0);
		nDE1 = VException.explicitMake(Errors.NO_EXIST, DE, "server", "nDE1", 1, 2);
	}

	public static void testBasic() {
		expectBasic(aEN0, idActionA, "server aEN0 error A 0", 1);
		expectBasic(aEN1, idActionA, "server aEN1 error A 1 2", 2);
		expectBasic(aFR0, idActionA, "server aFR0 erreur A 0", 3);
		expectBasic(aFR1, idActionA, "FooServer  erreur A 1 2", 4);
		expectBasic(aDE0, idActionA, "A: server aDE0 0", 5);
		expectBasic(aDE1, idActionA, "A: server aDE1 1 2", 6);

		expectBasic(bEN0, idActionB, "server bEN0 problem B 0", 7);
		expectBasic(bEN1, idActionB, "server bEN1 problem B 1 2", 8);
		expectBasic(bFR0, idActionB, "server bFR0 problème B 0", 9);
		expectBasic(bFR1, idActionB, "server bFR1 problème B 1 2", 10);
		expectBasic(bDE0, idActionB, "B: server bDE0 0", 11);
		expectBasic(bDE1, idActionB, "B: server bDE1 1 2", 12);

		expectBasic(nEN0, Errors.NO_EXIST, "server nEN0 not found 0", 13);
		expectBasic(nEN1, Errors.NO_EXIST, "server nEN1 not found 1 2", 14);
		expectBasic(nFR0, Errors.NO_EXIST, "server nFR0 pas trouvé 0", 15);
		expectBasic(nFR1, Errors.NO_EXIST, "server nFR1 pas trouvé 1 2", 16);
		expectBasic(nDE0, Errors.NO_EXIST, "v.io/core/veyron2/verror.NoExist: server nDE0 0", 17);
		expectBasic(nDE1, Errors.NO_EXIST, "v.io/core/veyron2/verror.NoExist: server nDE1 1 2", 18);
	}

	public static void testEqual() {
		expectEqual(aEN0, aEN1, aDE0, aDE1, aDE0, aDE1);
		expectEqual(bEN0, bEN1, bDE0, bDE1, bDE0, bDE1);
		expectEqual(nEN0, nEN1, nDE0, nDE1, nDE0, nDE1);
	}

	private static void expectBasic(
		VException error, VException.IDAction idAction, String msg, int tag) {
		if (!error.getID().equals(idAction.getID())) {
			fail(String.format("%d: (%s).getID(); got %s, want %s", tag, error, error.getID(),
					idAction.getID()));
		}
		if (!error.getAction().equals(idAction.getAction())) {
			fail(String.format("%d: (%s).getAction(); got %s, want %s", tag, error,
					error.getAction(), idAction.getAction()));
		}
		if (!error.getMessage().equals(msg)) {
			fail(String.format("%d: (%s).getMessage(); got %s, want %s", tag, error,
					error.getMessage(), msg));
		}
		if (!error.is(idAction.getID())) {
			fail(String.format("%d: (%s).is(%s) == false, want true", tag, error,
					idAction.getID()));
		}
		if (!error.is(idAction)) {
			fail(String.format("%d: (%s).is(%s) == false, want true", tag, error,
					idAction));
		}
		if (error.is(idActionC.getID())) {
			fail(String.format("%d: (%s).is(%s) == true, want false", tag, error,
					idActionC.getID()));
		}
		if (error.is(idActionC)) {
			fail(String.format("%d: (%s).is(%s) == true, want false", tag, error,
					idActionC));
		}
	}

	private static void expectEqual(VException... errors) {
		for (VException error1 : errors) {
			for (VException error2 : errors) {
				if (!error1.equals(error2)) {
					fail(String.format("(%s) != (%s)", error1, error2));
				}
			}
		}
	}
}

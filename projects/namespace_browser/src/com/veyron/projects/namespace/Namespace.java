package com.veyron.projects.namespace;

import org.joda.time.Duration;

import io.v.core.veyron2.InputChannel;
import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.android.V;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.naming.VDLMountEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Namespace provides utility methods for Veyron namespace.
 */
public class Namespace {
	/**
	 * Returns the list of entries mounted under the provided namespace root.
	 * @param root             root of the namespace
	 * @return                 list of entries mounted under the provided root.
	 * @throws VeyronException if there was an error fetching the entries.
	 */
	public static List<VDLMountEntry> glob(String root, VContext ctx) throws VeyronException {
		final io.v.core.veyron2.naming.Namespace n = V.getNamespace(ctx);
		final VContext ctxT = ctx.withTimeout(new Duration(20000));  // 20s
		final InputChannel<VDLMountEntry> chan = n.glob(ctxT, root.isEmpty() ? "*" : root + "/*");
		final ArrayList<VDLMountEntry> entries = new ArrayList<VDLMountEntry>();
		try {
			while (true) {
				entries.add(chan.readValue());
			}
		} catch (java.io.EOFException e) {
			// We're done.
			return entries;
		}
	}
}
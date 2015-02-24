package io.v.v23.naming;

import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

import io.v.v23.InputChannel;

/**
 * Namespace provides translation from object names to server object addresses.  It represents the
 * interface to a client side library for the MountTable service.
 */
public interface Namespace {
    /**
     * Returns all names matching the provided pattern.
     *
     * @param  context         a client context.
     * @param  pattern         a pattern that should be matched.
     * @return                 an input channel of MountEntry objects matching the provided pattern.
     * @throws VException      if an error is encountered.
     */
    public InputChannel<VDLMountEntry> glob(VContext context, String pattern) throws VException;
}
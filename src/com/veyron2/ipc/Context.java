package com.veyron2.ipc;

/**
 * Context defines a context under which outgoing RPC calls are made.  It
 * carries some setting information, but also creates relationships between RPCs
 * executed under the same context.
 * TODO(spetrovic): Add Deadline and other settings.
 */
public interface Context {}
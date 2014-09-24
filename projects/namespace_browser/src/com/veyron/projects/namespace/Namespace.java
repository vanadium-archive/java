package com.veyron.projects.namespace;

import io.veyron.veyron.veyron2.InputChannel;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.naming.MountEntry;

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
  public static List<MountEntry> glob(String root) throws VeyronException {
    final io.veyron.veyron.veyron2.Runtime r = RuntimeFactory.defaultRuntime();
    final io.veyron.veyron.veyron2.naming.Namespace n = r.getNamespace();
    final InputChannel<MountEntry> chan = n.glob(null, root + "/*");
    final ArrayList<MountEntry> entries = new ArrayList<MountEntry>();
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
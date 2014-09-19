package net.veyron;

import com.veyron2.InputChannel;
import com.veyron2.RuntimeFactory;
import com.veyron2.ipc.VeyronException;
import com.veyron2.naming.MountEntry;

import java.util.ArrayList;
import java.util.List;

public class Namespace {
  public static List<String> glob(String root) throws VeyronException {
    final com.veyron2.Runtime r = RuntimeFactory.defaultRuntime();
    final com.veyron2.naming.Namespace n = r.getNamespace();
    final InputChannel<MountEntry> chan = n.glob(null, root + "/*");
    final ArrayList<String> names = new ArrayList<String>();
    try {
      while (true) {
        final MountEntry entry = chan.readValue();
        names.add(entry.getName());
      }
    } catch (java.io.EOFException e) {
      // We're done.
      return names;
    }
  }
}

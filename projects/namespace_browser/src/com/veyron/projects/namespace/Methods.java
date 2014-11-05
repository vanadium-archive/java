package com.veyron.projects.namespace;

import com.google.common.reflect.TypeToken;

import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.VRuntime;
import io.veyron.veyron.veyron2.ipc.Client;
import io.veyron.veyron.veyron2.ipc.Client.Call;
import io.veyron.veyron.veyron2.ipc.ServiceSignature;
import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.util.ArrayList;
import java.util.List;

/**
 * Namespace provides utility methods for working with Veyron object methods.
 */
public class Methods {
/**
   * Returns the list of method names of the provided object.
   *
   * @param name             a name of the object
   * @return                 list of method names of the provided object.
   * @throws VeyronException if there was an error getting the list of names.
   */
	public static List<String> get(String name) throws VeyronException {
		final VRuntime r = RuntimeFactory.defaultRuntime();
		final Client client = r.getClient();
		final Call call = client.startCall(null, name, "signature", new Object[0]);
		final TypeToken<?>[] resultTypes = new TypeToken<?>[]{ new TypeToken<ServiceSignature>() {} };
		final ServiceSignature sSign = (ServiceSignature)call.finish(resultTypes)[0];
		final List<String> ret = new ArrayList<String>();
		if (sSign.getMethods() != null) {
			for (String method : sSign.getMethods().keySet()) {
				ret.add(method);
			}
		}
		return ret;
	}
}

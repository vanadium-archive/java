package io.v.android.apps.namespace_browser;

import com.google.common.reflect.TypeToken;

import org.joda.time.Duration;

import io.v.core.veyron2.android.V;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.ipc.Client;
import io.v.core.veyron2.ipc.Client.Call;
import io.v.core.veyron2.verror2.VException;

import java.lang.reflect.Type;
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
   * @throws VException      if there was an error getting the list of names.
   */
	public static List<String> get(String name, VContext ctx) throws VException {
		final Client client = V.getClient(ctx);
		final VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
		final Call call = client.startCall(ctxT, name, "signature", new Object[0], new Type[0]);
		final Type[] resultTypes = new Type[]{ new TypeToken<ServiceSignature>() {}.getType() };
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

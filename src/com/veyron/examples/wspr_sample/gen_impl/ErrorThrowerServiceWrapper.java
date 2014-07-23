// This file was auto-generated by the veyron vdl tool.
// Source(s):  cache.vdl error_thrower.vdl
package com.veyron.examples.wspr_sample.gen_impl;

import com.veyron.examples.wspr_sample.Cache;
import com.veyron.examples.wspr_sample.CacheFactory;
import com.veyron.examples.wspr_sample.CacheService;
import com.veyron.examples.wspr_sample.ErrorThrower;
import com.veyron.examples.wspr_sample.ErrorThrowerFactory;
import com.veyron.examples.wspr_sample.ErrorThrowerService;
import com.veyron.examples.wspr_sample.KeyValuePair;
import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import java.util.ArrayList;

public class ErrorThrowerServiceWrapper {

	private final ErrorThrowerService service;

	public ErrorThrowerServiceWrapper(ErrorThrowerService service) {
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) throws VeyronException { 
		if ("throwAborted".equals(method)) {
			return new Object[]{  };
		}
		if ("throwBadArg".equals(method)) {
			return new Object[]{  };
		}
		if ("throwBadProtocol".equals(method)) {
			return new Object[]{  };
		}
		if ("throwInternal".equals(method)) {
			return new Object[]{  };
		}
		if ("throwNotAuthorized".equals(method)) {
			return new Object[]{  };
		}
		if ("throwNotFound".equals(method)) {
			return new Object[]{  };
		}
		if ("throwUnknown".equals(method)) {
			return new Object[]{  };
		}
		if ("throwGoError".equals(method)) {
			return new Object[]{  };
		}
		if ("throwCustomStandardError".equals(method)) {
			return new Object[]{  };
		}
		if ("listAllBuiltInErrorIDs".equals(method)) {
			return new Object[]{  };
		}
        if ("getMethodTags".equals(method)) {
            return new Object[]{};
        }
		throw new VeyronException("method: " + method + " not found");
	}
	// Methods from interface ErrorThrower.
	public void throwAborted(ServerCall call) throws VeyronException { 
		this.service.throwAborted(call);
	}
	public void throwBadArg(ServerCall call) throws VeyronException { 
		this.service.throwBadArg(call);
	}
	public void throwBadProtocol(ServerCall call) throws VeyronException { 
		this.service.throwBadProtocol(call);
	}
	public void throwInternal(ServerCall call) throws VeyronException { 
		this.service.throwInternal(call);
	}
	public void throwNotAuthorized(ServerCall call) throws VeyronException { 
		this.service.throwNotAuthorized(call);
	}
	public void throwNotFound(ServerCall call) throws VeyronException { 
		this.service.throwNotFound(call);
	}
	public void throwUnknown(ServerCall call) throws VeyronException { 
		this.service.throwUnknown(call);
	}
	public void throwGoError(ServerCall call) throws VeyronException { 
		this.service.throwGoError(call);
	}
	public void throwCustomStandardError(ServerCall call) throws VeyronException { 
		this.service.throwCustomStandardError(call);
	}
	public ArrayList<String> listAllBuiltInErrorIDs(ServerCall call) throws VeyronException { 
		return this.service.listAllBuiltInErrorIDs(call);
	}
}

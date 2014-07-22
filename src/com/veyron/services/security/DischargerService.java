// This file was auto-generated by the veyron vdl tool.
// Source: discharger.vdl
package com.veyron.services.security;

import com.veyron.services.security.gen_impl.DischargerServiceWrapper;
import com.veyron2.ipc.ServerContext;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.VeyronService;

/**
 * DischargeIssuer service issues caveat discharges when requested.
 */
@VeyronService(serviceWrapper=DischargerServiceWrapper.class)
public interface DischargerService { 
	// Discharge is called by a principal that holds a blessing with a third
// party caveat and seeks to get a discharge that proves the fulfillment of
// this caveat.
// Caveat and Discharge are of type ThirdPartyCaveat and ThirdPartyDischarge
// respectively. (not enforced here because vdl does not know these types)
// TODO(ataly,ashankar): Figure out a VDL representation for ThirdPartyCaveat
// and Discharge and use those here?
	public Object discharge(ServerContext context, Object caveat) throws VeyronException;
}

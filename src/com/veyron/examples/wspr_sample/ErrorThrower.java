// This file was auto-generated by the veyron vdl tool.
// Source: error_thrower.vdl
package com.veyron.examples.wspr_sample;

import com.veyron2.Options;
import com.veyron2.ipc.Context;
import com.veyron2.ipc.VeyronException;
import java.util.ArrayList;

/**
 * A testing interface with methods that throw various types of errors
 */
public interface ErrorThrower { 
	// Throws veyron2/vError.Aborted error
	public void throwAborted(Context context) throws VeyronException;
	public void throwAborted(Context context, Options veyronOpts) throws VeyronException;
	// Throws veyron2/vError.BadArg error
	public void throwBadArg(Context context) throws VeyronException;
	public void throwBadArg(Context context, Options veyronOpts) throws VeyronException;
	// Throws veyron2/vError.BadProtocol error
	public void throwBadProtocol(Context context) throws VeyronException;
	public void throwBadProtocol(Context context, Options veyronOpts) throws VeyronException;
	// Throws veyron2/vError.Internal error
	public void throwInternal(Context context) throws VeyronException;
	public void throwInternal(Context context, Options veyronOpts) throws VeyronException;
	// Throws veyron2/vError.NotAuthorized error
	public void throwNotAuthorized(Context context) throws VeyronException;
	public void throwNotAuthorized(Context context, Options veyronOpts) throws VeyronException;
	// Throws veyron2/vError.NotFound error
	public void throwNotFound(Context context) throws VeyronException;
	public void throwNotFound(Context context, Options veyronOpts) throws VeyronException;
	// Throws veyron2/vError.Unknown error
	public void throwUnknown(Context context) throws VeyronException;
	public void throwUnknown(Context context, Options veyronOpts) throws VeyronException;
	// Throws normal Go error
	public void throwGoError(Context context) throws VeyronException;
	public void throwGoError(Context context, Options veyronOpts) throws VeyronException;
	// Throws custom error created by using Standard
	public void throwCustomStandardError(Context context) throws VeyronException;
	public void throwCustomStandardError(Context context, Options veyronOpts) throws VeyronException;
	// Lists all errors Ids available in veyron2/verror
	public ArrayList<String> listAllBuiltInErrorIDs(Context context) throws VeyronException;
	public ArrayList<String> listAllBuiltInErrorIDs(Context context, Options veyronOpts) throws VeyronException;
}

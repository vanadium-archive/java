package io.v.core.veyron2.context;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.v.core.veyron2.VeyronException;

import java.util.concurrent.CountDownLatch;

public class ContextImpl extends CancelableContext {
	private static final String TAG = "Veyron runtime";

	private long nativePtr;
	private long nativeCancelPtr;  // zero for non-cancelable contexts.

	private native DateTime nativeDeadline(long nativePtr) throws VeyronException;
	private native CountDownLatch nativeDone(long nativePtr) throws VeyronException;
	private native Object nativeValue(long nativePtr, Object key) throws VeyronException;
	private native CancelableContext nativeWithCancel(long nativePtr) throws VeyronException;
	private native CancelableContext nativeWithDeadline(long nativePtr, DateTime deadline)
		throws VeyronException;
	private native CancelableContext nativeWithTimeout(long nativePtr, Duration timeout)
		throws VeyronException;
	private native Context nativeWithValue(long nativePtr, Object key, Object value)
		throws VeyronException;
	private native void nativeCancel(long nativeCancelPtr) throws VeyronException;
	private native void nativeFinalize(long nativePtr, long nativeCancelPtr);

	private ContextImpl(long nativePtr, long nativeCancelPtr) {
		this.nativePtr = nativePtr;
		this.nativeCancelPtr = nativeCancelPtr;
	}
	@Override
	public DateTime deadline() {
		try {
				return nativeDeadline(this.nativePtr);
		} catch (VeyronException e) {
				android.util.Log.e(TAG, "Couldn't get deadline: " + e.getMessage());
			return null;
		}
	}
	@Override
	public CountDownLatch done() {
		try {
				return nativeDone(this.nativePtr);
		} catch (VeyronException e) {
				android.util.Log.e(TAG, "Couldn't invoke done: " + e.getMessage());
				return null;
		}
	}
	@Override
	public Object value(Object key) {
		try {
			return nativeValue(this.nativePtr, key);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get value: " + e.getMessage());
			return null;
		}
	}
	@Override
	public CancelableContext withCancel() {
		try {
			return nativeWithCancel(this.nativePtr);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't create cancelable Context: " + e.getMessage());
		}
	}
	@Override
	public CancelableContext withDeadline(DateTime deadline) {
		try {
			return nativeWithDeadline(this.nativePtr, deadline);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't create Context with deadline: " + e.getMessage());
		}
	}
	@Override
	public CancelableContext withTimeout(Duration timeout) {
		try {
			return nativeWithTimeout(this.nativePtr, timeout);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't create Context with timeout: " + e.getMessage());
		}
	}
	@Override
	public io.v.core.veyron2.context.Context withValue(Object key, Object value) {
		try {
			return nativeWithValue(this.nativePtr, key, value);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't create Context with data: " + e.getMessage());
		}
	}
	@Override
	public void cancel() {
		try {
			nativeCancel(this.nativeCancelPtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't cancel Context: " + e.getMessage());
		}
	}
	@Override
	void implementationsOnlyInThisPackage() {}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr, this.nativeCancelPtr);
	}
	private long nativePtr() {
		return this.nativePtr;
	}
}
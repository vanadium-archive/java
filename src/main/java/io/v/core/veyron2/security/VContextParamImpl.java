package io.v.core.veyron2.security;

import org.joda.time.DateTime;

class VContextParamImpl implements io.v.core.veyron2.security.VContext {
	private final VContextParams params;

	VContextParamImpl(VContextParams params) {
		this.params = params;
	}

	@Override
	public DateTime timestamp() {
		return this.params.getTimestamp();
	}
	@Override
	public String method() {
		return this.params.getMethod();
	}
	@Override
	public Object[] methodTags() {
		return this.params.getMethodTags();
	}
	@Override
	public String name() {
		return this.params.getName();
	}
	@Override
	public String suffix() {
		return this.params.getSuffix();
	}
	@Override
	public String localEndpoint() {
		return this.params.getLocalEndpoint();
	}
	@Override
	public String remoteEndpoint() {
		return this.params.getRemoteEndpoint();
	}
	@Override
	public Principal localPrincipal() {
		return this.params.getLocalPrincipal();
	}
	@Override
	public Blessings localBlessings() {
		return this.params.getLocalBlessings();
	}
	@Override
	public Blessings remoteBlessings() {
		return this.params.getRemoteBlessings();
	}
}
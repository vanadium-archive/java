package io.veyron.veyron.veyron2.naming;

import com.google.gson.annotations.SerializedName;
import org.joda.time.Duration;

/**
 * MountedServer represents a server mounted under an object name.
 */
public class MountedServer {
	@SerializedName("Server")
	private String server;
	@SerializedName("TTL")
	private Duration ttl;

	public MountedServer(String server, Duration ttl) {
		this.server = server;
		this.ttl = ttl;
	}

	/**
	 * Returns the server object address (OA): endpoint + suffix.
	 *
	 * @return a server object address (OA).
	 */
	public String getServer() { return this.server; }

	/**
	 * Returns the Time-To-Live after which the mount expires.
	 *
	 * @return a Time-To-Live after which the mount expires.
	 */
	public Duration getTTL() { return this.ttl; }
}
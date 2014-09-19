package io.veyron.veyron.veyron2.naming;

import com.google.gson.annotations.SerializedName;
import io.veyron.veyron.veyron2.ipc.VeyronException;

/**
 * MountEntry represents a name mounted in the mounttable.
 */
public class MountEntry {
	@SerializedName("Name")
	private String name;
	@SerializedName("Servers")
	private MountedServer[] servers;
	@SerializedName("Error")
	private VeyronException error;

	public MountEntry(String name, MountedServer[] servers, VeyronException error) {
		this.name = name;
		this.servers = servers;
		this.error = error;
	}

	/**
	 * Returns the mounted name.
	 *
	 * @return the mounted name.
	 */
	public String getName() { return this.name; }

	/**
	 * Returns the list of servers mounted under the given name.
	 *
	 * @return the list of servers mounted under the given name.
	 */
	public MountedServer[] getServers() { return this.servers; }

	/**
	 * Returns an error (if any) that occurred fulfilling the request.
	 *
	 * @return an error that occurred fulfilling the request.
	 */
	public VeyronException getError() { return this.error; }
}
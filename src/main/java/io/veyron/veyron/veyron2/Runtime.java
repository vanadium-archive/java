package io.veyron.veyron.veyron2;

import io.veyron.veyron.veyron2.ipc.Client;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.Server;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.naming.Namespace;
import io.veyron.veyron.veyron2.security.Principal;
import io.veyron.veyron.veyron2.security.PrivateID;
import io.veyron.veyron.veyron2.security.PublicIDStore;

/**
 * Runtime represents the local environment allowing clients and servers to communicate
 * with one another.
 */
public interface Runtime {
	/**
	 * Creates a new Client instance.
	 *
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient() throws VeyronException;

	/**
	 * Creates a new Client instance with the provided options.  A particular implementation of this
	 * interface chooses which options to support, but at the minimum it must handle the following
	 * options:
	 * {@link io.veyron.veyron.veyron2.OptionDefs#LOCAL_ID}
	 *
	 * @param  opts            client options
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient(Options opts) throws VeyronException;

	/**
	 * Creates a new Server instance.
	 *
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer() throws VeyronException;

	/**
	 * Creates a new Server instance with the provided options.  A particular implementation of this
	 * interface chooses which options to support, but at the minimum it must handle the following
	 * options:
	 * {@link io.veyron.veyron.veyron2.OptionDefs#LOCAL_ID}
	 *
	 * @param  opts            server options
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer(Options opts) throws VeyronException;

	/**
	 * Returns the pre-configured Client that is created when the Runtime is initialized.
	 *
	 * @return the pre-configured client instance.
	 */
	public Client getClient();

	/**
	 * Returns a new Context instance.
	 *
	 * @return Context the new (client) context.
	 */
	public Context newContext();

	/**
	 * Returns the Principal that represents this Runtime.
	 *
	 * @return the Principal that represents this Runtime.
	 */
	public Principal getPrincipal();

	/**
	 * Creates a new PrivateID with the provided name and a newly minted private key.
	 *
	 * @param  name            name of the PrivateID
	 * @return                 PrivateID with the given name and a newly minted private key.
	 * @throws VeyronException if the PrivateID couldn't be created.
	 */
	public PrivateID newIdentity(String name) throws VeyronException;

	/**
	 * Returns the PrivateID used by the runtime.
	 *
	 * @return PrivateID used by the runtime.
	 */
	public PrivateID getIdentity();

	/**
	 * Returns the store of PublicIDs held by the runtime.
	 *
	 * @return the store of PublicIDs held by the runtime.
	 */
	public PublicIDStore getPublicIDStore();

	/**
	 * Returns the pre-configured Namespace that is created when the Runtime is initialized.
	 *
	 * @return the pre-configured Namespace instance.
	 */
	public Namespace getNamespace();
}
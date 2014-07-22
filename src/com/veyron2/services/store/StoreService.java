// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron2.services.store;

import com.veyron2.ipc.ServerContext;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.store.gen_impl.StoreServiceWrapper;
import com.veyron2.vdl.Stream;
import com.veyron2.vdl.VeyronService;
import java.util.ArrayList;

/**
 * Store is the client interface to the storage system.
 */
@VeyronService(serviceWrapper=StoreServiceWrapper.class)
public interface StoreService { 
	// CreateTransaction creates the transaction and sets the options for it.
	public void createTransaction(ServerContext context, TransactionID tID, ArrayList<java.lang.Object> options) throws VeyronException;
	// Commit commits the changes in the transaction to the store.  The
// operation is atomic, so all mutations are performed, or none.  Returns an
// error if the transaction aborted.
	public void commit(ServerContext context, TransactionID tID) throws VeyronException;
	// Abort discards a transaction.  This is an optimization; transactions
// eventually time out and get discarded.  However, live transactions
// consume resources, so if you know that you won't be using a transaction
// anymore, you should discard it explicitly.
	public void abort(ServerContext context, TransactionID tID) throws VeyronException;
	// ReadConflicts returns the stream of conflicts to store values.  A
// conflict occurs when there is a concurrent modification to a value.
	public void readConflicts(ServerContext context, Stream<Conflict,Void> stream) throws VeyronException;
}

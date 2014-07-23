// This file was auto-generated by the veyron vdl tool.
// Source(s):  bank.vdl
package com.veyron.examples.bank.gen_impl;

import com.veyron.examples.bank.Bank;
import com.veyron.examples.bank.BankAccount;
import com.veyron.examples.bank.BankAccountFactory;
import com.veyron.examples.bank.BankAccountService;
import com.veyron.examples.bank.BankFactory;
import com.veyron.examples.bank.BankService;
import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;

public class BankAccountServiceWrapper {

	private final BankAccountService service;

	public BankAccountServiceWrapper(BankAccountService service) {
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) throws VeyronException { 
		if ("deposit".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(2) };
		}
		if ("withdraw".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(2) };
		}
		if ("transfer".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(2) };
		}
		if ("balance".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(1) };
		}
        if ("getMethodTags".equals(method)) {
            return new Object[]{};
        }
		throw new VeyronException("method: " + method + " not found");
	}
	// Methods from interface BankAccount.
	public void deposit(ServerCall call, long amount) throws VeyronException { 
		this.service.deposit(call, amount);
	}
	public void withdraw(ServerCall call, long amount) throws VeyronException { 
		this.service.withdraw(call, amount);
	}
	public void transfer(ServerCall call, long receiver, long amount) throws VeyronException { 
		this.service.transfer(call, receiver, amount);
	}
	public long balance(ServerCall call) throws VeyronException { 
		return this.service.balance(call);
	}
}

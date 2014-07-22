
// This file was auto-generated by the veyron vdl tool.
// Source: application.vdl
package com.veyron2.services.mgmt.application;

import java.util.ArrayList;

/**
 * Envelope is a collection of metadata that describes an application.
 */
public final class Envelope { 
	// Title is the publisher-assigned application title.  Application
// installations with the same title are considered as belonging to the
// same application by the application management system.
//
// A change in the title signals a new application.
	private String title;
	// Arguments is an array of command-line arguments to be used when
// executing the binary.
	private ArrayList<String> args;
	// Binary is an object name that identifies the application binary.
	private String binary;
	// Environment is a map that stores the environment variable values
// to be used when executing the binary.
	private ArrayList<String> env;

	public Envelope(String title, ArrayList<String> args, String binary, ArrayList<String> env) { 
		this.title = title;
		this.args = args;
		this.binary = binary;
		this.env = env;
	}
	public String getTitle() { return this.title; }
	public ArrayList<String> getArgs() { return this.args; }
	public String getBinary() { return this.binary; }
	public ArrayList<String> getEnv() { return this.env; }

	public void setTitle(String title) { this.title = title; }
	public void setArgs(ArrayList<String> args) { this.args = args; }
	public void setBinary(String binary) { this.binary = binary; }
	public void setEnv(ArrayList<String> env) { this.env = env; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final Envelope other = (Envelope)obj;
		if (!(this.title.equals(other.title))) return false;
		if (!(this.args.equals(other.args))) return false;
		if (!(this.binary.equals(other.binary))) return false;
		if (!(this.env.equals(other.env))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (title == null ? 0 : title.hashCode());
		result = prime * result + (args == null ? 0 : args.hashCode());
		result = prime * result + (binary == null ? 0 : binary.hashCode());
		result = prime * result + (env == null ? 0 : env.hashCode());
		return result;
	}
}
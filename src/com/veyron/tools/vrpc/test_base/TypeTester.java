// This file was auto-generated by the veyron vdl tool.
// Source: test_base.vdl
package com.veyron.tools.vrpc.test_base;

import com.veyron2.Options;
import com.veyron2.ipc.Context;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.ClientStream;
import java.util.ArrayList;
import java.util.HashMap;

public interface TypeTester { 
	// Methods to test support for generic types.
	public boolean echoBool(Context context, boolean i1) throws VeyronException;
	public boolean echoBool(Context context, boolean i1, Options veyronOpts) throws VeyronException;
		public float echoFloat32(Context context, float i1) throws VeyronException;
	public float echoFloat32(Context context, float i1, Options veyronOpts) throws VeyronException;
		public double echoFloat64(Context context, double i1) throws VeyronException;
	public double echoFloat64(Context context, double i1, Options veyronOpts) throws VeyronException;
		public int echoInt32(Context context, int i1) throws VeyronException;
	public int echoInt32(Context context, int i1, Options veyronOpts) throws VeyronException;
		public long echoInt64(Context context, long i1) throws VeyronException;
	public long echoInt64(Context context, long i1, Options veyronOpts) throws VeyronException;
		public String echoString(Context context, String i1) throws VeyronException;
	public String echoString(Context context, String i1, Options veyronOpts) throws VeyronException;
		public byte echoByte(Context context, byte i1) throws VeyronException;
	public byte echoByte(Context context, byte i1, Options veyronOpts) throws VeyronException;
		public int echoUInt32(Context context, int i1) throws VeyronException;
	public int echoUInt32(Context context, int i1, Options veyronOpts) throws VeyronException;
		public long echoUInt64(Context context, long i1) throws VeyronException;
	public long echoUInt64(Context context, long i1, Options veyronOpts) throws VeyronException;
	// Methods to test support for composite types.
	public void inputArray(Context context, byte[] i1) throws VeyronException;
	public void inputArray(Context context, byte[] i1, Options veyronOpts) throws VeyronException;
		public void inputMap(Context context, HashMap<Byte, Byte> i1) throws VeyronException;
	public void inputMap(Context context, HashMap<Byte, Byte> i1, Options veyronOpts) throws VeyronException;
		public void inputSlice(Context context, ArrayList<Byte> i1) throws VeyronException;
	public void inputSlice(Context context, ArrayList<Byte> i1, Options veyronOpts) throws VeyronException;
		public void inputStruct(Context context, Struct i1) throws VeyronException;
	public void inputStruct(Context context, Struct i1, Options veyronOpts) throws VeyronException;
		public byte[] outputArray(Context context) throws VeyronException;
	public byte[] outputArray(Context context, Options veyronOpts) throws VeyronException;
		public HashMap<Byte, Byte> outputMap(Context context) throws VeyronException;
	public HashMap<Byte, Byte> outputMap(Context context, Options veyronOpts) throws VeyronException;
		public ArrayList<Byte> outputSlice(Context context) throws VeyronException;
	public ArrayList<Byte> outputSlice(Context context, Options veyronOpts) throws VeyronException;
		public Struct outputStruct(Context context) throws VeyronException;
	public Struct outputStruct(Context context, Options veyronOpts) throws VeyronException;
	// Methods to test support for different number of arguments.
	public void noArguments(Context context) throws VeyronException;
	public void noArguments(Context context, Options veyronOpts) throws VeyronException;
	// MultipleArgumentsOut packages output arguments for method MultipleArguments.
	public static class MultipleArgumentsOut { 
		public int o1;
		public int o2;
	}
		public TypeTester.MultipleArgumentsOut multipleArguments(Context context, int i1, int i2) throws VeyronException;
	public TypeTester.MultipleArgumentsOut multipleArguments(Context context, int i1, int i2, Options veyronOpts) throws VeyronException;
	// Methods to test support for streaming.
	public ClientStream<Void,Boolean,Void> streamingOutput(Context context, int numStreamItems, boolean streamItem) throws VeyronException;
	public ClientStream<Void,Boolean,Void> streamingOutput(Context context, int numStreamItems, boolean streamItem, Options veyronOpts) throws VeyronException;
}
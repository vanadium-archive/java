// This file was auto-generated by the veyron vdl tool.
// Source: tunnel.vdl
package io.veyron.examples.tunnel;


public interface Tunnel  {

    
    

    
    // The Forward method is used for network forwarding. All the data sent over
// the byte stream is forwarded to the requested network address and all the
// data received from that network connection is sent back in the reply
// stream.

    public com.veyron2.vdl.ClientStream<byte[],byte[], java.lang.Void> forward(final com.veyron2.ipc.Context context, final java.lang.String network, final java.lang.String address) throws com.veyron2.ipc.VeyronException;
    public com.veyron2.vdl.ClientStream<byte[],byte[], java.lang.Void> forward(final com.veyron2.ipc.Context context, final java.lang.String network, final java.lang.String address, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException;

    
    

    
    // The Shell method is used to either run shell commands remotely, or to open
// an interactive shell. The data received over the byte stream is sent to the
// shell's stdin, and the data received from the shell's stdout and stderr is
// sent back in the reply stream. It returns the exit status of the shell
// command.

    public com.veyron2.vdl.ClientStream<io.veyron.examples.tunnel.ClientShellPacket,io.veyron.examples.tunnel.ServerShellPacket, java.lang.Integer> shell(final com.veyron2.ipc.Context context, final java.lang.String command, final io.veyron.examples.tunnel.ShellOpts shellOpts) throws com.veyron2.ipc.VeyronException;
    public com.veyron2.vdl.ClientStream<io.veyron.examples.tunnel.ClientShellPacket,io.veyron.examples.tunnel.ServerShellPacket, java.lang.Integer> shell(final com.veyron2.ipc.Context context, final java.lang.String command, final io.veyron.examples.tunnel.ShellOpts shellOpts, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException;

}
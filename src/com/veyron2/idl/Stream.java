package com.veyron2.idl;

import com.veyron2.ipc.VeyronException;

/**
 * Stream implements a bidirectional stream.
**/
public interface Stream<Input, Output> {
        // TODO(spetrovic): implement this method.
        public void Send(Input item) throws VeyronException;

        // TODO(spetrovic): implement this method.
        public Output Recv() throws VeyronException;
}

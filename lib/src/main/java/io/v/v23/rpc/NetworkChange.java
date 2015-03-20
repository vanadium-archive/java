package io.v.v23.rpc;

import org.joda.time.DateTime;

import io.v.v23.verror.VException;

import java.util.Arrays;

/**
 * NetworkChange represents the changes made in response to a network setting change
 * being received.
 */
public class NetworkChange {
    private final DateTime time;
    private final ServerState state;
    private final String[] changedEndpoints;
    private final String setting;
    private final VException error;

    public NetworkChange(DateTime time,
            ServerState state, String[] changedEndpoints, String setting, VException error) {
        this.time = time;
        this.state = state;
        this.changedEndpoints = changedEndpoints;
        this.setting = setting;
        this.error = error;
    }

    /**
     * Returns the time of the last change.
     *
     * @return time of the last change
     */
    public DateTime getTime() { return this.time; }

    /**
     * Returns the current state of the server.
     *
     * @return current state of the server
     */
    public ServerState getState() { return this.state; }

    /**
     * Returns the set of endpoints added/removed as a result of this change.
     *
     * @return set of endpoints added/removed as a result of this change
     */
    public String[] getChangedEndpoints() { return this.changedEndpoints; }

    /**
     * Returns the setting sent for the last change.
     *
     * @return setting sent for the last change
     */
    public String getSetting() { return this.setting; }

    /**
     * Returns any error encountered.
     *
     * @return any error encountered
     */
    public VException getError() { return this.error; }

    @Override
    public String toString() {
        return String.format("{Time: %s, State: %s, Setting: %s, Changed: %s, Error: %s}",
                this.time, this.state, this.setting, Arrays.toString(this.changedEndpoints),
                this.error);
    }
}
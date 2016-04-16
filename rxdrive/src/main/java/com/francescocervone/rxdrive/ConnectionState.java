package com.francescocervone.rxdrive;

import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class ConnectionState {

    public enum State {
        CONNECTED,
        SUSPENDED,
        FAILED
    }

    public enum ConnectionSuspendedCause {
        NETWORK_LOST,
        SERVICE_DISCONNECTED
    }

    private State mState;

    private Bundle mBundle;
    private ConnectionSuspendedCause mCause;
    private ConnectionResult mConnectionResult;

    private ConnectionState(State state, Bundle bundle, ConnectionSuspendedCause cause, ConnectionResult connectionResult) {
        mState = state;
        mBundle = bundle;
        mCause = cause;
        mConnectionResult = connectionResult;
    }

    public static ConnectionState connected(Bundle bundle) {
        return new Builder()
                .state(State.CONNECTED)
                .bundle(bundle)
                .build();
    }

    public static ConnectionState suspended(int cause) {
        return new Builder()
                .state(State.SUSPENDED)
                .cause(cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST ?
                        ConnectionSuspendedCause.NETWORK_LOST :
                        ConnectionSuspendedCause.SERVICE_DISCONNECTED)
                .build();
    }

    public static ConnectionState failed(ConnectionResult result) {
        return new Builder()
                .state(State.FAILED)
                .result(result)
                .build();
    }

    public boolean isConnected() {
        return mState == State.CONNECTED;
    }

    public boolean isSuspended() {
        return mState == State.SUSPENDED;
    }

    public boolean isFailed() {
        return mState == State.FAILED;
    }

    public Bundle getBundle() {
        return mBundle;
    }

    public ConnectionSuspendedCause getCause() {
        return mCause;
    }

    public ConnectionResult getConnectionResult() {
        return mConnectionResult;
    }

    public State getState() {
        return mState;
    }

    private static class Builder {
        private State mState;
        private Bundle mBundle;
        private ConnectionSuspendedCause mCause;
        private ConnectionResult mConnectionResult;

        public Builder() {

        }

        public Builder state(State state) {
            mState = state;
            return this;
        }

        public Builder bundle(Bundle bundle) {
            mBundle = bundle;
            return this;
        }

        public Builder cause(ConnectionSuspendedCause cause) {
            mCause = cause;
            return this;
        }

        public Builder result(ConnectionResult result) {
            mConnectionResult = result;
            return this;
        }

        public ConnectionState build() {
            return new ConnectionState(mState, mBundle, mCause, mConnectionResult);
        }
    }
}

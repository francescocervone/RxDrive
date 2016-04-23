package com.francescocervone.rxdrive;

import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class ConnectionState {

    public enum State {
        CONNECTED,
        SUSPENDED,
        FAILED,
        UNABLE_TO_RESOLVE
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

    static ConnectionState connected(Bundle bundle) {
        return new Builder()
                .state(State.CONNECTED)
                .bundle(bundle)
                .build();
    }

    static ConnectionState suspended(int cause) {
        return new Builder()
                .state(State.SUSPENDED)
                .cause(cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST ?
                        ConnectionSuspendedCause.NETWORK_LOST :
                        ConnectionSuspendedCause.SERVICE_DISCONNECTED)
                .build();
    }

    static ConnectionState failed(ConnectionResult result) {
        return new Builder()
                .state(State.FAILED)
                .result(result)
                .build();
    }

    static ConnectionState unableToResolve(ConnectionResult result) {
        return new Builder()
                .state(State.UNABLE_TO_RESOLVE)
                .result(result)
                .build();
    }

    /**
     * @return true if GoogleApiClient is connected
     */
    public boolean isConnected() {
        return mState == State.CONNECTED;
    }

    /**
     * @return true if GoogleApiClient connection is suspended
     */
    public boolean isSuspended() {
        return mState == State.SUSPENDED;
    }

    /**
     * @return true if connection to GoogleApiClient is failed
     */
    public boolean isFailed() {
        return mState == State.FAILED;
    }

    /**
     * @return true if startResolutionForResult failed
     * @see com.google.android.gms.common.ConnectionResult#startResolutionForResult
     */
    public boolean isUnableToResolve() {
        return mState == State.UNABLE_TO_RESOLVE;
    }

    /**
     * This method should be called when state is connected
     *
     * @return the Bundle object returned by GoogleApiClient
     */
    public Bundle getBundle() {
        return mBundle;
    }

    /**
     * This method should be called when connection is suspended
     *
     * @return the cause of the suspension of GoogleApiClient connection
     */
    public ConnectionSuspendedCause getCause() {
        return mCause;
    }

    /**
     * This method should be called when connection to GoogleApiClient is failed
     *
     * @return the ConnectionResult returned by GoogleApiClient, useful to try to resolve the problem
     */
    public ConnectionResult getConnectionResult() {
        return mConnectionResult;
    }

    /**
     * @return the current state of connection to GoogleApiClient
     */
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

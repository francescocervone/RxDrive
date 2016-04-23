package com.francescocervone.rxdrive;

import com.google.android.gms.common.api.Status;

public class RxDriveException extends RuntimeException {

    private Status mStatus;

    public RxDriveException(Status status) {

        mStatus = status;
    }

    public Status getStatus() {
        return mStatus;
    }
}

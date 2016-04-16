package com.francescocervone.rxdrive;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class RxDrive {

    private PublishSubject<ConnectionState> mGoogleApiClientPublishSubject = PublishSubject.create();

    private GoogleApiClient mClient;
    private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            mGoogleApiClientPublishSubject.onNext(ConnectionState.connected(bundle));
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mGoogleApiClientPublishSubject.onNext(ConnectionState.suspended(cause));
        }
    };

    private GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            mGoogleApiClientPublishSubject.onNext(ConnectionState.failed(connectionResult));
        }
    };

    /**
     * @param builder is a GoogleApiClient builder for your application
     */
    public RxDrive(GoogleApiClient.Builder builder) {
        mClient = builder.addApi(Drive.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();
    }

    /**
     * @param context is necessary to create a new GoogleApiClient
     */
    public RxDrive(Context context) {
        this(new GoogleApiClient.Builder(context)
                .addScope(Drive.SCOPE_APPFOLDER));
    }


    /**
     * Creates an Observable that emits the connection state changes the GoogleApiClient
     *
     * @return the Observable for connection state changes
     */
    public Observable<ConnectionState> connection() {
        return mGoogleApiClientPublishSubject.asObservable();
    }

    /**
     * Establishes a connection with the GoogleApiClient created before
     */
    public void connect() {
        mClient.connect();
    }

    /**
     * Disconnects from GoogleApiClient
     */
    public void disconnect() {
        mClient.disconnect();
    }

    /**
     * Check if the GoogleApiClient is connected
     *
     * @return true if GoogleApiClient is connected, false otherwise
     */
    public boolean isConnected() {
        return mClient.isConnected();
    }

    /**
     * Lists the files in the default folder
     *
     * @return an Observable with the list of the files
     */
    public Observable<List<DriveFile>> listFiles() {
        return Observable.defer(new Func0<Observable<List<DriveFile>>>() {
            @Override
            public Observable<List<DriveFile>> call() {
                List<DriveFile> list = new LinkedList<>();

                DriveApi.MetadataBufferResult result = Drive.DriveApi.getAppFolder(mClient)
                        .listChildren(mClient)
                        .await();

                if (result.getStatus().isSuccess()) {
                    MetadataBuffer metadataBuffer = result.getMetadataBuffer();

                    for (Metadata m : metadataBuffer) {
                        list.add(m.getDriveId().asDriveFile());
                    }

                    metadataBuffer.release();
                    return Observable.just(list);
                } else {
                    return Observable.error(new RuntimeException(result.getStatus().getStatusMessage()));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Creates a file on Drive
     *
     * @param file is the file that will be uploaded
     * @return an Observable with the new DriveFile
     */
    public Observable<DriveFile> createFile(final File file) {
        return createFile(Uri.fromFile(file));
    }

    /**
     * Creates a file on Drive
     *
     * @param uri is the Uri of a file that will be uploaded
     * @return an Observable with the new DriveFile
     */
    public Observable<DriveFile> createFile(final Uri uri) {
        return Observable.defer(new Func0<Observable<DriveFile>>() {
            @Override
            public Observable<DriveFile> call() {
                try {
                    DriveContents driveContents = Drive.DriveApi.newDriveContents(mClient)
                            .await()
                            .getDriveContents();

                    ContentResolver contentResolver = mClient.getContext().getContentResolver();
                    IOUtils.copy(
                            contentResolver.openInputStream(uri),
                            driveContents.getOutputStream());

                    DriveFolder.DriveFileResult result = Drive.DriveApi.getAppFolder(mClient)
                            .createFile(
                                    mClient,
                                    new MetadataChangeSet.Builder()
                                            .setMimeType(contentResolver
                                                    .getType(uri))
                                            .setTitle(uri.getLastPathSegment())
                                            .build(),
                                    driveContents)
                            .await();

                    if (result.getStatus().isSuccess()) {
                        return Observable.just(result.getDriveFile());
                    } else {
                        return Observable.error(new RuntimeException(result.getStatus().getStatusMessage()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return Observable.error(e);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Removes a file from Drive
     *
     * @param driveFile the file that will be removed from Drive
     * @return an Observable with `true` if the file is removed
     */
    public Observable<Boolean> removeFile(final DriveFile driveFile) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                Status status = driveFile.delete(mClient).await();
                if (status.isSuccess()) {
                    return Observable.just(true);
                } else {
                    return Observable.error(new RuntimeException(status.getStatusMessage()));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns the Metadata of a driveFile
     *
     * @param driveFile the file you want the Metadata
     * @return the Metadata of the driveFile
     */
    public Observable<Metadata> metadata(final DriveFile driveFile) {
        return Observable.defer(new Func0<Observable<Metadata>>() {
            @Override
            public Observable<Metadata> call() {
                DriveResource.MetadataResult result = driveFile.getMetadata(mClient).await();
                if (result.getStatus().isSuccess()) {
                    return Observable.just(result.getMetadata());
                } else {
                    return Observable.error(new RuntimeException(result.getStatus().getStatusMessage()));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}

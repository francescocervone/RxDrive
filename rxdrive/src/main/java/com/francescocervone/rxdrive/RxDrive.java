package com.francescocervone.rxdrive;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Query;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class RxDrive {

    private PublishSubject<ConnectionState> mConnectionStatePublishSubject = PublishSubject.create();

    private GoogleApiClient mClient;
    private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            mConnectionStatePublishSubject.onNext(ConnectionState.connected(bundle));
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mConnectionStatePublishSubject.onNext(ConnectionState.suspended(cause));
        }
    };

    private GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            mConnectionStatePublishSubject.onNext(ConnectionState.failed(connectionResult));
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
        return mConnectionStatePublishSubject.asObservable();
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
     * Lists resources matching a query
     *
     * @param query Drive query
     * @return an Observable with the list of the resources
     */
    public Observable<List<DriveId>> list(final Query query) {
        return Observable.defer(new Func0<Observable<List<DriveId>>>() {
            @Override
            public Observable<List<DriveId>> call() {
                List<DriveId> list = new ArrayList<>();
                DriveApi.MetadataBufferResult result = Drive.DriveApi.getAppFolder(mClient)
                        .queryChildren(mClient, query)
                        .await();

                if (result.getStatus().isSuccess()) {
                    MetadataBuffer buffer = result.getMetadataBuffer();

                    for (Metadata metadata : buffer) {
                        list.add(metadata.getDriveId());
                    }

                    buffer.release();
                    return Observable.just(list);
                } else {
                    return Observable.error(new RuntimeException(result.getStatus().getStatusMessage()));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * Lists resources in default folder
     *
     * @return an Observable with the list of the resources
     */
    public Observable<List<DriveId>> list() {
        return Observable.defer(new Func0<Observable<List<DriveId>>>() {
            @Override
            public Observable<List<DriveId>> call() {
                List<DriveId> list = new ArrayList<>();

                DriveApi.MetadataBufferResult result = Drive.DriveApi.getAppFolder(mClient)
                        .listChildren(mClient)
                        .await();

                if (result.getStatus().isSuccess()) {
                    MetadataBuffer buffer = result.getMetadataBuffer();

                    for (Metadata m : buffer) {
                        list.add(m.getDriveId());
                    }

                    buffer.release();
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
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(final File file) {
        return createFile(file, file.getName());
    }

    /**
     * Creates a file on Drive
     *
     * @param file  is the file that will be uploaded
     * @param title is the title that you want for the new file
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(File file, String title) {
        return createFile(file, title, MimeTypeMap.getFileExtensionFromUrl(file.getPath()));
    }

    /**
     * Creates a file on Drive
     *
     * @param file     is the file that will be uploaded
     * @param title    is the title that you want for the new file
     * @param mimeType is the mimeType of the file
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(File file, String title, String mimeType) {
        return createFile(Uri.fromFile(file), title, mimeType);
    }

    /**
     * Creates a file on Drive
     *
     * @param uri is the Uri of a file that will be uploaded
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(final Uri uri) {
        return createFile(uri, uri.getLastPathSegment());
    }

    /**
     * Creates a file on Drive
     *
     * @param uri   is the Uri of a file that will be uploaded
     * @param title is the title that you want for the new file
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(final Uri uri, String title) {
        return createFile(uri, title, getContentResolver().getType(uri));
    }

    /**
     * Creates a file on Drive
     *
     * @param uri      is the Uri of a file that will be uploaded
     * @param title    is the title that you want for the new file
     * @param mimeType is the mimeType of the file
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(final Uri uri, String title, String mimeType) {
        try {
            return createFile(getContentResolver()
                            .openInputStream(uri),
                    title,
                    mimeType);
        } catch (FileNotFoundException e) {
            return Observable.error(e);
        }
    }


    /**
     * Creates a file on Drive
     *
     * @param inputStream is the InputStream that will be uploaded
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(final InputStream inputStream) {
        return createFile(inputStream, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Creates a file on Drive
     *
     * @param inputStream is the InputStream that will be uploaded
     * @param title       is the title that you want for the new file
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(final InputStream inputStream, String title) {
        return createFile(inputStream, title, null);
    }

    /**
     * Creates a file on Drive
     *
     * @param inputStream is the InputStream that will be uploaded
     * @param title       is the title that you want for the new file
     * @param mimeType    is the mimeType of the file
     * @return an Observable with the new DriveId
     */
    public Observable<DriveId> createFile(
            final InputStream inputStream,
            final String title,
            final String mimeType) {

        return Observable.defer(new Func0<Observable<DriveId>>() {
            @Override
            public Observable<DriveId> call() {
                try {
                    DriveContents driveContents = Drive.DriveApi.newDriveContents(mClient)
                            .await()
                            .getDriveContents();

                    IOUtils.copy(
                            inputStream,
                            driveContents.getOutputStream());

                    DriveFolder.DriveFileResult result = Drive.DriveApi.getAppFolder(mClient)
                            .createFile(
                                    mClient,
                                    new MetadataChangeSet.Builder()
                                            .setTitle(title)
                                            .setMimeType(mimeType)
                                            .build(),
                                    driveContents)
                            .await();

                    if (result.getStatus().isSuccess()) {
                        return Observable.just(result.getDriveFile().getDriveId());
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
     * Removes a resource from Drive
     *
     * @param driveResource the resource that will be removed from Drive
     * @return an Observable with `true` if the resource is removed
     */
    public Observable<Boolean> remove(final DriveResource driveResource) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                Status status = driveResource.delete(mClient).await();
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
     * Returns the Metadata of a DriveResource
     *
     * @param driveResource the resource you want the Metadata
     * @return the Metadata of the driveResource
     */
    public Observable<Metadata> metadata(final DriveResource driveResource) {
        return Observable.defer(new Func0<Observable<Metadata>>() {
            @Override
            public Observable<Metadata> call() {
                DriveResource.MetadataResult result = driveResource.getMetadata(mClient).await();
                if (result.getStatus().isSuccess()) {
                    return Observable.just(result.getMetadata());
                } else {
                    return Observable.error(new RuntimeException(result.getStatus().getStatusMessage()));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    public Observable<InputStream> open(DriveId driveId) {
        return open(driveId, null);
    }

    public Observable<InputStream> open(final DriveId driveId,
                                        final Subscriber<Progress> progressSubscriber) {
        return Observable.defer(new Func0<Observable<InputStream>>() {
            @Override
            public Observable<InputStream> call() {
                DriveApi.DriveContentsResult result = driveId.asDriveFile().open(
                        mClient,
                        DriveFile.MODE_READ_ONLY,
                        new DriveFile.DownloadProgressListener() {
                            @Override
                            public void onProgress(long bytesDownloaded, long bytesExpected) {
                                if (progressSubscriber != null) {
                                    Log.d("maccio", "onProgress: " + bytesDownloaded + " " + bytesExpected);
                                    progressSubscriber.onNext(
                                            new Progress(bytesDownloaded, bytesExpected));
                                }
                            }
                        })
                        .await();
                if (result.getStatus().isSuccess()) {
                    if (progressSubscriber != null) {
                        progressSubscriber.onCompleted();
                    }
                    return Observable.just(result.getDriveContents().getInputStream());
                } else {
                    return Observable.error(new RuntimeException(result.getStatus().getStatusMessage()));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private ContentResolver getContentResolver() {
        return getContext()
                .getContentResolver();
    }

    private Context getContext() {
        return mClient.getContext();
    }
}

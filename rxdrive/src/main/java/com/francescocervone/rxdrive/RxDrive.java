package com.francescocervone.rxdrive;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subjects.PublishSubject;

public class RxDrive {

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    private static final int NO_RESOLUTION_REQUEST_CODE = 0;

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
     * This constructor will create an instance of GoogleApiClient with user files permissions.
     * If you want to create an instance with app folder permissions or with other Play Services
     * APIs, then you should use {@link RxDrive#RxDrive(GoogleApiClient.Builder)}
     *
     * @param context is necessary to create a new GoogleApiClient
     * @see RxDrive#RxDrive(GoogleApiClient.Builder)
     * @see com.google.android.gms.drive.Drive#SCOPE_FILE
     * @see com.google.android.gms.drive.Drive#SCOPE_APPFOLDER
     */
    public RxDrive(Context context) {
        this(new GoogleApiClient.Builder(context).addScope(Drive.SCOPE_FILE));
    }


    /**
     * Creates an Observable that emits the connection state changes the GoogleApiClient
     *
     * @return the Observable for connection state changes
     */
    public Observable<ConnectionState> connectionObservable() {
        return mConnectionStatePublishSubject;
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
     * @return the root folder of Google Drive
     */
    public DriveFolder getRootFolder() {
        return Drive.DriveApi.getRootFolder(mClient);
    }

    /**
     * @return the app folder on Google Drive
     */
    public DriveFolder getAppFolder() {
        return Drive.DriveApi.getAppFolder(mClient);
    }

    /**
     * Fetches a driveId
     *
     * @param s the string of the driveId
     * @return an Observable with the driveId if exists
     */
    public Single<DriveId> fetchDriveId(final String s) {
        return Single.fromCallable(new Callable<DriveId>() {
            @Override
            public DriveId call() throws Exception {
                DriveApi.DriveIdResult driveIdResult = Drive.DriveApi.fetchDriveId(mClient, s)
                        .await();
                if (driveIdResult.getStatus().isSuccess()) {
                    return driveIdResult.getDriveId();
                } else {
                    throw new RxDriveException(driveIdResult.getStatus());
                }
            }
        });
    }

    /**
     * Lists resources in default folder
     *
     * @return an Observable with the list of the resources
     */
    public Single<List<DriveId>> listChildren(final DriveFolder driveFolder) {
        return Single.fromCallable(new Callable<List<DriveId>>() {
            @Override
            public List<DriveId> call() throws Exception {
                List<DriveId> list = new ArrayList<>();

                DriveApi.MetadataBufferResult result = driveFolder.listChildren(mClient).await();

                if (result.getStatus().isSuccess()) {
                    MetadataBuffer buffer = result.getMetadataBuffer();

                    for (Metadata m : buffer) {
                        list.add(m.getDriveId());
                    }

                    buffer.release();
                    return list;
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });
    }


    /**
     * Lists the parents of a Drive resource
     *
     * @param driveResource
     * @return the list of the parents
     */
    public Single<List<DriveId>> listParents(final DriveResource driveResource) {
        return Single.fromCallable(new Callable<List<DriveId>>() {
            @Override
            public List<DriveId> call() throws Exception {
                List<DriveId> list = new ArrayList<>();

                DriveApi.MetadataBufferResult result = driveResource.listParents(mClient).await();

                if (result.getStatus().isSuccess()) {
                    MetadataBuffer buffer = result.getMetadataBuffer();

                    for (Metadata m : buffer) {
                        list.add(m.getDriveId());
                    }

                    buffer.release();
                    return list;
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });
    }


    /**
     * Sets the parents of a resource
     *
     * @param driveResource the resource where to set the parents
     * @param parents       a set of drive id that will be the parents of the resource
     * @return true if the operation succeeds
     */
    public Completable setParents(final DriveResource driveResource, final Set<DriveId> parents) {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                Status status = driveResource.setParents(mClient, parents).await();
                if (!status.isSuccess()) {
                    throw new RxDriveException(status);
                }
            }
        });
    }

    /**
     * Executes a Query on Google Drive in the default folder
     *
     * @param query the query you want to submit
     * @return
     */
    public Single<List<DriveId>> query(final Query query) {
        return Single.fromCallable(new Callable<List<DriveId>>() {
            @Override
            public List<DriveId> call() throws Exception {
                List<DriveId> list = new ArrayList<>();

                DriveApi.MetadataBufferResult result = Drive.DriveApi
                        .query(mClient, query)
                        .await();

                if (result.getStatus().isSuccess()) {
                    MetadataBuffer buffer = result.getMetadataBuffer();

                    for (Metadata metadata : buffer) {
                        list.add(metadata.getDriveId());
                    }

                    buffer.release();
                    return list;
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });
    }

    /**
     * Lists resources matching a query
     *
     * @param query Drive query
     * @return an Observable with the list of the resources
     */
    public Single<List<DriveId>> queryChildren(final DriveFolder driveFolder, final Query query) {
        return Single.fromCallable(new Callable<List<DriveId>>() {
            @Override
            public List<DriveId> call() throws Exception {
                List<DriveId> list = new ArrayList<>();
                DriveApi.MetadataBufferResult result = driveFolder
                        .queryChildren(mClient, query)
                        .await();

                if (result.getStatus().isSuccess()) {
                    MetadataBuffer buffer = result.getMetadataBuffer();

                    for (Metadata metadata : buffer) {
                        list.add(metadata.getDriveId());
                    }

                    buffer.release();
                    return list;
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });
    }

    /**
     * Creates a file on Drive
     *
     * @param folder the folder where to create the new file
     * @param file   is the file that will be uploaded
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, final File file) {
        return createFile(folder, file, file.getName());
    }

    /**
     * Creates a file on Drive
     *
     * @param folder the folder where to create the new file
     * @param file   is the file that will be uploaded
     * @param title  is the title that you want for the new file
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, File file, String title) {
        return createFile(folder, file, title, MimeTypeMap.getFileExtensionFromUrl(file.getPath()));
    }

    /**
     * Creates a file on Drive
     *
     * @param folder   the folder where to create the new file
     * @param file     is the file that will be uploaded
     * @param title    is the title that you want for the new file
     * @param mimeType is the mimeType of the file
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, File file, String title, String mimeType) {
        return createFile(folder, Uri.fromFile(file), title, mimeType);
    }

    /**
     * Creates a file on Drive
     *
     * @param folder the folder where to create the new file
     * @param uri    is the Uri of a file that will be uploaded
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, final Uri uri) {
        return createFile(folder, uri, uri.getLastPathSegment());
    }

    /**
     * Creates a file on Drive
     *
     * @param folder the folder where to create the new file
     * @param uri    is the Uri of a file that will be uploaded
     * @param title  is the title that you want for the new file
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, final Uri uri, String title) {
        return createFile(folder, uri, title, getContentResolver().getType(uri));
    }

    /**
     * Creates a file on Drive
     *
     * @param folder   the folder where to create the new file
     * @param uri      is the Uri of a file that will be uploaded
     * @param title    is the title that you want for the new file
     * @param mimeType is the mimeType of the file
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, final Uri uri, String title, String mimeType) {
        try {
            return createFile(
                    folder,
                    getContentResolver()
                            .openInputStream(uri),
                    title,
                    mimeType);
        } catch (FileNotFoundException e) {
            return Single.error(e);
        }
    }


    /**
     * Creates a file on Drive
     *
     * @param folder      the folder where to create the new file
     * @param inputStream is the InputStream that will be uploaded
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, final InputStream inputStream) {
        return createFile(folder, inputStream, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Creates a file on Drive
     *
     * @param folder      the folder where to create the new file
     * @param inputStream is the InputStream that will be uploaded
     * @param title       is the title that you want for the new file
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(DriveFolder folder, final InputStream inputStream, String title) {
        return createFile(folder, inputStream, title, null);
    }

    /**
     * Creates a file on Drive
     *
     * @param folder      the folder where to create the new file
     * @param inputStream is the InputStream that will be uploaded
     * @param title       is the title that you want for the new file
     * @param mimeType    is the mimeType of the file
     * @return an Observable with the new DriveId
     */
    public Single<DriveId> createFile(
            final DriveFolder folder,
            final InputStream inputStream,
            final String title,
            final String mimeType) {

        return Single.fromCallable(new Callable<DriveId>() {
            @Override
            public DriveId call() throws Exception {
                DriveContents driveContents = Drive.DriveApi.newDriveContents(mClient)
                        .await()
                        .getDriveContents();

                IOUtils.copy(
                        inputStream,
                        driveContents.getOutputStream());

                DriveFolder.DriveFileResult result = folder
                        .createFile(
                                mClient,
                                new MetadataChangeSet.Builder()
                                        .setTitle(title)
                                        .setMimeType(mimeType)
                                        .build(),
                                driveContents)
                        .await();

                if (result.getStatus().isSuccess()) {
                    return result.getDriveFile().getDriveId();
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });
    }

    /**
     * Updates a file on Drive
     *
     * @param driveFile drive file
     * @param file      the content to write
     * @return an Observable with the new DriveId
     */
    public Single<DriveFile> updateFileContent(final DriveFile driveFile, File file) {
        return updateFileContent(driveFile, Uri.fromFile(file));
    }

    /**
     * Updates a file on Drive
     *
     * @param driveFile drive file
     * @param uri       the content to write
     * @return an Observable with the new DriveId
     */
    public Single<DriveFile> updateFileContent(final DriveFile driveFile, Uri uri) {
        try {
            return updateFileContent(driveFile, getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            return Single.error(e);
        }
    }

    /**
     * Updates a file on Drive
     *
     * @param driveFile drive file
     * @param content   the content to write
     * @return an Observable with the DriveId
     */
    public Single<DriveFile> updateFileContent(final DriveFile driveFile, final InputStream content) {
        return Single.fromCallable(new Callable<DriveFile>() {
            @Override
            public DriveFile call() throws Exception {
                DriveApi.DriveContentsResult driveContentsResult = driveFile
                        .open(mClient, DriveFile.MODE_WRITE_ONLY, null)
                        .await();
                DriveContents driveContents = driveContentsResult.getDriveContents();
                IOUtils.copy(content, driveContents.getOutputStream());
                Status status = driveContents.commit(mClient, null).await();
                if (status.isSuccess()) {
                    return driveFile;
                } else {
                    throw new RxDriveException(status);
                }
            }
        });
    }

    /**
     * Creates a new folder
     *
     * @param folder where to create the new folder
     * @param title  the title of the new folder
     * @return an observable with the new DriveFolder object
     */
    public Single<DriveFolder> createFolder(final DriveFolder folder, final String title) {
        return Single.fromCallable(new Callable<DriveFolder>() {
            @Override
            public DriveFolder call() throws Exception {
                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                        .setTitle(title)
                        .build();
                DriveFolder.DriveFolderResult result = folder.createFolder(
                        mClient,
                        metadataChangeSet)
                        .await();
                if (result.getStatus().isSuccess()) {
                    return result.getDriveFolder();
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });

    }

    /**
     * Removes a resource from Drive
     *
     * @param driveResource the resource that will be removed from Drive
     * @return an Observable with `true` if the resource is removed
     */
    public Completable delete(final DriveResource driveResource) {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                Status status = driveResource.delete(mClient).await();
                if (!status.isSuccess()) {
                    throw new RxDriveException(status);
                }
            }
        });
    }

    /**
     * Trashes a resource
     *
     * @param driveResource the resource to put in the trash
     * @return true if the operation succeeds
     */
    public Completable trash(final DriveResource driveResource) {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                Status status = driveResource.trash(mClient).await();
                if (!status.isSuccess()) {
                    throw new RxDriveException(status);
                }
            }
        });
    }

    /**
     * Untrashes a resource
     *
     * @param driveResource the resource to remove from the trash
     * @return true if the operation succeeds
     */
    public Completable untrash(final DriveResource driveResource) {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                Status status = driveResource.untrash(mClient).await();
                if (!status.isSuccess()) {
                    throw new RxDriveException(status);
                }
            }
        });
    }

    /**
     * Do sync
     *
     * @return nothing
     */
    public Completable sync() {
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                Drive.DriveApi.requestSync(mClient).await();
            }
        });
    }

    /**
     * Returns the Metadata of a DriveResource
     *
     * @param driveResource the resource you want the Metadata
     * @return the Metadata of the driveResource
     */
    public Single<Metadata> getMetadata(final DriveResource driveResource) {
        return Single.fromCallable(new Callable<Metadata>() {
            @Override
            public Metadata call() throws Exception {
                DriveResource.MetadataResult result = driveResource.getMetadata(mClient).await();
                if (result.getStatus().isSuccess()) {
                    return result.getMetadata();
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });
    }


    /**
     * Open a driveId
     *
     * @param driveId the file to open
     * @return the InputStream of the content
     */
    public Single<InputStream> open(DriveId driveId) {
        return open(driveId, null);
    }

    /**
     * Open a driveId
     *
     * @param driveId            the file to open
     * @param progressSubscriber the subscriber that listen for download progress
     * @return the InputStream of the content
     */
    public Single<InputStream> open(final DriveId driveId,
                                    final Subscriber<Progress> progressSubscriber) {
        return Single.fromCallable(new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
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
                    return result.getDriveContents().getInputStream();
                } else {
                    throw new RxDriveException(result.getStatus());
                }
            }
        });
    }

    /**
     * Tries to resolve GoogleApiClient connection failed
     *
     * @param activity the current activity
     * @param result   the connection result of GoogleApiClient
     */
    public void resolveConnection(Activity activity, ConnectionResult result) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(activity, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                mConnectionStatePublishSubject.onNext(ConnectionState.unableToResolve(result));
            }
        } else {
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(activity, result.getErrorCode(), NO_RESOLUTION_REQUEST_CODE)
                    .show();
        }
    }

    /**
     * You should call this method in your onActivityResult if you tried to resolve connection
     * with RxDrive using method resolveConnection.
     *
     * @param requestCode request code of onActivityResult
     * @param resultCode  result code of onActivityResult
     * @param data        intent of onActivityResult
     * @see #resolveConnection(Activity, ConnectionResult)
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
            case NO_RESOLUTION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    connect();
                }
                break;
        }
    }

    private ContentResolver getContentResolver() {
        return getContext()
                .getContentResolver();
    }

    private Context getContext() {
        return mClient.getContext();
    }
}

package com.francescocervone.rxdrive.sample;

import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.francescocervone.rxdrive.ConnectionState;
import com.francescocervone.rxdrive.RxDrive;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.drive.DriveFile;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    private static final int PICK_IMAGE_CODE = 2;
    public static final int NO_RESOLUTION_REQUEST_CODE = 0;
    private RecyclerView mRecyclerView;
    private Button mAddPhoto;

    private CompositeSubscription mSubscriptions = new CompositeSubscription();
    private DriveFileAdapter mAdapter;
    private RxDrive mRxDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mAddPhoto = (Button) findViewById(R.id.add_photo);

        mAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_CODE);
            }
        });

        mRxDrive = new RxDrive(this);

        mAdapter = new DriveFileAdapter(mRxDrive);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupGoogleApiClientObservable();
        mRxDrive.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRxDrive.disconnect();
        mSubscriptions.unsubscribe();
    }

    private void setupGoogleApiClientObservable() {
        Subscription subscription = mRxDrive.connection()
                .subscribe(new Action1<ConnectionState>() {
                    @Override
                    public void call(ConnectionState connectionState) {
                        switch (connectionState.getState()) {
                            case CONNECTED:
                                mAddPhoto.setEnabled(true);
                                listFiles();
                                break;
                            case SUSPENDED:
                                mAddPhoto.setEnabled(false);
                                log(connectionState.getCause().name());
                                break;
                            case FAILED:
                                mAddPhoto.setEnabled(false);
                                log(connectionState.getConnectionResult().getErrorMessage());
                                resolve(connectionState.getConnectionResult());
                                break;
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        log(throwable);
                    }
                });
        mSubscriptions.add(subscription);
    }

    private void resolve(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), NO_RESOLUTION_REQUEST_CODE).show();
        }
    }

    private void listFiles() {
        mRxDrive.listFiles()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<DriveFile>>() {
                    @Override
                    public void call(List<DriveFile> driveFiles) {
                        mAdapter.setFiles(driveFiles);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        log(throwable);
                    }
                });
    }

    private Subscription createFile(Uri uri) {
        return mRxDrive.createFile(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DriveFile>() {
                    @Override
                    public void call(DriveFile driveFile) {
                        mAdapter.addFile(driveFile);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        log(throwable);
                    }
                });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mRxDrive.connect();
                }
                break;
            case PICK_IMAGE_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    createFile(uri);
                }
        }
    }

    private void log(Object object) {
        Log.d(TAG, "log: " + object);
        Toast.makeText(MainActivity.this, object.toString(), Toast.LENGTH_SHORT).show();
        if (object instanceof Throwable) {
            ((Throwable) object).printStackTrace();
        }
    }
}

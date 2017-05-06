package com.francescocervone.rxdrive.sample;

import android.content.Intent;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;

import java.util.List;

import rx.Observable;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity implements DriveFileAdapter.OnDriveIdClickListener {

    private static final String TAG = MainActivity.class.getName();
    private static final int PICK_IMAGE_CODE = 2;
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

        mRxDrive = new RxDrive(new GoogleApiClient.Builder(this)
                .addScope(Drive.SCOPE_APPFOLDER));

        mAdapter = new DriveFileAdapter(mRxDrive);
        mAdapter.setDriveIdClickListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupGoogleApiClientObservable();
        mRxDrive.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRxDrive.disconnect();
        mSubscriptions.unsubscribe();
    }

    private void setupGoogleApiClientObservable() {
        Subscription subscription = mRxDrive.connectionObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ConnectionState>() {
                    @Override
                    public void call(ConnectionState connectionState) {
                        switch (connectionState.getState()) {
                            case CONNECTED:
                                mAddPhoto.setEnabled(true);
                                list();
                                break;
                            case SUSPENDED:
                                mAddPhoto.setEnabled(false);
                                log(connectionState.getCause().name());
                                break;
                            case FAILED:
                                mAddPhoto.setEnabled(false);
                                log(connectionState.getConnectionResult().getErrorMessage());
                                mRxDrive.resolveConnection(MainActivity.this, connectionState.getConnectionResult());
                                break;
                            case UNABLE_TO_RESOLVE:
                                log("Unable to resolve");
                                finish();
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

    private void list() {
        mRxDrive.listChildren(mRxDrive.getAppFolder())
                .subscribeOn(Schedulers.io())
                .flatMapObservable(new Func1<List<DriveId>, Observable<DriveId>>() {
                    @Override
                    public Observable<DriveId> call(List<DriveId> driveIds) {
                        return Observable.from(driveIds);
                    }
                })
                .flatMap(new Func1<DriveId, Observable<Metadata>>() {
                    @Override
                    public Observable<Metadata> call(DriveId driveId) {
                        return mRxDrive.getMetadata(driveId.asDriveResource()).toObservable();
                    }
                })
                .toList()
                .toSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Metadata>>() {
                    @Override
                    public void call(List<Metadata> driveFiles) {
                        mAdapter.setResources(driveFiles);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        log(throwable);
                    }
                });
    }

    private Subscription createFile(Uri uri) {
        return mRxDrive.createFile(mRxDrive.getAppFolder(), uri)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<DriveId, Single<Metadata>>() {
                    @Override
                    public Single<Metadata> call(DriveId driveId) {
                        return mRxDrive.getMetadata(driveId.asDriveResource());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Metadata>() {
                    @Override
                    public void call(Metadata metadata) {
                        mAdapter.addResource(metadata);
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
        mRxDrive.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_IMAGE_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    createFile(uri);
                }
        }
    }

    private void log(Object object) {
        if (object != null) {
            Log.d(TAG, "log: " + object);
            Toast.makeText(MainActivity.this, object.toString(), Toast.LENGTH_SHORT).show();
            if (object instanceof Throwable) {
                ((Throwable) object).printStackTrace();
            }
        }
    }

    @Override
    public void onDriveIdClick(DriveId driveId) {
        Intent intent = new Intent(this, ImageActivity.class);
        intent.putExtra(ImageActivity.IMAGE_EXTRA, driveId);
        startActivity(intent);
    }
}

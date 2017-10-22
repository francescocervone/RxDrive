package com.francescocervone.rxdrive.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.francescocervone.rxdrive.RxDrive;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements DriveFileAdapter.OnDriveIdClickListener {

    private static final String TAG = MainActivity.class.getName();
    private static final int PICK_IMAGE_CODE = 2;
    private RecyclerView mRecyclerView;
    private Button mAddPhoto;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private DriveFileAdapter mAdapter;
    private RxDrive mRxDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recycler_view);
        mAddPhoto = findViewById(R.id.add_photo);

        mAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_CODE);
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
        mCompositeDisposable.clear();
    }

    private void setupGoogleApiClientObservable() {
        Disposable disposable = mRxDrive.connectionObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectionState -> {
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
                }, this::log);
        mCompositeDisposable.add(disposable);
    }

    private void list() {
        mRxDrive.listChildren(mRxDrive.getAppFolder())
                .subscribeOn(Schedulers.io())
                .flatMapObservable(Observable::fromIterable)
                .flatMapSingle(driveId -> mRxDrive.getMetadata(driveId.asDriveResource()))
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(driveFiles -> mAdapter.setResources(driveFiles), this::log);
    }

    private void createFile(Uri uri) {
        mCompositeDisposable.add(
                mRxDrive.createFile(mRxDrive.getAppFolder(), uri)
                        .subscribeOn(Schedulers.io())
                        .flatMap(driveId -> mRxDrive.getMetadata(driveId.asDriveResource()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(metadata -> mAdapter.addResource(metadata), this::log));
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

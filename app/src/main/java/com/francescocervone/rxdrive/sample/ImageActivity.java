package com.francescocervone.rxdrive.sample;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.francescocervone.rxdrive.ConnectionState;
import com.francescocervone.rxdrive.Progress;
import com.francescocervone.rxdrive.RxDrive;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageActivity extends AppCompatActivity {

    public static final String IMAGE_EXTRA = "image";
    private TextView mTextView;
    private ImageView mImageView;
    private PhotoViewAttacher mAttacher;
    private RxDrive mRxDrive;
    private CompositeDisposable mSubscriptions = new CompositeDisposable();
    private DriveId mDriveId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        mDriveId = getIntent().getParcelableExtra(IMAGE_EXTRA);
        if (mDriveId == null) {
            finish();
            return;
        }

        mTextView = findViewById(R.id.percentage);
        mImageView = findViewById(R.id.image);
        mAttacher = new PhotoViewAttacher(mImageView, true);

        mRxDrive = new RxDrive(new GoogleApiClient.Builder(this).addScope(Drive.SCOPE_APPFOLDER));
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupConnection();
        mRxDrive.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRxDrive.disconnect();
        mSubscriptions.clear();
    }

    private void setupConnection() {
        Disposable disposable = mRxDrive.connectionObservable()
                .observeOn(Schedulers.io())
                .filter(ConnectionState::isConnected)
                .flatMapSingle(connectionState -> mRxDrive.open(mDriveId, getProgressObserver()))
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(Observable.empty())
                .subscribe(this::loadImage);
        mSubscriptions.add(disposable);
    }

    private void loadImage(InputStream inputStream) {
        try {
            Glide.with(this)
                    .load(IOUtils.toByteArray(inputStream))
                    .apply(new RequestOptions()
                            .fitCenter())
                    .listener(getRequestListener())
                    .into(mImageView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private RequestListener<Drawable> getRequestListener() {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                mAttacher.update();
                return false;
            }
        };
    }

    @NonNull
    private Observer<Progress> getProgressObserver() {
        return new Observer<Progress>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Progress progress) {
                mTextView.setText(progress.getPercentage() + "%");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
    }
}

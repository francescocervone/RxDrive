package com.francescocervone.rxdrive.sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
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

import rx.Single;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageActivity extends AppCompatActivity {

    public static final String IMAGE_EXTRA = "image";
    private TextView mTextView;
    private ImageView mImageView;
    private PhotoViewAttacher mAttacher;
    private RxDrive mRxDrive;
    private CompositeSubscription mSubscriptions = new CompositeSubscription();
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

        mTextView = (TextView) findViewById(R.id.percentage);
        mImageView = (ImageView) findViewById(R.id.image);
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
        mSubscriptions.unsubscribe();
    }

    private void setupConnection() {
        Subscription subscription = mRxDrive.connectionObservable()
                .observeOn(Schedulers.io())
                .filter(new Func1<ConnectionState, Boolean>() {
                    @Override
                    public Boolean call(ConnectionState connectionState) {
                        return connectionState.isConnected();
                    }
                })
                .flatMapSingle(new Func1<ConnectionState, Single<InputStream>>() {
                    @Override
                    public Single<InputStream> call(ConnectionState connectionState) {
                        return mRxDrive.open(mDriveId, getProgressSubscriber());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<InputStream>() {
                    @Override
                    public void call(InputStream inputStream) {
                        loadImage(inputStream);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
        mSubscriptions.add(subscription);
    }

    private void loadImage(InputStream inputStream) {
        try {
            Glide.with(this)
                    .load(IOUtils.toByteArray(inputStream))
                    .fitCenter()
                    .listener(getRequestListener())
                    .into(mImageView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private RequestListener<byte[], GlideDrawable> getRequestListener() {
        return new RequestListener<byte[], GlideDrawable>() {
            @Override
            public boolean onException(Exception e, byte[] model, Target<GlideDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, byte[] model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                mAttacher.update();
                return false;
            }
        };
    }

    @NonNull
    private Subscriber<Progress> getProgressSubscriber() {
        return new Subscriber<Progress>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Progress progress) {
                mTextView.setText(progress.getPercentage() + "%");
            }
        };
    }
}

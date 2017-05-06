package com.francescocervone.rxdrive.sample;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.francescocervone.rxdrive.RxDrive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class DriveFileAdapter extends RecyclerView.Adapter<DriveFileAdapter.DriveFileViewHolder> {
    private static final String TAG = DriveFileAdapter.class.getName();
    private RxDrive mRxDrive;

    private List<Metadata> mResources = new ArrayList<>();

    private OnDriveIdClickListener mListener;

    public static class DriveFileViewHolder extends RecyclerView.ViewHolder {

        private TextView mTextView;
        private Button mRemoveButton;

        public DriveFileViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.percentage);
            mRemoveButton = (Button) itemView.findViewById(R.id.remove);
        }
    }

    public DriveFileAdapter(@NonNull RxDrive rxDrive) {
        mRxDrive = rxDrive;
    }

    public DriveFileAdapter(@NonNull RxDrive rxDrive, @NonNull List<Metadata> resources) {
        this(rxDrive);
        mResources = resources;
    }

    public void setDriveIdClickListener(OnDriveIdClickListener listener) {
        mListener = listener;
    }

    public void setResources(List<Metadata> resources) {
        mResources = resources;
        notifyDataSetChanged();
    }

    public void addResource(Metadata resource) {
        mResources.add(resource);
        notifyItemInserted(mResources.size() - 1);
    }

    @Override
    public DriveFileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_drive_file, parent, false);
        return new DriveFileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final DriveFileViewHolder holder, int position) {
        final Metadata metadata = mResources.get(position);
        holder.mTextView.setText(metadata.getTitle());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDriveIdClick(metadata.getDriveId());
            }
        });
        holder.mRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRxDrive.delete(metadata.getDriveId().asDriveResource())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action0() {
                            @Override
                            public void call() {
                                remove(holder.getAdapterPosition());
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                log(throwable);
                            }
                        });
            }
        });
    }

    private void remove(int position) {
        mResources.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mResources.size();
    }

    private void log(Object object) {
        Log.d(TAG, "log: " + object);
        if (object instanceof Throwable) {
            ((Throwable) object).printStackTrace();
        }
    }

    interface OnDriveIdClickListener {
        void onDriveIdClick(DriveId driveId);
    }
}

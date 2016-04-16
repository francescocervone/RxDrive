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
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.Metadata;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

public class DriveFileAdapter extends RecyclerView.Adapter<DriveFileAdapter.DriveFileViewHolder> {
    private static final String TAG = DriveFileAdapter.class.getName();
    private RxDrive mRxDrive;

    private List<DriveFile> mFiles = new ArrayList<>();

    public static class DriveFileViewHolder extends RecyclerView.ViewHolder {

        private TextView mTextView;
        private Button mRemoveButton;

        public DriveFileViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.text);
            mRemoveButton = (Button) itemView.findViewById(R.id.remove);
        }
    }

    public DriveFileAdapter(@NonNull RxDrive rxDrive) {
        mRxDrive = rxDrive;
    }

    public DriveFileAdapter(@NonNull RxDrive rxDrive, @NonNull List<DriveFile> files) {
        this(rxDrive);
        mFiles = files;
    }

    public void setFiles(List<DriveFile> files) {
        mFiles = files;
        notifyItemRangeInserted(0, mFiles.size());
    }

    public void addFile(DriveFile file) {
        mFiles.add(file);
        notifyItemInserted(mFiles.size() - 1);
    }

    @Override
    public DriveFileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_drive_file, parent, false);
        return new DriveFileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final DriveFileViewHolder holder, int position) {
        final DriveFile driveFile = mFiles.get(position);
        mRxDrive.metadata(driveFile)
                .subscribe(new Action1<Metadata>() {
                    @Override
                    public void call(Metadata metadata) {
                        holder.mTextView.setText(metadata.getTitle());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        log(throwable);
                    }
                });

        holder.mRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRxDrive.removeFile(driveFile)
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean removed) {
                                if (removed) {
                                    removeFile(holder.getAdapterPosition());
                                }
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

    private void removeFile(int position) {
        mFiles.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    private void log(Object object) {
        Log.d(TAG, "log: " + object);
        if (object instanceof Throwable) {
            ((Throwable) object).printStackTrace();
        }
    }

}

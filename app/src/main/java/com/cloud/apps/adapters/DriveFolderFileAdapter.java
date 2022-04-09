package com.cloud.apps.adapters;

import static com.cloud.apps.utils.Consts.DOWNLOAD_PATH;
import static com.cloud.apps.utils.Functions.convertedTime;
import static com.cloud.apps.utils.Functions.getSize;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.apps.R;
import com.cloud.apps.databinding.AdapterDriveFolderFileListBinding;
import com.cloud.apps.models.DriveFile;
import com.cloud.apps.repo.FilesRepo;
import com.cloud.apps.repo.UserRepo;
import com.cloud.apps.utils.Functions;

import java.io.File;
import java.util.ArrayList;

public class DriveFolderFileAdapter extends RecyclerView.Adapter<DriveFolderFileAdapter.ViewHolder> {

    Context context;
    ArrayList<DriveFile> folders;
    OnClick listener;
    boolean isClickAble;
    UserRepo userRepo;
    FilesRepo filesRepo;

    public DriveFolderFileAdapter(Context context, ArrayList<DriveFile> folders, OnClick listener, boolean isClickAble) {
        this.context = context;
        this.folders = folders;
        this.listener = listener;
        this.isClickAble = isClickAble;
        userRepo = UserRepo.getInstance(context);
        filesRepo = FilesRepo.getInstance();
    }

    public DriveFolderFileAdapter(Context context, ArrayList<DriveFile> folders, boolean isClickAble) {
        this.context = context;
        this.folders = folders;
        this.isClickAble = isClickAble;
        userRepo = UserRepo.getInstance(context);
        filesRepo = FilesRepo.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_drive_folder_file_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DriveFolderFileAdapter.ViewHolder h, int position) {
        int p = h.getAdapterPosition();
        DriveFile selectedFile = folders.get(p);
        boolean isDownloaded = filesRepo.getDownloadedFileNames().contains(selectedFile.getName());
        h.binding.fileNameTv.setText(selectedFile.getName());
        h.binding.iconIv.setImageResource(selectedFile.isFolder() ? R.drawable.ic_baseline_folder_24 : R.drawable.ic_baseline_insert_drive_file_24);
        h.binding.downloadIbt.setVisibility(selectedFile.isFolder() ? View.INVISIBLE : View.VISIBLE);
        h.binding.downloadIbt.setImageResource(isDownloaded ? R.drawable.ic_baseline_check_box_24 : R.drawable.ic_baseline_download_24);


        h.itemView.setOnClickListener(v -> {
            if (!isClickAble || !selectedFile.isFolder())
                return;
            listener.onClick(selectedFile.getId());
        });

        h.binding.downloadIbt.setOnClickListener(v -> {
            if (isDownloaded)
                return;
            h.binding.downloadIbt.setVisibility(View.INVISIBLE);
            h.binding.progressBar.setVisibility(View.VISIBLE);
            userRepo.getDriveServiceHelper().downloadFile(new File(DOWNLOAD_PATH, selectedFile.getName()), selectedFile.getId()).addOnSuccessListener(aBoolean -> {
                if (aBoolean) {
                    h.binding.downloadIbt.setImageResource(R.drawable.ic_baseline_check_box_24);
                    h.binding.downloadIbt.setVisibility(View.VISIBLE);
                    h.binding.progressBar.setVisibility(View.INVISIBLE);
                    File file = new File(DOWNLOAD_PATH + "/" + selectedFile.getName());
                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + selectedFile.getName() + " (" + getSize(file.length()) + ")" + "5");
                } else {
                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + selectedFile.getName() + "6");
                }
            });
        });


    }

    @Override
    public int getItemCount() {
        return folders.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        AdapterDriveFolderFileListBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = AdapterDriveFolderFileListBinding.bind(itemView);
        }
    }

    public interface OnClick {
        void onClick(String id);
    }

}
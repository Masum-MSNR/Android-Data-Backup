package com.cloud.apps.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.apps.R;
import com.cloud.apps.databinding.AdapterFolderFileListBinding;

import java.io.File;
import java.util.ArrayList;

public class FolderFileAdapter extends RecyclerView.Adapter<FolderFileAdapter.ViewHolder> {

    Context context;
    ArrayList<File> folders;
    OnClick listener;
    boolean isClickAble;

    public FolderFileAdapter(Context context, ArrayList<File> folders, OnClick listener, boolean isClickAble) {
        this.context = context;
        this.folders = folders;
        this.listener = listener;
        this.isClickAble = isClickAble;
    }

    public FolderFileAdapter(Context context, ArrayList<File> folders, boolean isClickAble) {
        this.context = context;
        this.folders = folders;
        this.isClickAble = isClickAble;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_folder_file_list_narrow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FolderFileAdapter.ViewHolder h, int position) {

        int p = h.getAdapterPosition();
        File selectedFile = folders.get(p);
        h.binding.fileNameTv.setText(selectedFile.getName());
        h.binding.iconIv.setImageResource(selectedFile.isDirectory() ? R.drawable.ic_baseline_folder_24 : R.drawable.ic_baseline_insert_drive_file_24);


        h.itemView.setOnClickListener(v -> {
            if (!isClickAble || !selectedFile.isDirectory())
                return;
            listener.onClick(selectedFile.getName());
        });

    }

    @Override
    public int getItemCount() {
        return folders.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        AdapterFolderFileListBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = AdapterFolderFileListBinding.bind(itemView);
        }
    }

    public interface OnClick {
        void onClick(String currentDir);
    }

}
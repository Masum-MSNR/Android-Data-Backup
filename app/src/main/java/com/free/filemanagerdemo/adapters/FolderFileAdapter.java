package com.free.filemanagerdemo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.free.filemanagerdemo.R;
import com.free.filemanagerdemo.databinding.AdapterFolderFileListBinding;

import java.io.File;
import java.util.ArrayList;

public class FolderFileAdapter extends RecyclerView.Adapter<FolderFileAdapter.ViewHolder> {

    Context context;
    ArrayList<File> folders;

    public FolderFileAdapter(Context context, ArrayList<File> folders) {
        this.context = context;
        this.folders = folders;
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

}
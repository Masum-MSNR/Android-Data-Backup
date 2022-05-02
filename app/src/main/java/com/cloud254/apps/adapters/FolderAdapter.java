package com.cloud254.apps.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud254.apps.R;
import com.cloud254.apps.databinding.AdapterFolderFileListBinding;

import java.io.File;
import java.util.ArrayList;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    Context context;
    ArrayList<File> folders;
    ClickListener listener;
    ArrayList<String> paths;

    public FolderAdapter(Context context, ArrayList<File> folders, ClickListener listener) {
        this.context = context;
        this.folders = folders;
        this.listener = listener;
        paths = new ArrayList<>();
    }

    public FolderAdapter(Context context, ArrayList<File> folders) {
        this.context = context;
        this.folders = folders;
        paths = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_folder_file_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FolderAdapter.ViewHolder h, int position) {

        int p = h.getAdapterPosition();
        File selectedFile = folders.get(p);
        h.binding.fileNameTv.setText(selectedFile.getName());
        h.binding.iconIv.setImageResource(selectedFile.isDirectory() ? R.drawable.ic_baseline_folder_24 : R.drawable.ic_baseline_insert_drive_file_24);

        if (paths.contains(selectedFile.getAbsolutePath())) {
            h.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
            h.binding.closeIbt.setVisibility(View.VISIBLE);
        } else {
            h.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            h.binding.closeIbt.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (paths.size() > 0) {
                h.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
                h.binding.closeIbt.setVisibility(View.VISIBLE);
                if (!paths.contains(selectedFile.getAbsolutePath()))
                    paths.add(selectedFile.getAbsolutePath());
                return;
            }
            listener.onClick(selectedFile.getName());
        });

        h.itemView.setOnLongClickListener(v -> {
            h.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
            h.binding.closeIbt.setVisibility(View.VISIBLE);
            if (!paths.contains(selectedFile.getAbsolutePath()))
                paths.add(selectedFile.getAbsolutePath());
            return true;
        });

        h.binding.closeIbt.setOnClickListener(v -> {
            h.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            h.binding.closeIbt.setVisibility(View.GONE);
            paths.remove(selectedFile.getAbsolutePath());
        });

    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public ArrayList<String> getPaths() {
        return paths;
    }

    public interface ClickListener {
        void onClick(String currentDir);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        AdapterFolderFileListBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = AdapterFolderFileListBinding.bind(itemView);
        }
    }
}
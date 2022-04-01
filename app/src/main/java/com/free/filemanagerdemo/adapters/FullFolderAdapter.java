package com.free.filemanagerdemo.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.free.filemanagerdemo.R;
import com.free.filemanagerdemo.databinding.AdapterFullFolderBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FullFolderAdapter extends RecyclerView.Adapter<FullFolderAdapter.ViewHolder> {

    Context context;
    ArrayList<String> folders;

    public FullFolderAdapter(Context context, ArrayList<String> folders) {
        this.context = context;
        this.folders = folders;
        Log.v("size", String.valueOf(folders.size()));
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_full_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FullFolderAdapter.ViewHolder h, int position) {

        int p = h.getAdapterPosition();
        String revPath = new StringBuilder(folders.get(p)).reverse().toString();
        int i = revPath.indexOf('/');
        revPath = revPath.substring(0, i);
        revPath = new StringBuilder(revPath).reverse().toString();
        Log.v("path", revPath);
        h.binding.folderNameTv.setText(revPath);


        h.itemView.setOnClickListener(v -> {
            h.binding.folderItemsRv.setVisibility(h.binding.folderItemsRv.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
//            notifyItemChanged(p);
        });

        h.binding.folderItemsRv.setLayoutManager(new LinearLayoutManager(context));

        File root = new File(folders.get(p));
        File[] filesAndFolders = root.listFiles();
        ArrayList<File> fileFolders = new ArrayList<>();
        FolderFileAdapter adapter = new FolderFileAdapter(context, fileFolders);
        h.binding.folderItemsRv.setAdapter(adapter);
        if (filesAndFolders != null) {
            fileFolders.addAll(Arrays.asList(filesAndFolders));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        AdapterFullFolderBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = AdapterFullFolderBinding.bind(itemView);
        }
    }


}
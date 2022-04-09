package com.cloud.apps.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.apps.R;
import com.cloud.apps.databinding.AdapterFullFolderBinding;
import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.cloud.apps.repo.UserRepo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FullFolderAdapter extends RecyclerView.Adapter<FullFolderAdapter.ViewHolder> {

    Context context;
    ArrayList<String> folders;
    GoogleDriveServiceHelper helper;
    Map<String, Boolean> synced;

    public FullFolderAdapter(Context context, ArrayList<String> folders, GoogleDriveServiceHelper helper) {
        this.context = context;
        this.folders = folders;
        this.helper = helper;
        synced = new HashMap<>();
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
        h.binding.folderNameTv.setText(revPath);
        if (synced.get(folders.get(p)) != null && synced.get(folders.get(p))) {
            h.binding.syncIv.setImageResource(R.drawable.ic_baseline_check_box_24);
        } else {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.rotate_animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    h.binding.syncIv.setImageResource(R.drawable.ic_baseline_check_box_24);
                    synced.put(folders.get(p), true);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            h.binding.syncIv.setImageResource(R.drawable.ic_baseline_sync_24);
            h.binding.syncIv.startAnimation(animation);
            helper.uploadFolder(folders.get(p), UserRepo.getInstance(context).getRootFolderId(), fileCounter(folders.get(p)), animation, p);
        }
        h.itemView.setOnClickListener(v -> {
            h.binding.folderItemsRv.setVisibility(h.binding.folderItemsRv.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        h.binding.folderItemsRv.setLayoutManager(new LinearLayoutManager(context));

        File root = new File(folders.get(p));
        File[] filesAndFolders = root.listFiles();
        ArrayList<File> fileFolders = new ArrayList<>();
        FolderFileAdapter adapter = new FolderFileAdapter(context, fileFolders, false);
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

    private int fileCounter(String path) {
        int count = 0;
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        for (File file : filesAndFolders) {
            if (file.isDirectory()) {
                count += fileCounter(file.getPath());
            } else
                count++;
        }
        return count;
    }
}
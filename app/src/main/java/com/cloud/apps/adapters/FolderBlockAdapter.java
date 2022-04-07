package com.cloud.apps.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.apps.R;
import com.cloud.apps.databinding.AdapterFolderBlockBinding;

import java.util.ArrayList;

public class FolderBlockAdapter extends RecyclerView.Adapter<FolderBlockAdapter.ViewHolder> {

    Context context;
    ArrayList<String> folderNames;

    public FolderBlockAdapter(Context context, ArrayList<String> folderNames) {
        this.context = context;
        this.folderNames = folderNames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_folder_block, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        int p = h.getAdapterPosition();
        if (p == (4 + folderNames.size())) {
            h.binding.folderImageIv.setVisibility(View.INVISIBLE);
            h.binding.folderNameTv.setVisibility(View.INVISIBLE);
            h.binding.plusIv.setVisibility(View.VISIBLE);
            return;
        }
        switch (p) {
            case 0:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_image_folder);
                h.binding.folderNameTv.setText("Images");
                break;
            case 1:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_video_folder);
                h.binding.folderNameTv.setText("Videos");
                break;
            case 2:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_documents_folder);
                h.binding.folderNameTv.setText("Documents");
                break;
            case 3:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_audio_folder);
                h.binding.folderNameTv.setText("Audios");
                break;
            default:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_fit_folder);
                h.binding.folderNameTv.setText(folderNames.get(p - 4));
        }
    }

    @Override
    public int getItemCount() {
        return 4 + folderNames.size() + 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        AdapterFolderBlockBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = AdapterFolderBlockBinding.bind(itemView);
        }
    }
}

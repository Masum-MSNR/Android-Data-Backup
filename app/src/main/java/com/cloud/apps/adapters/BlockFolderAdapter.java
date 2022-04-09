package com.cloud.apps.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.apps.R;
import com.cloud.apps.activities.SpecificFileActivity;
import com.cloud.apps.activities.ViewFolderActivity;
import com.cloud.apps.databinding.AdapterFolderBlockBinding;

import java.util.ArrayList;

public class BlockFolderAdapter extends RecyclerView.Adapter<BlockFolderAdapter.ViewHolder> {

    Context context;
    ArrayList<String> folderNames;
    OnPlusClick listener;

    public BlockFolderAdapter(Context context, ArrayList<String> folderNames, OnPlusClick listener) {
        this.context = context;
        this.folderNames = folderNames;
        this.listener = listener;
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
            h.binding.plusIv.setOnClickListener(v -> listener.onPlusClick());
            return;
        } else {
            h.binding.folderImageIv.setVisibility(View.VISIBLE);
            h.binding.folderNameTv.setVisibility(View.VISIBLE);
            h.binding.plusIv.setVisibility(View.INVISIBLE);
        }
        switch (p) {
            case 0:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_image_folder);
                h.binding.folderNameTv.setText("Images");
                initiateClickListener(h.itemView, "image");
                break;
            case 1:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_video_folder);
                h.binding.folderNameTv.setText("Videos");
                initiateClickListener(h.itemView, "video");
                break;
            case 2:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_documents_folder);
                h.binding.folderNameTv.setText("Documents");
                initiateClickListener(h.itemView, "document");
                break;
            case 3:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_audio_folder);
                h.binding.folderNameTv.setText("Audios");
                initiateClickListener(h.itemView, "audio");
                break;
            default:
                h.binding.folderImageIv.setImageResource(R.drawable.ic_fit_folder);
                h.binding.folderNameTv.setText(folderNames.get(p - 4));
                h.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ViewFolderActivity.class);
                    intent.putExtra("path", folderNames.get(p-4));
                    context.startActivity(intent);
                });
        }
    }

    private void initiateClickListener(View itemView, String type) {
        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SpecificFileActivity.class);
            intent.putExtra("type", type);
            context.startActivity(intent);
        });
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

    public interface OnPlusClick {
        void onPlusClick();
    }
}

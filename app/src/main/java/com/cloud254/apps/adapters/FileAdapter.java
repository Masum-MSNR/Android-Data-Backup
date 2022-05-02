package com.cloud254.apps.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud254.apps.R;
import com.cloud254.apps.databinding.AdapterFileBinding;
import com.cloud254.apps.models.SingleFile;

import java.util.ArrayList;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    Context context;
    ArrayList<SingleFile> files;
    String type;

    public FileAdapter(Context context, ArrayList<SingleFile> files, String type) {
        this.context = context;
        this.files = files;
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        int p = h.getAdapterPosition();
        h.binding.fileNameTv.setText(files.get(p).getName());
        h.binding.fileParentTv.setText(files.get(p).getParent());
        h.binding.fileSizeTv.setText(files.get(p).getSize());
//        switch (type) {
//            case "image":
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                Bitmap pb = BitmapFactory.decodeFile(files.get(p).getParent() + "/" + files.get(p).getName(), options);
//                h.binding.thumbIv.setImageBitmap(pb);
//                break;
//            case "video":
//                Bitmap vb = ThumbnailUtils.createVideoThumbnail(files.get(p).getParent() + "/" + files.get(p).getName(), MediaStore.Images.Thumbnails.MICRO_KIND);
//                h.binding.thumbIv.setImageBitmap(vb);
//                break;
//        }

    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        AdapterFileBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = AdapterFileBinding.bind(itemView);
        }
    }
}


package com.cloud.apps.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.apps.R;
import com.cloud.apps.databinding.AdapterLogBinding;
import com.cloud.apps.models.LoG;

import java.util.ArrayList;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {


    Context context;
    ArrayList<LoG> logs;

    public LogAdapter(Context context, ArrayList<LoG> logs) {
        this.context = context;
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LogAdapter.ViewHolder h, int position) {
        int p = h.getAdapterPosition();

        h.binding.dateTimeTv.setText(logs.get(p).getDate());
        h.binding.detailsTv.setText(logs.get(p).getDetails());

        switch (logs.get(p).getStatus()) {
            case 1:
                h.binding.detailsTv.setTextColor(ContextCompat.getColor(context, R.color.green));
                h.binding.statusTv.setText("Created:");
                h.binding.statusTv.setTextColor(ContextCompat.getColor(context, R.color.green));
                break;
            case 3:
                h.binding.detailsTv.setTextColor(ContextCompat.getColor(context, R.color.green));
                h.binding.statusTv.setText("Uploaded:");
                h.binding.statusTv.setTextColor(ContextCompat.getColor(context, R.color.green));
                break;
            case 5:
                h.binding.detailsTv.setTextColor(ContextCompat.getColor(context, R.color.green));
                h.binding.statusTv.setText("Downloaded:");
                h.binding.statusTv.setTextColor(ContextCompat.getColor(context, R.color.green));
                break;
            case 2:
            case 4:
            case 6:
                h.binding.detailsTv.setTextColor(ContextCompat.getColor(context, R.color.error));
                h.binding.statusTv.setText("Failed:");
                h.binding.statusTv.setTextColor(ContextCompat.getColor(context, R.color.error));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        AdapterLogBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = AdapterLogBinding.bind(itemView);
        }
    }

}
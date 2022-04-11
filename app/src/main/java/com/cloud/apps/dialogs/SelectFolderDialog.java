package com.cloud.apps.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloud.apps.R;
import com.cloud.apps.adapters.FolderAdapter;
import com.cloud.apps.databinding.DialogSelectFolderBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class SelectFolderDialog extends AppCompatDialogFragment implements FolderAdapter.ClickListener {

    DialogSelectFolderBinding binding;
    Context context;
    String path, currentPath;
    ArrayList<File> folders;
    FolderAdapter adapter;
    SelectListener listener;
    int type;

    public SelectFolderDialog(Context context, SelectListener listener, int type) {
        this.context = context;
        this.listener = listener;
        this.type = type;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.Dialog);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_select_folder, null);
        binding = DialogSelectFolderBinding.bind(view);

        binding.instructionTv.setPaintFlags(binding.instructionTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);


        folders = new ArrayList<>();
        adapter = new FolderAdapter(context, folders, this);

        currentPath = path = Environment.getExternalStorageDirectory().getPath();
        binding.filesRv.setLayoutManager(new LinearLayoutManager(context));
        binding.filesRv.setAdapter(adapter);

        loadStorage(path);

        binding.backIbt.setOnClickListener(v -> {
            if (path.equals(currentPath))
                return;
            String revPath = new StringBuilder(currentPath).reverse().toString();
            int i = revPath.indexOf('/');
            revPath = revPath.substring(i + 1);
            revPath = new StringBuilder(revPath).reverse().toString();
            currentPath = revPath;
            loadStorage(currentPath);
        });

        binding.closeIbt.setOnClickListener(v -> {
            dismiss();
        });

        binding.selectTv.setOnClickListener(v -> {
            if (adapter.getPaths().size() <= 0) {
                Toast.makeText(context, "Please select any folder. Tap and hold to select.", Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onSelect(adapter.getPaths(), type);
            dismiss();
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogPopAnimation;
        return dialog;
    }

    public void loadStorage(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        folders.clear();
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    folders.add(file);
                }
            }
        }
        if (folders == null || folders.size() == 0) {
            adapter.notifyDataSetChanged();
            binding.emptyTv.setVisibility(View.VISIBLE);
            return;
        }
        Collections.sort(folders, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));
        binding.emptyTv.setVisibility(View.INVISIBLE);
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onClick(String currentDir) {
        currentPath = currentPath + "/" + currentDir;
        loadStorage(currentPath);
    }

    public interface SelectListener {
        void onSelect(ArrayList<String> paths, int type);
    }
}

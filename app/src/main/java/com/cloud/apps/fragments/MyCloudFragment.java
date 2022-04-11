package com.cloud.apps.fragments;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;
import static com.cloud.apps.utils.Consts.GLOBAL_KEY;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.folderTrack;
import static com.cloud.apps.utils.Consts.previousId;
import static com.cloud.apps.utils.Functions.setRootId;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloud.apps.adapters.DriveFolderFileAdapter;
import com.cloud.apps.databinding.FragmentMyCloudBinding;
import com.cloud.apps.dialogs.LoadingDialog;
import com.cloud.apps.models.DriveFile;
import com.cloud.apps.repo.UserRepo;

import java.util.ArrayList;
import java.util.Stack;

public class MyCloudFragment extends Fragment implements DriveFolderFileAdapter.OnClick {

    FragmentMyCloudBinding binding;

    Context context;
    UserRepo userRepo;

    MutableLiveData<Boolean> isConnecting;
    DriveFolderFileAdapter adapter;
    ArrayList<DriveFile> files;
    Stack<ArrayList<DriveFile>> listStack;
    Observer<String> observer;
    String currentId;
    LoadingDialog dialog;

    public MyCloudFragment(Context context) {
        this.context = context;
        userRepo = UserRepo.getInstance(context);
        isConnecting = new MutableLiveData<>(false);
        files = new ArrayList<>();
        listStack = new Stack<>();
        dialog = new LoadingDialog();
        dialog.setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMyCloudBinding.inflate(getLayoutInflater());

        adapter = new DriveFolderFileAdapter(context, files, this, true);
        binding.driveFilesRv.setLayoutManager(new LinearLayoutManager(context));
        binding.driveFilesRv.setAdapter(adapter);

        dialog.show(getChildFragmentManager(), dialog.getTag());
        //listeners

        observer = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                Log.d(TAG, "onCreateView: ");
                if (s.isEmpty()) {
                    return;
                }
                currentId = s;
                files.clear();
                files.addAll(listStack.pop());
                binding.notifier.setVisibility(files.size() == 0 ? View.VISIBLE : View.INVISIBLE);
                loadRv();
            }
        };
        previousId.observe((LifecycleOwner) context, observer);

        isConnecting.observe((LifecycleOwner) context, aBoolean -> {
            if (aBoolean) {
                dialog.show(getChildFragmentManager(), dialog.getTag());
            } else {
                dialog.dismiss();
            }
            binding.progressFrame.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
        });

        binding.tryAgainBt.setOnClickListener(v -> {
            dialog.show(getChildFragmentManager(), dialog.getTag());
            binding.tryAgainBt.setVisibility(View.GONE);
            checkRootFolder();
        });


        checkLogin();

        return binding.getRoot();
    }


    private void loadDriveFiles(String id) {
        dialog.show(getChildFragmentManager(), dialog.getTag());
        files.clear();
        userRepo.getDriveDownloadService().getDataFromDrive(id).addOnSuccessListener(driveFiles -> {
            binding.notifier.setText(driveFiles.size() == 0 ? "Empty" : "");
            binding.notifier.setVisibility(driveFiles.size() == 0 ? View.VISIBLE : View.INVISIBLE);
            files.addAll(driveFiles);
            loadRv();
        });
    }

    private void loadRv() {
        dialog.dismiss();
        adapter.notifyDataSetChanged();
    }


    private void checkLogin() {
        if (!userRepo.getLogin().getValue()) {
            binding.notifier.setText("Please connect with Google Drive first.");
            binding.notifier.setVisibility(View.VISIBLE);
        } else {
            binding.notifier.setText("");
            binding.notifier.setVisibility(View.INVISIBLE);
            if (userRepo.getRootFolderId() == null) {
                isConnecting.setValue(true);
                checkRootFolder();
            } else {
                currentId = userRepo.getRootFolderId();
                loadDriveFiles(userRepo.getRootFolderId());
            }
        }
    }

    private void checkRootFolder() {
        userRepo.getDriveServiceHelper().isFolderPresent(GLOBAL_KEY, "root").addOnSuccessListener(id -> {
            if (id.isEmpty()) {
                userRepo.getDriveServiceHelper().createNewFolder(GLOBAL_KEY, "root").addOnSuccessListener(rootId -> {
                    if (!rootId.isEmpty()) {
                        userRepo.setRootFolderId(rootId);
                        isConnecting.setValue(false);
                        setRootId("root_id", rootId, context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
                        loadDriveFiles(rootId);
                    } else {
                        binding.tryAgainBt.setVisibility(View.VISIBLE);
                        dialog.dismiss();
                        binding.progressFrame.setVisibility(View.GONE);
                        Toast.makeText(context, "Something went wrong! Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                userRepo.setRootFolderId(id);
                isConnecting.setValue(false);
                setRootId("root_id", id, context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
                loadDriveFiles(id);
            }
        });
    }

    @Override
    public void onClick(String id) {
        folderTrack.push(currentId);
        currentId = id;
        ArrayList<DriveFile> driveFiles = new ArrayList<>(adapter.getFolders());
        listStack.push(driveFiles);
        files.clear();
        adapter.notifyDataSetChanged();
        loadDriveFiles(id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        folderTrack.clear();
        previousId.setValue("");
        previousId.removeObserver(observer);
    }
}
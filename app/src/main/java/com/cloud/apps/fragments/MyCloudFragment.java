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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloud.apps.adapters.DriveFolderFileAdapter;
import com.cloud.apps.databinding.FragmentMyCloudBinding;
import com.cloud.apps.models.DriveFile;
import com.cloud.apps.repo.UserRepo;

import java.util.ArrayList;

public class MyCloudFragment extends Fragment implements DriveFolderFileAdapter.OnClick {

    FragmentMyCloudBinding binding;

    Context context;
    UserRepo userRepo;

    MutableLiveData<Boolean> isConnecting;
    DriveFolderFileAdapter adapter;
    ArrayList<DriveFile> files;
    ArrayList<DriveFile> tempDriveFiles;

    String currentId;

    public MyCloudFragment(Context context) {
        this.context = context;
        userRepo = UserRepo.getInstance(context);
        isConnecting = new MutableLiveData<>(false);
        files = new ArrayList<>();
        tempDriveFiles = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMyCloudBinding.inflate(getLayoutInflater());


        adapter = new DriveFolderFileAdapter(context, tempDriveFiles, this, true);
        binding.driveFilesRv.setLayoutManager(new LinearLayoutManager(context));
        binding.driveFilesRv.setAdapter(adapter);
        //listeners

        previousId.observe((LifecycleOwner) context, s -> {
            if (s.isEmpty()) {
                return;
            }
            currentId = s;
            loadRv(s);
        });

        isConnecting.observe((LifecycleOwner) context, aBoolean -> {
            binding.progressBar.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            binding.progressFrame.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
        });

        binding.tryAgainBt.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tryAgainBt.setVisibility(View.GONE);
            checkRootFolder();
        });


        checkLogin();

        return binding.getRoot();
    }


    private void loadDriveFiles(String id) {
        Log.d(TAG, "loadDriveFiles: ");
        binding.centerPb.setVisibility(View.VISIBLE);
        files.clear();
        userRepo.getDriveServiceHelper().getDataFromDrive(id).addOnSuccessListener(driveFiles -> {
            binding.notifier.setText(driveFiles.size() == 0 ? "Empty" : "");
            binding.notifier.setVisibility(driveFiles.size() == 0 ? View.VISIBLE : View.INVISIBLE);
            files.addAll(driveFiles);
            loadRv(id);
        });
    }

    private void loadRv(String id) {
        tempDriveFiles.clear();
        for (DriveFile f : files) {
            if (f.getParent().equals(id)) {
                tempDriveFiles.add(f);
            }
        }
        binding.centerPb.setVisibility(View.INVISIBLE);
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
                        binding.progressBar.setVisibility(View.GONE);
                        binding.progressFrame.setVisibility(View.GONE);
                        Toast.makeText(context,"Something went wrong! Please try again.", Toast.LENGTH_SHORT).show();
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
        loadRv(id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        folderTrack.clear();
        previousId.setValue("");
    }
}
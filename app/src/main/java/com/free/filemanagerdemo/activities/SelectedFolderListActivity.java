package com.free.filemanagerdemo.activities;

import static com.free.filemanagerdemo.utils.Consts.MY_PREFS_NAME;
import static com.free.filemanagerdemo.utils.Consts.ROOT_KEY;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.free.filemanagerdemo.adapters.FullFolderAdapter;
import com.free.filemanagerdemo.databinding.ActivitySelectedFolderListBinding;
import com.free.filemanagerdemo.dialogs.LoadingDialog;
import com.free.filemanagerdemo.dialogs.SelectFolderDialog;
import com.free.filemanagerdemo.driveApi.GoogleDriveServiceHelper;
import com.free.filemanagerdemo.utils.Functions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import net.steamcrafted.loadtoast.LoadToast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SelectedFolderListActivity extends AppCompatActivity implements SelectFolderDialog.SelectListener {

    ActivitySelectedFolderListBinding binding;

    ArrayList<String> folders;
    FullFolderAdapter adapter;

    GoogleDriveServiceHelper driverServiceHelper;

    public static LoadingDialog loadingDialog;

    LoadToast loadToast;
    SelectFolderDialog selectFolderDialog;

    public static int count;
    public static int uploadedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectedFolderListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Selected Folder Being Synced");

        selectFolderDialog = new SelectFolderDialog(this, this);
        folders = new ArrayList<>();
        loadingDialog = new LoadingDialog();
        loadToast = new LoadToast(this);

        binding.uploadFab.setVisibility(GoogleSignIn.getLastSignedInAccount(this) != null ? View.VISIBLE : View.GONE);

        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(this).getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Drive API Migration")
                            .build();
            driverServiceHelper = new GoogleDriveServiceHelper(this, googleDriveService);
        }

        binding.addFab.setOnClickListener(v -> {
            selectFolderDialog.show(getSupportFragmentManager(), selectFolderDialog.getTag());
        });

        binding.uploadFab.setOnClickListener(v -> upload(ROOT_KEY));

        adapter = new FullFolderAdapter(this, folders, driverServiceHelper);
        binding.selectedFoldersRv.setLayoutManager(new LinearLayoutManager(this));
        binding.selectedFoldersRv.setAdapter(adapter);
        loadRecyclerView();
    }

    public void loadRecyclerView() {
        folders.clear();
        folders.addAll(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        if (folders.size() > 0)
            binding.noSelectedFolderTv.setVisibility(View.GONE);
        else {
            binding.noSelectedFolderTv.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    public void upload(String id) {
        Set<String> pathSet = new HashSet<>(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        count = 0;
        uploadedCount = 0;
        for (String path : pathSet) {
            fileCounter(path);
        }
        for (String path : pathSet) {
            uploadFolder(path, id);
        }
    }


    private void fileCounter(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        for (File file : filesAndFolders) {
            if (file.isDirectory()) {
                fileCounter(file.getPath());
            } else
                count++;
        }
    }

    private void uploadFolder(String path, String parentId) {
        File root = new File(path);
        String folderName = root.getName();
        driverServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        driverServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    File[] fileFolders = root.listFiles();
                                    if (fileFolders != null) {
                                        for (File file : fileFolders) {
                                            if (file.isDirectory()) {
                                                uploadFolder(file.getPath(), folderId);
                                            } else {
                                                driverServiceHelper.isFilePresent(file.getPath(), folderId).addOnSuccessListener(aBoolean -> {
                                                    if (!aBoolean) {
                                                        driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), folderId).addOnSuccessListener(aBoolean1 -> {
                                                            if (aBoolean1) {
                                                                SelectedFolderListActivity.uploadedCount++;
                                                                if (SelectedFolderListActivity.count == SelectedFolderListActivity.uploadedCount) {
                                                                    Toast.makeText(SelectedFolderListActivity.this, "Upload Successful.", Toast.LENGTH_SHORT).show();
                                                                    SelectedFolderListActivity.loadingDialog.dismiss();
                                                                }
                                                            }
                                                        });
                                                    } else {
                                                        SelectedFolderListActivity.uploadedCount++;
                                                        if (SelectedFolderListActivity.count == SelectedFolderListActivity.uploadedCount) {
                                                            Toast.makeText(SelectedFolderListActivity.this, "Upload Successful.", Toast.LENGTH_SHORT).show();
                                                            SelectedFolderListActivity.loadingDialog.dismiss();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(exception -> Log.v("e", exception.toString()));
                    } else {
                        File[] fileFolders = root.listFiles();
                        if (fileFolders != null) {
                            for (File file : fileFolders) {
                                if (file.isDirectory()) {
                                    uploadFolder(file.getPath(), id);
                                } else {
                                    driverServiceHelper.isFilePresent(file.getPath(), id).addOnSuccessListener(aBoolean -> {
                                        if (!aBoolean) {
                                            driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), id).addOnSuccessListener(aBoolean1 -> {
                                                if (aBoolean1) {
                                                    SelectedFolderListActivity.uploadedCount++;
                                                    if (SelectedFolderListActivity.count == SelectedFolderListActivity.uploadedCount) {
                                                        Toast.makeText(SelectedFolderListActivity.this, "Upload Successful.", Toast.LENGTH_SHORT).show();
                                                        SelectedFolderListActivity.loadingDialog.dismiss();
                                                    }
                                                }
                                            });
                                        } else {
                                            SelectedFolderListActivity.uploadedCount++;
                                            if (SelectedFolderListActivity.count == SelectedFolderListActivity.uploadedCount) {
                                                Toast.makeText(SelectedFolderListActivity.this, "Upload Successful.", Toast.LENGTH_SHORT).show();
                                                SelectedFolderListActivity.loadingDialog.dismiss();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }

                })
                .addOnFailureListener(exception -> Log.v("e", exception.toString()));
    }

    @Override
    public void onSelect(ArrayList<String> paths) {
        Set<String> pathSet = new HashSet<>();
        pathSet.addAll(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        pathSet.addAll(paths);
        Functions.setPaths("selected_paths", pathSet, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
        loadRecyclerView();
    }
}

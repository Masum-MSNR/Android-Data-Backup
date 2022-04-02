package com.free.filemanagerdemo.activities;

import static com.free.filemanagerdemo.utils.Consts.MY_PREFS_NAME;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.free.filemanagerdemo.adapters.FullFolderAdapter;
import com.free.filemanagerdemo.databinding.ActivitySelectedFolderListBinding;
import com.free.filemanagerdemo.dialogs.LoadingDialog;
import com.free.filemanagerdemo.dialogs.SelectFolderDialog;
import com.free.filemanagerdemo.utils.Functions;
import com.free.filemanagerdemo.utils.GoogleDriveServiceHelper;
import com.free.filemanagerdemo.utils.PermissionHandler;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    ArrayList<String> folders;
    FullFolderAdapter adapter;

    GoogleDriveServiceHelper driverServiceHelper;
    GoogleSignInClient googleSignInClient;

    public static LoadingDialog loadingDialog;

    LoadToast loadToast;
    SelectFolderDialog selectFolderDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectedFolderListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Selected Folder Being Synced");

        selectFolderDialog = new SelectFolderDialog(this, this);
        folders = new ArrayList<>();
        loadingDialog = new LoadingDialog();
        loadToast=new LoadToast(this);


        binding.uploadFab.setVisibility(GoogleSignIn.getLastSignedInAccount(this) != null ? View.VISIBLE : View.GONE);
        binding.signInFab.setVisibility(GoogleSignIn.getLastSignedInAccount(this) != null ? View.GONE : View.VISIBLE);
        binding.signOutFab.setVisibility(GoogleSignIn.getLastSignedInAccount(this) != null ? View.VISIBLE : View.GONE);


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
            if (PermissionHandler.checkForPermission((Activity) this, permissions)) {
                selectFolderDialog.show(getSupportFragmentManager(), selectFolderDialog.getTag());
            } else {
                PermissionHandler.requestPermission(this, permissions, 100);
            }
        });

        binding.uploadFab.setOnClickListener(v -> {
            uploadFile();
        });

        binding.signInFab.setOnClickListener(v -> {
            requestSignIn();
        });

        binding.signOutFab.setOnClickListener(v -> {
            GoogleSignIn.getClient(getApplicationContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    binding.signInFab.setVisibility(View.VISIBLE);
                    binding.signOutFab.setVisibility(View.GONE);
                    binding.uploadFab.setVisibility(View.GONE);
                }
            });
        });
        adapter = new FullFolderAdapter(this, folders);
        binding.selectedFoldersRv.setLayoutManager(new LinearLayoutManager(this));
        binding.selectedFoldersRv.setAdapter(adapter);

        if (PermissionHandler.checkForPermission((Activity) this, permissions)) {
            loadRecyclerView();
        }
    }

    private void requestSignIn() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .requestEmail()
                        .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        startActivityForResult(googleSignInClient.getSignInIntent(), 1);
    }

    public void createFolder(String folderName, String filePath) {
        if (driverServiceHelper != null) {
            driverServiceHelper.isFolderPresent(folderName)
                    .addOnSuccessListener(id -> {
                        if (id.isEmpty()) {
                            driverServiceHelper.createFolder(folderName)
                                    .addOnSuccessListener(folderId -> {
                                        driverServiceHelper.uploadFileToGoogleDrive(filePath, folderId);
                                    })
                                    .addOnFailureListener(exception -> Log.v("e", exception.toString()));
                        } else {
                            driverServiceHelper.uploadFileToGoogleDrive(filePath, id);
                        }

                    })
                    .addOnFailureListener(exception -> Log.v("e", exception.toString()));
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length == 2) {
            selectFolderDialog.show(getSupportFragmentManager(), selectFolderDialog.getTag());
        }
    }

    @Override
    public void onSelect(ArrayList<String> paths) {
        Set<String> pathSet = new HashSet<>();
        pathSet.addAll(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        pathSet.addAll(paths);
        Functions.setPaths("selected_paths", pathSet, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
        loadRecyclerView();
    }

    public static int count;
    public static int uploadedCount;

    public void uploadFile() {
        Set<String> pathSet = new HashSet<>(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        loadingDialog.show(getSupportFragmentManager(), loadingDialog.getTag());
        count = 0;
        uploadedCount = 0;
        for (String path : pathSet) {
            fileCounter(path);
        }
        for (String path : pathSet) {
            File root = new File(path);
            uploadFolder(path);
//            File root = new File(path);
//            File[] filesAndFolders = root.listFiles();
//            if (filesAndFolders != null) {
//                for (File file : filesAndFolders) {
//                    if (file.isDirectory()) {
//
//                    } else {
//
//                    }
//                }
//            }
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

    private void uploadFolder(String path) {
        File root = new File(path);
        String folderName = root.getName();
        driverServiceHelper.isFolderPresent(folderName)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        driverServiceHelper.createFolder(folderName)
                                .addOnSuccessListener(folderId -> {
                                    File[] fileFolders = root.listFiles();
                                    if (fileFolders != null) {
                                        for (File file : fileFolders) {
                                            if (file.isDirectory()) {
                                                uploadFolder(file.getPath());
                                            } else {
                                                driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), folderId).addOnSuccessListener(new OnSuccessListener<Boolean>() {
                                                    @Override
                                                    public void onSuccess(Boolean aBoolean) {
                                                        if(aBoolean){
                                                            SelectedFolderListActivity.uploadedCount++;
                                                            if (SelectedFolderListActivity.count == SelectedFolderListActivity.uploadedCount) {
                                                                Toast.makeText(SelectedFolderListActivity.this, "Upload Successful.", Toast.LENGTH_SHORT).show();
                                                                SelectedFolderListActivity.loadingDialog.dismiss();
                                                            }
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
                                    uploadFolder(file.getPath());
                                } else {
                                    driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), id).addOnSuccessListener(new OnSuccessListener<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean aBoolean) {
                                            if(aBoolean){
                                                SelectedFolderListActivity.uploadedCount++;
                                                if (SelectedFolderListActivity.count == SelectedFolderListActivity.uploadedCount) {
                                                    Toast.makeText(SelectedFolderListActivity.this, "Upload Successful.", Toast.LENGTH_SHORT).show();
                                                    SelectedFolderListActivity.loadingDialog.dismiss();
                                                }
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
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                handleSignInResult(resultData);
            }
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());

                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Drive API Migration")
                                    .build();

                    binding.signInFab.setVisibility(View.GONE);
                    binding.signOutFab.setVisibility(View.VISIBLE);
                    binding.uploadFab.setVisibility(View.VISIBLE);
                    driverServiceHelper = new GoogleDriveServiceHelper(this, googleDriveService);
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("TAG", "Unable to sign in.", exception);
                    }
                });
    }


}

package com.free.filemanagerdemo.activities;

import static com.free.filemanagerdemo.utils.Consts.GLOBAL_KEY;
import static com.free.filemanagerdemo.utils.Consts.MY_PREFS_NAME;
import static com.free.filemanagerdemo.utils.Consts.ROOT_KEY;
import static com.free.filemanagerdemo.utils.Functions.setRootId;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.free.filemanagerdemo.R;
import com.free.filemanagerdemo.databinding.ActivityConnectivityBinding;
import com.free.filemanagerdemo.dialogs.SelectFolderDialog;
import com.free.filemanagerdemo.driveApi.GoogleDriveServiceHelper;
import com.free.filemanagerdemo.utils.Functions;
import com.free.filemanagerdemo.utils.PermissionHandler;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConnectivityActivity extends AppCompatActivity implements SelectFolderDialog.SelectListener {

    private static final String TAG = "ConnectivityActivity";

    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    ActivityConnectivityBinding binding;

    GoogleDriveServiceHelper driverServiceHelper;
    GoogleSignInClient googleSignInClient;

    ActivityResultLauncher<Intent> launcher;

    MutableLiveData<Boolean> isLoggedIn;
    MutableLiveData<Boolean> isConnecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isLoggedIn = new MutableLiveData<>(GoogleSignIn.getLastSignedInAccount(this) != null);
        isConnecting = new MutableLiveData<>(false);


        isLoggedIn.observe(this, aBoolean -> {
            binding.connectToGoogleBt.setVisibility(aBoolean ? View.INVISIBLE : View.VISIBLE);
            binding.connectedTv.setVisibility(aBoolean ? View.VISIBLE : View.INVISIBLE);
            binding.disconnectBt.setVisibility(aBoolean ? View.VISIBLE : View.INVISIBLE);
        });

        isConnecting.observe(this, aBoolean -> {
            binding.connectedTv.setText(aBoolean ? "Connecting to Google Drive..." : "Google Drive is now connected.");
            binding.disconnectBt.setEnabled(!aBoolean && isLoggedIn.getValue());
            binding.pickFolderBt.setEnabled(!aBoolean && isLoggedIn.getValue());
            binding.selectedFoldersBt.setEnabled(!aBoolean && isLoggedIn.getValue());
        });

        if (isLoggedIn.getValue()) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)).getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName(getString(R.string.app_name))
                            .build();
            driverServiceHelper = new GoogleDriveServiceHelper(this, googleDriveService);
            isLoggedIn.setValue(true);
            isConnecting.setValue(true);
            driverServiceHelper.isFolderPresent(GLOBAL_KEY, "root").addOnSuccessListener(id -> {
                if (id.isEmpty()) {
                    driverServiceHelper.createNewFolder(GLOBAL_KEY, "root").addOnSuccessListener(rootId -> {
                        ROOT_KEY = rootId;
                        isConnecting.setValue(false);
                        setRootId("root_id", rootId, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
                    });
                } else {
                    ROOT_KEY = id;
                    isConnecting.setValue(false);
                    setRootId("root_id", id, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
                }
            });
        }

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleSignInResult(result.getData()));

        binding.connectToGoogleBt.setOnClickListener(v -> requestSignIn());

        binding.pickFolderBt.setOnClickListener(v -> {
            SelectFolderDialog dialog = new SelectFolderDialog(this, this);
            dialog.show(getSupportFragmentManager(), dialog.getTag());
        });

        binding.selectedFoldersBt.setOnClickListener(v -> {
            startActivity(new Intent(this, SelectedFolderListActivity.class));
        });

        binding.disconnectBt.setOnClickListener(v -> GoogleSignIn.getClient(getApplicationContext(),
                GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().addOnCompleteListener(task -> {
            isLoggedIn.setValue(false);
            isConnecting.setValue(false);
        }));

        checkPermission();

    }

    private void checkPermission() {
        if (!PermissionHandler.checkForPermission(this, permissions)) {
            PermissionHandler.requestPermission(this, permissions, 100);
        }
    }

    private void requestSignIn() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .requestEmail()
                        .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        launcher.launch(googleSignInClient.getSignInIntent());
    }


    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());

                    Drive googleDriveService = new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName(getString(R.string.app_name))
                            .build();

                    driverServiceHelper = new GoogleDriveServiceHelper(this, googleDriveService);
                    isLoggedIn.setValue(true);
                    isConnecting.setValue(true);
                    driverServiceHelper.isFolderPresent(GLOBAL_KEY, "root").addOnSuccessListener(id -> {
                        if (id.isEmpty()) {
                            driverServiceHelper.createNewFolder(GLOBAL_KEY, "root").addOnSuccessListener(rootId -> {
                                ROOT_KEY = rootId;
                                isConnecting.setValue(false);
                                setRootId("root_id", rootId, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
                            });
                        } else {
                            ROOT_KEY = id;
                            isConnecting.setValue(false);
                            setRootId("root_id", id, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
                        }
                    });
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length == 2 && grantResults[0] == -1 || grantResults[1] == -1) {
            onBackPressed();
        }
    }

    @Override
    public void onSelect(ArrayList<String> paths) {
        Set<String> pathSet = new HashSet<>();
        pathSet.addAll(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        pathSet.addAll(paths);
        Functions.setPaths("selected_paths", pathSet, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
        startActivity(new Intent(this, SelectedFolderListActivity.class));
    }
}
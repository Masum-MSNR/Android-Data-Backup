package com.cloud.apps.activities;

import static com.cloud.apps.utils.Consts.GLOBAL_KEY;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.ROOT_KEY;
import static com.cloud.apps.utils.Functions.getSize;
import static com.cloud.apps.utils.Functions.setAlarm;
import static com.cloud.apps.utils.Functions.setRootId;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.cloud.apps.R;
import com.cloud.apps.adapters.FolderBlockAdapter;
import com.cloud.apps.databinding.ActivityConnectivityBinding;
import com.cloud.apps.dialogs.SelectFolderDialog;
import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.cloud.apps.utils.Consts;
import com.cloud.apps.utils.Functions;
import com.cloud.apps.viewModels.ConnectivityActivityViewModel;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConnectivityActivity extends AppCompatActivity implements SelectFolderDialog.SelectListener {

    private static final String TAG = "ConnectivityActivity";

    ActivityConnectivityBinding binding;
    ConnectivityActivityViewModel viewModel;

    GoogleDriveServiceHelper driverServiceHelper;
    GoogleSignInClient googleSignInClient;

    ActivityResultLauncher<Intent> launcher;

    MutableLiveData<Boolean> isLoggedIn;
    MutableLiveData<Boolean> isConnecting;

    TimePickerDialog timePickerDialog;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    FolderBlockAdapter adapter;
    ArrayList<String> folderNames;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ConnectivityActivityViewModel.class);
        viewModel.init();

        getSupportActionBar().setTitle("Connectivity");

        folderNames = new ArrayList<>();
        isLoggedIn = new MutableLiveData<>(GoogleSignIn.getLastSignedInAccount(this) != null);
        isConnecting = new MutableLiveData<>(false);
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleSignInResult(result.getData()));

        preferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        editor = preferences.edit();

        adapter = new FolderBlockAdapter(this, folderNames);
        binding.blockFolderRv.setLayoutManager(new GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false));
        binding.blockFolderRv.setAdapter(adapter);

        String storage = getSize(viewModel.getFreeStorage()) + "/" + getSize(viewModel.getTotalStorage());

        binding.availableStorageTv.setText(storage);

        int percentage = viewModel.getUsedPercentage();
        binding.indicator.setProgressCompat(percentage, true);
        binding.percentTv.setText(percentage + "%");

        String tempTime = preferences.getString("time", "12:00 AM");
        binding.timeTv.setText(tempTime);

        checkLogin();

        //listeners
        Consts.LAST_SYNC_TIME.observe(this, s -> {
            binding.lastSyncTv.setText(s.isEmpty() ? "" : "Last Synced: " + s);
        });

        timePickerDialog = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
            String time = setAlarm(this, selectedHour, selectedMinute);
            binding.timeTv.setText(time);
            editor.putString("time", time);
            editor.apply();
        }, Integer.parseInt(tempTime.substring(0, 2)), Integer.parseInt(tempTime.substring(3, 5)), false);

        binding.editIbt.setOnClickListener(v -> {
            timePickerDialog.show();
        });

        viewModel.getDriveAvailAbleStorage().observe(this, s -> {
            binding.driveAvailableStorageTv.setText(s);
            binding.drivePercentTv.setText(viewModel.getDrivePercentage() + "%");
            binding.driveStorageIndicator.setProgressCompat(viewModel.getDrivePercentage(), true);
        });

        isLoggedIn.observe(this, aBoolean -> {
            binding.connectedTv.setVisibility(aBoolean ? View.VISIBLE : View.INVISIBLE);
            binding.connectToGoogleBt.setVisibility(aBoolean ? View.INVISIBLE : View.VISIBLE);
        });

        isConnecting.observe(this, aBoolean -> {
            binding.progressBar.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            binding.progressFrame.setVisibility(aBoolean ? View.VISIBLE : View.GONE);
            binding.pickFolderBt.setEnabled(!aBoolean && isLoggedIn.getValue());
            binding.selectedFoldersBt.setEnabled(!aBoolean && isLoggedIn.getValue());
        });

        binding.tryAgainBt.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tryAgainBt.setVisibility(View.GONE);
            checkRootFolder();
        });

        binding.connectToGoogleBt.setOnClickListener(v -> requestSignIn());

        binding.pickFolderBt.setOnClickListener(v -> {
            SelectFolderDialog dialog = new SelectFolderDialog(this, this);
            dialog.show(getSupportFragmentManager(), dialog.getTag());
        });

        binding.selectedFoldersBt.setOnClickListener(v -> {
            startActivity(new Intent(this, SelectedFolderListActivity.class));
        });

    }

    private void checkLogin() {
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
            new Thread(() -> {
                try {
                    viewModel.getAbout(ConnectivityActivity.this, credential.getToken());
                } catch (IOException | GoogleAuthException e) {
                    e.printStackTrace();
                }
            }).start();
            driverServiceHelper = new GoogleDriveServiceHelper(this, googleDriveService);
            isLoggedIn.setValue(true);
            isConnecting.setValue(true);

            checkRootFolder();
        }
    }

    private void checkRootFolder() {
        driverServiceHelper.isFolderPresent(GLOBAL_KEY, "root").addOnSuccessListener(id -> {
            if (id.isEmpty()) {
                driverServiceHelper.createNewFolder(GLOBAL_KEY, "root").addOnSuccessListener(rootId -> {
                    if (!rootId.isEmpty()) {
                        ROOT_KEY = rootId;
                        isConnecting.setValue(false);
                        setRootId("root_id", rootId, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
                    } else {
                        binding.tryAgainBt.setVisibility(View.VISIBLE);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.progressFrame.setVisibility(View.GONE);
                        Toast.makeText(ConnectivityActivity.this, "Something went wrong! Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                ROOT_KEY = id;
                isConnecting.setValue(false);
                setRootId("root_id", id, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
            }
        });
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
                    new Thread(() -> {
                        try {
                            viewModel.getAbout(ConnectivityActivity.this, credential.getToken());
                        } catch (IOException | GoogleAuthException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    driverServiceHelper = new GoogleDriveServiceHelper(this, googleDriveService);
                    isLoggedIn.setValue(true);
                    isConnecting.setValue(true);
                    checkRootFolder();
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
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
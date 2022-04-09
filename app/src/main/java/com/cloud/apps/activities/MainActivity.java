package com.cloud.apps.activities;

import static com.cloud.apps.utils.Consts.APP_PATH;
import static com.cloud.apps.utils.Consts.DOWNLOAD_PATH;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.previousId;
import static com.cloud.apps.utils.Consts.folderTrack;
import static com.cloud.apps.utils.Consts.mutableLogSet;
import static com.cloud.apps.utils.Functions.getAbout;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.R;
import com.cloud.apps.databinding.ActivityMainBinding;
import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.cloud.apps.fragments.BlankFragment;
import com.cloud.apps.fragments.DashboardFragment;
import com.cloud.apps.fragments.LogFragment;
import com.cloud.apps.fragments.MyCloudFragment;
import com.cloud.apps.repo.FilesRepo;
import com.cloud.apps.repo.UserRepo;
import com.cloud.apps.utils.Consts;
import com.cloud.apps.utils.PermissionHandler;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.navigation.NavigationBarView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    UserRepo userRepo;
    FilesRepo filesRepo;
    ActivityMainBinding binding;
    Fragment currentFragment;
    int navMenuId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setTitle("Dashboard");

        userRepo = UserRepo.getInstance(this);
        currentFragment = new DashboardFragment(this);
        navMenuId = R.id.dashboard;

        TreeSet<String> logSet = new TreeSet<>(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getStringSet("logs", new TreeSet<>()));

        mutableLogSet = new MutableLiveData<>(logSet);


        Consts.LAST_SYNC_TIME.setValue(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("last_sync_time", ""));

        binding.bottomNavigation.setOnItemSelectedListener(listener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, currentFragment).commit();
        checkPermission();
    }

    private void checkPermission() {
        if (!PermissionHandler.checkForPermission(this, permissions)) {
            PermissionHandler.requestPermission(this, permissions, 100);
        } else {
            isFolderExits();
            filesRepo = FilesRepo.getInstance();
            if (userRepo.getLogin().getValue()) {
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
                        getAbout(this, credential.getToken());
                        userRepo.setToken(credential.getToken());
                    } catch (IOException | GoogleAuthException e) {
                        e.printStackTrace();
                    }
                }).start();
                userRepo.setDriveServiceHelper(new GoogleDriveServiceHelper(this, googleDriveService));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length == 2 && grantResults[0] == -1 || grantResults[1] == -1) {
            onBackPressed();
        }else {
            isFolderExits();
            filesRepo = FilesRepo.getInstance();
        }
    }

    private final NavigationBarView.OnItemSelectedListener listener = item -> {
        if (item.getItemId() == R.id.dashboard) {
            currentFragment = new DashboardFragment(MainActivity.this);
            navMenuId = R.id.dashboard;
        } else if (item.getItemId() == R.id.my_cloud) {
            currentFragment = new MyCloudFragment(MainActivity.this);
            navMenuId = R.id.my_cloud;
        } else if (item.getItemId() == R.id.log) {
            currentFragment = new LogFragment(MainActivity.this);
            navMenuId = R.id.log;
        } else {
            currentFragment = new BlankFragment();
        }
        getSupportActionBar().setTitle(item.getTitle());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, currentFragment).commit();
        return true;
    };

    private void isFolderExits() {
        File file = new File(APP_PATH);
        if (!file.exists()) {
            if (file.mkdir()) {
                file = new File(DOWNLOAD_PATH);
                file.mkdir();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (navMenuId == R.id.dashboard) {
            super.onBackPressed();
        } else if (navMenuId == R.id.my_cloud) {
            if (!folderTrack.empty()) {
                previousId.setValue(folderTrack.pop());
                return;
            }
            binding.bottomNavigation.setSelectedItemId(R.id.dashboard);
            navMenuId = R.id.dashboard;
        }
    }
}
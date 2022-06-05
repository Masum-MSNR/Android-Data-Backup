package com.cloud254.apps.activities;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static com.cloud254.apps.utils.Consts.APP_PATH;
import static com.cloud254.apps.utils.Consts.DOWNLOAD_PATH;
import static com.cloud254.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud254.apps.utils.Consts.folderTrack;
import static com.cloud254.apps.utils.Consts.mutableLogSet;
import static com.cloud254.apps.utils.Consts.previousId;
import static com.cloud254.apps.utils.Functions.getAbout;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.cloud254.apps.R;
import com.cloud254.apps.databinding.ActivityMainBinding;
import com.cloud254.apps.driveApi.DriveDownloadService;
import com.cloud254.apps.driveApi.DriveService;
import com.cloud254.apps.fragments.BlankFragment;
import com.cloud254.apps.fragments.DashboardFragment;
import com.cloud254.apps.fragments.LogFragment;
import com.cloud254.apps.fragments.MyCloudFragment;
import com.cloud254.apps.repo.FilesRepo;
import com.cloud254.apps.repo.UserRepo;
import com.cloud254.apps.utils.Consts;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.navigation.NavigationBarView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
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


    ActivityResultLauncher<Intent> launcher;
    ActivityResultLauncher<Intent> launcher2;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setTitle("Dashboard");

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        });
        launcher2 = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Toast.makeText(this, "Permission Granted.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

//        if (getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getBoolean("first", true)) {
//            SplashDialog dialog = new SplashDialog();
//            dialog.setCancelable(false);
//            dialog.show(getSupportFragmentManager(), dialog.getTag());
//        }

//        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
//        intent.addCategory("android.intent.category.DEFAULT");
//        intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
//        launcher.launch(intent);

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
        if (!checkStoragePermission()) {
            requestPermission();
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
                                new NetHttpTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName(getString(R.string.app_name))
                                .build();
                new Thread(() -> {
                    try {
                        getAbout(this, credential.getToken());
                        userRepo.setToken(credential.getToken());
                        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString("token", credential.getToken());
                        editor.apply();
                    } catch (IOException | GoogleAuthException e) {
                        e.printStackTrace();
                    }
                }).start();
                userRepo.setDriveServiceHelper(new DriveService(this, googleDriveService));
                userRepo.setDriveDownloadService(new DriveDownloadService(googleDriveService));
            }
        }
    }

    private boolean checkStoragePermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int r = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
            int rr = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
            return r == PackageManager.PERMISSION_GRANTED && rr == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                launcher2.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                launcher2.launch(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length == 2 && grantResults[0] == -1 || grantResults[1] == -1) {
            onBackPressed();
        } else {
            getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit().putBoolean("first", false).apply();
            isFolderExits();
            filesRepo = FilesRepo.getInstance();
        }
    }

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
        } else {
            binding.bottomNavigation.setSelectedItemId(R.id.dashboard);
            navMenuId = R.id.dashboard;
        }
    }
}
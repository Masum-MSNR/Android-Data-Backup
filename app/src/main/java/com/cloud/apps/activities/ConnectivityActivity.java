package com.cloud.apps.activities;

import static com.cloud.apps.utils.Consts.GLOBAL_KEY;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.ROOT_KEY;
import static com.cloud.apps.utils.Functions.setRootId;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.R;
import com.cloud.apps.broadcastReceivers.TaskReceiver;
import com.cloud.apps.databinding.ActivityConnectivityBinding;
import com.cloud.apps.dialogs.SelectFolderDialog;
import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.cloud.apps.utils.Consts;
import com.cloud.apps.utils.Functions;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ConnectivityActivity extends AppCompatActivity implements SelectFolderDialog.SelectListener {

    private static final String TAG = "ConnectivityActivity";

    ActivityConnectivityBinding binding;

    GoogleDriveServiceHelper driverServiceHelper;
    GoogleSignInClient googleSignInClient;

    ActivityResultLauncher<Intent> launcher;

    MutableLiveData<Boolean> isLoggedIn;
    MutableLiveData<Boolean> isConnecting;

    Calendar calendar;
    TimePickerDialog timePickerDialog;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Connectivity");

        isLoggedIn = new MutableLiveData<>(GoogleSignIn.getLastSignedInAccount(this) != null);
        isConnecting = new MutableLiveData<>(false);

        Consts.LAST_SYNC_TIME.observe(this, s -> {
            binding.lastSyncTv.setText(s.isEmpty() ? "" : "Last Synced: " + s);
        });

        preferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        editor = preferences.edit();

        calendar = Calendar.getInstance(Locale.getDefault());
        Log.v("MILISEC1", String.valueOf(calendar.getTimeInMillis()));


        String tempTime = preferences.getString("time", "12:00 AM");

        binding.timeTv.setText(tempTime);

        timePickerDialog = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            calendar.set(Calendar.MINUTE, selectedMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
            }

            String time = DateFormat.format("hh:mm aa", calendar).toString();
            binding.timeTv.setText(time);
            editor.putString("time", time);
            editor.apply();
            Intent intent = new Intent(this, TaskReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi), pi);
//            am.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
//                    AlarmManager.INTERVAL_DAY, pi);
        }, Integer.parseInt(tempTime.substring(0, 2)), Integer.parseInt(tempTime.substring(3, 5)), false);

        binding.editIbt.setOnClickListener(v -> {
            timePickerDialog.show();
        });

        isLoggedIn.observe(this, aBoolean -> {
            binding.connectedTv.setVisibility(aBoolean ? View.VISIBLE : View.INVISIBLE);
            binding.connectToGoogleBt.setVisibility(aBoolean ? View.INVISIBLE : View.VISIBLE);
        });

        isConnecting.observe(this, aBoolean -> {
            binding.progressBar.setVisibility(aBoolean ? View.VISIBLE : View.INVISIBLE);
            binding.pickFolderBt.setEnabled(!aBoolean && isLoggedIn.getValue());
            binding.selectedFoldersBt.setEnabled(!aBoolean && isLoggedIn.getValue());
        });

        binding.tryAgainBt.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tryAgainBt.setVisibility(View.INVISIBLE);
            checkRootFolder();
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

            checkRootFolder();
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
                        binding.progressBar.setVisibility(View.INVISIBLE);
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
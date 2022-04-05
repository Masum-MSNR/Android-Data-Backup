package com.cloud.apps.broadcastReceivers;

import static android.content.Context.MODE_PRIVATE;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud.apps.utils.Consts.mutableLogSet;
import static com.cloud.apps.utils.Functions.convertedTime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.R;
import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.cloud.apps.utils.Functions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Broadcast Receiver class triggers at the set time even the off is not open.
 */

public class TaskReceiver extends BroadcastReceiver {

    GoogleDriveServiceHelper driverServiceHelper;
    SharedPreferences.Editor editor;
    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (GoogleSignIn.getLastSignedInAccount(context) == null)
            return;
        TreeSet<String> logSet = new TreeSet<>(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getStringSet("logs", new TreeSet<>()));
        mutableLogSet = new MutableLiveData<>(logSet);

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(context).getAccount());
        Drive googleDriveService =
                new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(context.getString(R.string.app_name))
                        .build();
        driverServiceHelper = new GoogleDriveServiceHelper(context, googleDriveService);

        editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        Set<String> pathSet = new HashSet<>(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        for (String path : pathSet) {
            String rootId = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("root_id", "");
            if (rootId.isEmpty())
                break;
            uploadFolder(path, rootId);
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
                                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "s");
                                    File[] fileFolders = root.listFiles();
                                    if (fileFolders != null && fileFolders.length > 0) {
                                        for (File file : fileFolders) {
                                            if (file.isDirectory()) {
                                                uploadFolder(file.getPath(), folderId);
                                            } else {
                                                driverServiceHelper.isFilePresent(file.getPath(), folderId).addOnSuccessListener(aBoolean -> {
                                                    if (!aBoolean) {
                                                        driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), folderId).addOnSuccessListener(aBoolean1 -> {
                                                            if (aBoolean1) {
                                                                int file_size = Integer.parseInt(String.valueOf(file.length()));
                                                                String bkm = "B";
                                                                if (file_size >= 1024) {
                                                                    file_size = Integer.parseInt(String.valueOf(file_size / 1024));
                                                                    bkm = "KB";
                                                                }
                                                                if (file_size >= 1024) {
                                                                    file_size = Integer.parseInt(String.valueOf(file_size / 1024));
                                                                    bkm = "MB";
                                                                }

                                                                String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                                                Functions.saveNewLog(context, lastSyncedTime + " " + file.getPath() + " (" + file_size + bkm + ")" + "s");
                                                                editor.putString("last_sync_time", lastSyncedTime);
                                                                editor.apply();
                                                                showNotification(lastSyncedTime);
                                                            }
                                                        });
                                                    } else {
                                                        String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                                        editor.putString("last_sync_time", lastSyncedTime);
                                                        editor.apply();
                                                        showNotification(lastSyncedTime);
                                                    }
                                                });
                                            }
                                        }
                                    } else {
                                        String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                        editor.putString("last_sync_time", lastSyncedTime);
                                        editor.apply();
                                        showNotification(lastSyncedTime);
                                    }
                                });
                    } else {
                        File[] fileFolders = root.listFiles();
                        if (fileFolders != null && fileFolders.length > 0) {
                            for (File file : fileFolders) {
                                if (file.isDirectory()) {
                                    uploadFolder(file.getPath(), id);
                                } else {
                                    driverServiceHelper.isFilePresent(file.getPath(), id).addOnSuccessListener(aBoolean -> {
                                        if (!aBoolean) {
                                            driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), id).addOnSuccessListener(aBoolean1 -> {
                                                if (aBoolean1) {
                                                    int file_size = Integer.parseInt(String.valueOf(file.length()));
                                                    String bkm = "B";
                                                    if (file_size >= 1024) {
                                                        file_size = Integer.parseInt(String.valueOf(file_size / 1024));
                                                        bkm = "KB";
                                                    }
                                                    if (file_size >= 1024) {
                                                        file_size = Integer.parseInt(String.valueOf(file_size / 1024));
                                                        bkm = "MB";
                                                    }

                                                    String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                                    Functions.saveNewLog(context, lastSyncedTime + " " + file.getPath() + " (" + file_size + bkm + ")" + "s");
                                                    editor.putString("last_sync_time", lastSyncedTime);
                                                    editor.apply();
                                                    showNotification(lastSyncedTime);
                                                }
                                            });
                                        } else {
                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                            editor.putString("last_sync_time", lastSyncedTime);
                                            editor.apply();
                                            showNotification(lastSyncedTime);
                                        }
                                    });
                                }
                            }
                        } else {
                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
                            editor.putString("last_sync_time", lastSyncedTime);
                            editor.apply();
                            showNotification(lastSyncedTime);
                        }
                    }
                });
    }

    private void showNotification(String lastSyncedTime) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentTitle("Last synced")
                .setContentText(lastSyncedTime)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
        m.notify(100, builder.build());
    }
}
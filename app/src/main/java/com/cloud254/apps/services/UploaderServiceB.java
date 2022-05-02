package com.cloud254.apps.services;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;

import com.cloud254.apps.R;
import com.cloud254.apps.driveApi.BGDriveService;
import com.cloud254.apps.helpers.DBHelper;
import com.cloud254.apps.models.UploadAbleFile;
import com.cloud254.apps.utils.Functions;
import com.cloud254.apps.utils.Consts;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class UploaderServiceB extends Service {

    Context context;
    BGDriveService backgroundGoogleDriveServiceHelper;
    DBHelper dbHelper;
    SharedPreferences.Editor editor;

    Queue<UploadAbleFile> queue;
    Queue<File> allFiles;


    String rootId;
    int i = 0, count = 0, lCount = 0, iMax = 500;
    long maxSize = 1073741824;
    long current = 0;
    boolean readyToStop = false;
    boolean running = true;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getApplicationContext();
        dbHelper = new DBHelper(context);
        queue = new LinkedList<>();
        allFiles = new LinkedList<>();
        TreeSet<String> logSet = new TreeSet<>(context.getSharedPreferences(Consts.MY_PREFS_NAME, MODE_PRIVATE).getStringSet("logs", new TreeSet<>()));
        Consts.mutableLogSet = new MutableLiveData<>(logSet);

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(context).getAccount());
        Drive googleDriveService =
                new Drive.Builder(
                        new NetHttpTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(context.getString(R.string.app_name))
                        .build();

        new Thread(() -> {
            try {
                Consts.token = credential.getToken();
            } catch (IOException | GoogleAuthException e) {
                e.printStackTrace();
            }
        }).start();

        backgroundGoogleDriveServiceHelper = new BGDriveService(context, googleDriveService);
        editor = context.getSharedPreferences(Consts.MY_PREFS_NAME, MODE_PRIVATE).edit();
        rootId = context.getSharedPreferences(Consts.MY_PREFS_NAME, MODE_PRIVATE).getString("root_id", "");
        Set<String> pathSet = new HashSet<>(Functions.getPaths(context.getSharedPreferences(Consts.MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));

        for (String path : pathSet) {
            totalFileCount(path);
        }

        if (pathSet.size() != 0) {
            for (String path : pathSet) {
                if (rootId.isEmpty())
                    break;
                uploadFolder(path, rootId);
            }
        } else {
            running = false;
            stopSelf();
        }
        return START_REDELIVER_INTENT;
    }

    private void uploadFolder(String path, String parentId) {
        File root = new File(path);
        String folderName = root.getName();
        backgroundGoogleDriveServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        backgroundGoogleDriveServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    File[] fileFolders = root.listFiles();
                                    if (!folderId.isEmpty()) {
                                        Functions.saveNewLog(context, Functions.convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        if (fileFolders != null && fileFolders.length > 0) {
                                            for (File file : fileFolders) {
                                                if (file.isDirectory()) {
                                                    uploadFolder(file.getPath(), folderId);
                                                } else {
                                                    if (!dbHelper.isFileUploaded(file.getPath())) {
                                                        current += file.length();
                                                        i++;
                                                        if (maxSize >= current && i <= iMax) {
                                                            queue.add(new UploadAbleFile(file.getPath(), folderId, null));
                                                        } else {
                                                            readyToStop = true;
                                                        }
                                                    }
                                                    check();
                                                }
                                            }
                                        }
                                    } else {
                                        Functions.saveNewLog(context, Functions.convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                                        int fc = fileCountInsideFolder(root.getPath());
                                        showNotification(159, "Error Occurs");
                                        Log.v("Field To Load", fc + "");
                                        if (fileFolders != null && fc > 0) {
                                            for (int j = 0; j < fc; j++) {
                                                check();
                                            }
                                        }
                                    }
                                }).addOnFailureListener(e -> {
                            Functions.saveNewLog(context, Functions.convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                            int fc = fileCountInsideFolder(root.getPath());
                            showNotification(170, "Error Occurs");
                            Log.v("Field To Load", fc + "");
                            if (root.listFiles() != null && fc > 0) {
                                for (int j = 0; j < fc; j++) {
                                    check();
                                }
                            }
                        });
                    } else {
                        File[] fileFolders = root.listFiles();
                        if (fileFolders != null && fileFolders.length > 0) {
                            for (File file : fileFolders) {
                                if (file.isDirectory()) {
                                    uploadFolder(file.getPath(), id);
                                } else {
                                    if (!dbHelper.isFileUploaded(file.getPath())) {
                                        current += file.length();
                                        i++;
                                        if (maxSize >= current && i <= iMax) {
                                            queue.add(new UploadAbleFile(file.getPath(), id, null));
                                        } else {
                                            readyToStop = true;
                                        }
                                    }
                                    check();
                                }
                            }
                        }
                    }
                }).addOnFailureListener(e -> {
            int fc = fileCountInsideFolder(root.getPath());
            showNotification(201, "Error Occurs");
            Log.v("Field To Load", fc + "");
            if (root.listFiles() != null && fc > 0) {
                for (int j = 0; j < fc; j++) {
                    check();
                }
            }
        });
    }

    private int fileCountInsideFolder(String path) {
        int i = 0;
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null)
            return 0;
        for (File f : files) {
            if (f.isDirectory()) {
                i += fileCountInsideFolder(f.getPath());
            } else
                i++;
        }
        return i;
    }

    private void check() {
        lCount++;
        if (count == lCount || readyToStop) {
            upload();
            showNotification(queue.size(), "Starting Queue");
        }

    }

    private void upload() {
        if (!queue.isEmpty()) {
            UploadAbleFile uploadAbleFile = queue.peek();
            java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
            backgroundGoogleDriveServiceHelper.isDriveSpaceAvailable(tempFile, Consts.token).addOnSuccessListener(integer -> {
                if (integer == 2) {
                    backgroundGoogleDriveServiceHelper.uploadFileToGoogleDriveV2(uploadAbleFile).addOnSuccessListener(aBoolean -> {
                        Log.v(tempFile.getName(), tempFile.length() + "");
                        queue.remove();
                        upload();
                    }).addOnFailureListener(e -> {
                        showNotification(247, "Error Occurs");
                        upload();
                    });
                } else if (integer == 4 || integer == 1) {
                    upload();
                    showNotification(252, "Error Occurs");
                } else {
                    showNotification();
                    dbHelper.close();
                    running = false;
                    stopSelf();
                }
            }).addOnFailureListener(e -> {
                upload();
                showNotification(260, "Error Occurs");
            });
        } else {
            showNotification(queue.size(), "Ending Queue");
            showNotification(Functions.convertedTime(System.currentTimeMillis()));
            dbHelper.close();
            running = false;
            stopSelf();
        }
    }

    private void showNotification(String lastSyncedTime) {
        if (!running)
            return;
        editor.putString("last_sync_time", lastSyncedTime);
        editor.apply();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Consts.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentTitle("Last synced")
                .setContentText(lastSyncedTime)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
        m.notify(100, builder.build());
    }

    private void showNotification(int size, String status) {
        if (!running)
            return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Consts.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentTitle(status)
                .setContentText(size + "")
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
        m.notify(new Random().nextInt(), builder.build());
    }

    private void showNotification() {
        if (!running)
            return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Consts.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentTitle("Google Drive Storage Full")
                .setContentText("Please make some space.")
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
        m.notify(101, builder.build());
    }

    private void totalFileCount(String path) {
        File file = new File(path);
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                totalFileCount(f.getPath());
            } else {
                count++;
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
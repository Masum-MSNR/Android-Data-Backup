package com.cloud.apps.broadcastReceivers;

import static android.content.Context.MODE_PRIVATE;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Functions.setAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.cloud.apps.services.UploaderServiceF;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Broadcast Receiver class triggers at the set time even the off is not open.
 */

public class TaskReceiver extends BroadcastReceiver {

    //    GoogleDriveServiceHelper driverServiceHelper;
//    SharedPreferences.Editor editor;
    Context context;
//    Queue<UploadAbleFile> queue;
//    String token;
//    boolean called = false;
//    boolean uploading = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        SharedPreferences preferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String time = preferences.getString("time", "09:00 PM");
        DateFormat h12 = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
        DateFormat h24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date;
        try {
            date = h12.parse(time);
            time = h24.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        setAlarm(context, Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3, 5)));

        if (GoogleSignIn.getLastSignedInAccount(context) == null)
            return;

        intent = new Intent(context, UploaderServiceF.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
//        queue = new LinkedList<>();
//        TreeSet<String> logSet = new TreeSet<>(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getStringSet("logs", new TreeSet<>()));
//        mutableLogSet = new MutableLiveData<>(logSet);
//
//        GoogleAccountCredential credential =
//                GoogleAccountCredential.usingOAuth2(
//                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
//        credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(context).getAccount());
//        Drive googleDriveService =
//                new Drive.Builder(
//                        AndroidHttp.newCompatibleTransport(),
//                        new GsonFactory(),
//                        credential)
//                        .setApplicationName(context.getString(R.string.app_name))
//                        .build();
//
//        new Thread(() -> {
//            try {
//                token = credential.getToken();
//            } catch (IOException | GoogleAuthException e) {
//                e.printStackTrace();
//            }
//        }).start();
//
//        driverServiceHelper = new GoogleDriveServiceHelper(context, googleDriveService, true);
//
//        editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
//        Set<String> pathSet = new HashSet<>(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
//        pathSet.addAll(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "c_selected_paths"));
//        for (String path : pathSet) {
//            String rootId = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("root_id", "");
//            if (rootId.isEmpty())
//                break;
//            uploadFolder(path, rootId);
//        }
    }

//    private void uploadFolder(String path, String parentId) {
//        Log.v("Called", path);
//        File root = new File(path);
//        String folderName = root.getName();
//        driverServiceHelper.isFolderPresent(folderName, parentId)
//                .addOnSuccessListener(id -> {
//                    if (id.isEmpty()) {
//                        driverServiceHelper.createNewFolder(folderName, parentId)
//                                .addOnSuccessListener(folderId -> {
//                                    if (folderId.isEmpty()) {
//                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
//                                        File[] fileFolders = root.listFiles();
//                                        if (fileFolders != null && fileFolders.length > 0) {
//                                            for (File file : fileFolders) {
//                                                if (file.isDirectory()) {
//                                                    uploadFolder(file.getPath(), folderId);
//                                                } else {
//                                                    driverServiceHelper.isFilePresent(file.getPath(), folderId).addOnSuccessListener(aBoolean -> {
//                                                        if (!aBoolean) {
//                                                            queue.add(new UploadAbleFile(file.getPath(), id, null));
//                                                            check();
//                                                        } else {
//                                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
//                                                            editor.putString("last_sync_time", lastSyncedTime);
//                                                            editor.apply();
//                                                            showNotification(lastSyncedTime);
//                                                        }
//                                                    });
//                                                }
//                                            }
//                                        } else {
//                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
//                                            editor.putString("last_sync_time", lastSyncedTime);
//                                            editor.apply();
//                                            showNotification(lastSyncedTime);
//                                        }
//                                    } else {
//                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
//                                    }
//                                });
//                    } else {
//                        File[] fileFolders = root.listFiles();
//                        if (fileFolders != null && fileFolders.length > 0) {
//                            for (File file : fileFolders) {
//                                if (file.isDirectory()) {
//                                    uploadFolder(file.getPath(), id);
//                                } else {
//                                    driverServiceHelper.isFilePresent(file.getPath(), id).addOnSuccessListener(aBoolean -> {
//                                        if (!aBoolean) {
//                                            queue.add(new UploadAbleFile(file.getPath(), id, null));
//                                            check();
//                                        } else {
//                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
//                                            editor.putString("last_sync_time", lastSyncedTime);
//                                            editor.apply();
//                                            showNotification(lastSyncedTime);
//                                        }
//                                    });
//                                }
//                            }
//                        } else {
//                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
//                            editor.putString("last_sync_time", lastSyncedTime);
//                            editor.apply();
//                            showNotification(lastSyncedTime);
//                        }
//                    }
//                });
//    }
//
//    private void check() {
//        if (!queue.isEmpty() && !uploading) {
//            uploading = true;
//            UploadAbleFile uploadAbleFile = queue.peek();
//            java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
//            driverServiceHelper.isDriveSpaceAvailable(tempFile, token).addOnSuccessListener(integer -> {
//                if (integer == 2) {
//                    driverServiceHelper.uploadFileToGoogleDriveV2(uploadAbleFile).addOnSuccessListener(aBoolean -> {
//                        Log.v("Called", uploadAbleFile.getFilePath());
//                        if (aBoolean) {
//                            queue.remove();
//                        }
//                        uploading = false;
//                        check();
//                        String lastSyncedTime = convertedTime(System.currentTimeMillis());
//                        editor.putString("last_sync_time", lastSyncedTime);
//                        editor.apply();
//                        showNotification(lastSyncedTime);
//                    }).addOnFailureListener(e -> {
//                        uploading = false;
//                        check();
//                        String lastSyncedTime = convertedTime(System.currentTimeMillis());
//                        editor.putString("last_sync_time", lastSyncedTime);
//                        editor.apply();
//                        showNotification(lastSyncedTime);
//                    });
//                } else if (integer == 4 || integer == 1) {
//                    uploading = false;
//                    check();
//                    String lastSyncedTime = convertedTime(System.currentTimeMillis());
//                    editor.putString("last_sync_time", lastSyncedTime);
//                    editor.apply();
//                    showNotification(lastSyncedTime);
//                } else {
//                    showNotification();
//                }
//            }).addOnFailureListener(e -> {
//                uploading = false;
//                check();
//            });
//        }
//    }
//
//    private void showNotification(String lastSyncedTime) {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_cloudapp)
//                .setContentTitle("Last synced")
//                .setContentText(lastSyncedTime)
//                .setOnlyAlertOnce(true)
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
//        m.notify(100, builder.build());
//    }
//
//    private void showNotification() {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_cloudapp)
//                .setContentTitle("Google Drive Storage Full")
//                .setContentText("Please make some space.")
//                .setOnlyAlertOnce(true)
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
//        m.notify(101, builder.build());
//    }

}

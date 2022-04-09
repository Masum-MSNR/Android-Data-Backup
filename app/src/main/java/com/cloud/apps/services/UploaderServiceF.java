package com.cloud.apps.services;


import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud.apps.utils.Consts.PHONE_NAME;
import static com.cloud.apps.utils.Consts.mutableLogSet;
import static com.cloud.apps.utils.Functions.convertedTime;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.R;
import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.cloud.apps.models.UploadAbleFile;
import com.cloud.apps.utils.Functions;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

public class UploaderServiceF extends Service {

    GoogleDriveServiceHelper driverServiceHelper;
    SharedPreferences.Editor editor;
    Context context;
    Queue<UploadAbleFile> queue;
    String token;
    int count = 0, lCount = 0, subFolderCount = 0;
    ArrayList<UploadAbleFile> images;
    ArrayList<UploadAbleFile> videos;
    ArrayList<UploadAbleFile> documents;
    ArrayList<UploadAbleFile> audios;
    String rootId;
    String[] subFolders = new String[]{
            PHONE_NAME + "/Images",
            PHONE_NAME + "/Videos",
            PHONE_NAME + "/Documents",
            PHONE_NAME + "/Audios",
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentText("Sending backup")
                .build();
        startForeground(69, notification);
        context = getApplicationContext();
        queue = new LinkedList<>();
        images = new ArrayList<>();
        videos = new ArrayList<>();
        documents = new ArrayList<>();
        audios = new ArrayList<>();
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

        new Thread(() -> {
            try {
                token = credential.getToken();
            } catch (IOException | GoogleAuthException e) {
                e.printStackTrace();
            }
        }).start();

        driverServiceHelper = new GoogleDriveServiceHelper(context, googleDriveService, true);
        editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        Set<String> pathSet = new HashSet<>(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        pathSet.addAll(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "c_selected_paths"));
        for (String path : pathSet) {
            totalFileCount(path);
        }
        loadImages(Environment.getExternalStorageDirectory().getPath());
        Log.v("Stage_1", "called");
        loadVideos(Environment.getExternalStorageDirectory().getPath());
        Log.v("Stage_2", "called");
        loadDocuments(Environment.getExternalStorageDirectory().getPath());
        Log.v("Stage_3", "called");
        loadAudios(Environment.getExternalStorageDirectory().getPath());
        Log.v("Stage_4", "called");
        rootId = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("root_id", "");
        Log.v("Stage_5", "called");
        if (pathSet.size() != 0) {
            for (String path : pathSet) {
                if (rootId.isEmpty())
                    break;
                uploadFolder(path, rootId);
            }
        } else {
            uploadCommon();
        }
        Log.v("Stage_6", "called");
        return START_REDELIVER_INTENT;
    }

    private void uploadFolder(String path, String parentId) {
        Log.v("path", path);
        File root = new File(path);
        String folderName = root.getName();
        driverServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        driverServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    if (!folderId.isEmpty()) {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        File[] fileFolders = root.listFiles();
                                        if (fileFolders != null && fileFolders.length > 0) {
                                            for (File file : fileFolders) {
                                                if (file.isDirectory()) {
                                                    uploadFolder(file.getPath(), folderId);
                                                } else {
                                                    driverServiceHelper.isFilePresent(file.getPath(), folderId).addOnSuccessListener(aBoolean -> {
                                                        if (!aBoolean) {
                                                            queue.add(new UploadAbleFile(file.getPath(), folderId, null));
                                                        } else {
                                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                                            editor.putString("last_sync_time", lastSyncedTime);
                                                            editor.apply();
                                                            showNotification(lastSyncedTime);
                                                        }
                                                        check();
                                                    }).addOnFailureListener(e -> check());
                                                }
                                            }
                                        } else {
                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                            editor.putString("last_sync_time", lastSyncedTime);
                                            editor.apply();
                                            showNotification(lastSyncedTime);
                                        }
                                    } else {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
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
                                            queue.add(new UploadAbleFile(file.getPath(), id, null));
                                        } else {
                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                            editor.putString("last_sync_time", lastSyncedTime);
                                            editor.apply();
                                            showNotification(lastSyncedTime);
                                        }
                                        check();
                                    }).addOnFailureListener(e -> check());
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

    private void check() {
        lCount++;
        Log.v(count + "", lCount + "");
        if (count == lCount) {
            Log.v("Ssss", "ok");
            upload();
        }

    }

    private void upload() {
        if (!queue.isEmpty()) {
            Log.v("que", queue.size() + "");
            UploadAbleFile uploadAbleFile = queue.peek();
            java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
            driverServiceHelper.isDriveSpaceAvailable(tempFile, token).addOnSuccessListener(integer -> {
                if (integer == 2) {
                    driverServiceHelper.uploadFileToGoogleDriveV2(uploadAbleFile).addOnSuccessListener(aBoolean -> {
                        queue.remove();
                        upload();
                        showNotification(convertedTime(System.currentTimeMillis()));
                    }).addOnFailureListener(e -> {
                        upload();
                        showNotification(convertedTime(System.currentTimeMillis()));
                    });
                } else if (integer == 4 || integer == 1) {
                    upload();
                    showNotification(convertedTime(System.currentTimeMillis()));
                } else {
                    showNotification();
                    stopSelf();
                }
            }).addOnFailureListener(e -> {
                upload();
                showNotification(convertedTime(System.currentTimeMillis()));
            });
        } else {
            uploadCommon();
        }
    }

    private void uploadCommon() {
        count = images.size() + videos.size() + documents.size() + audios.size();
        lCount = 0;
        queue.clear();
        for (String s : subFolders) {
            if (s.equals(PHONE_NAME + "/Images")) {
                uploadSubFolder(s, rootId, images);
            } else if (s.equals(PHONE_NAME + "/Videos")) {
                uploadSubFolder(s, rootId, videos);
            } else if (s.equals(PHONE_NAME + "/Documents")) {
                uploadSubFolder(s, rootId, documents);
            } else if (s.equals(PHONE_NAME + "/Audios")) {
                uploadSubFolder(s, rootId, audios);
            }
        }
    }

    private void uploadSubFolder(String folderName, String parentId, ArrayList<UploadAbleFile> files) {
        driverServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        driverServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    if (!folderId.isEmpty()) {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        if (files != null && files.size() > 0) {
                                            for (UploadAbleFile file : files) {
                                                driverServiceHelper.isFilePresent(file.getFilePath(), folderId).addOnSuccessListener(aBoolean -> {
                                                    if (!aBoolean) {
                                                        queue.add(new UploadAbleFile(file.getFilePath(), folderId, null));
                                                    } else {
                                                        String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                                        editor.putString("last_sync_time", lastSyncedTime);
                                                        editor.apply();
                                                        showNotification(lastSyncedTime);
                                                    }
                                                    check2();
                                                }).addOnFailureListener(e -> check2());
                                            }
                                        } else {
                                            String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                            editor.putString("last_sync_time", lastSyncedTime);
                                            editor.apply();
                                            showNotification(lastSyncedTime);
                                        }
                                    } else {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                                    }
                                });
                    } else {
                        if (files != null && files.size() > 0) {
                            for (UploadAbleFile file : files) {
                                driverServiceHelper.isFilePresent(file.getFilePath(), id).addOnSuccessListener(aBoolean -> {
                                    if (!aBoolean) {
                                        queue.add(new UploadAbleFile(file.getFilePath(), id, null));
                                    } else {
                                        String lastSyncedTime = convertedTime(System.currentTimeMillis());
                                        editor.putString("last_sync_time", lastSyncedTime);
                                        editor.apply();
                                        showNotification(lastSyncedTime);
                                    }
                                    check2();
                                }).addOnFailureListener(e -> check2());
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

    private void check2() {
        lCount++;
        Log.v(count + "", lCount + "");
        if (count == lCount) {
            Log.v("MMMM", "ok");
            upload2();
        }
    }

    private void upload2() {
        if (!queue.isEmpty()) {
            Log.v("AAA", queue.size() + "");
            UploadAbleFile uploadAbleFile = queue.peek();
            java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
            driverServiceHelper.isDriveSpaceAvailable(tempFile, token).addOnSuccessListener(integer -> {
                if (integer == 2) {
                    Log.v("Path", uploadAbleFile.getFilePath() + "  " + uploadAbleFile.getFolderId());
                    driverServiceHelper.uploadFileToGoogleDriveV2(uploadAbleFile).addOnSuccessListener(aBoolean -> {
                        queue.remove();
                        upload2();
                        showNotification(convertedTime(System.currentTimeMillis()));
                    }).addOnFailureListener(e -> {
                        upload2();
                        showNotification(convertedTime(System.currentTimeMillis()));
                    });
                } else if (integer == 4 || integer == 1) {
                    upload2();
                    showNotification(convertedTime(System.currentTimeMillis()));
                } else {
                    showNotification();
                    stopSelf();
                }
            }).addOnFailureListener(e -> {
                upload2();
                showNotification(convertedTime(System.currentTimeMillis()));
            });
        } else {
            stopSelf();
        }
    }

    private void showNotification(String lastSyncedTime) {
        editor.putString("last_sync_time", lastSyncedTime);
        editor.apply();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentTitle("Last synced")
                .setContentText(lastSyncedTime)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
        m.notify(100, builder.build());
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
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

    public void loadImages(String path) {
        Log.v("loadImages", "called");
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        int i = 0;
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                i++;
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadImages(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".jpg")
                            || file.getPath().toLowerCase().endsWith(".jpeg")
                            || file.getPath().toLowerCase().endsWith(".png")
                    )
                        images.add(makeUploadAbleFile(file));
                }
                if (i == 10)
                    break;
            }
        }
    }

    public void loadVideos(String path) {
        Log.v("loadVideos", "called");
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        int i = 0;
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                i++;
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadVideos(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".mp4")
                            || file.getPath().toLowerCase().endsWith(".mkv")
                            || file.getPath().toLowerCase().endsWith(".webm")
                            || file.getPath().toLowerCase().endsWith(".mov")
                            || file.getPath().toLowerCase().endsWith(".flv")
                            || file.getPath().toLowerCase().endsWith(".avchd")
                    )
                        videos.add(makeUploadAbleFile(file));
                }
                if (i == 10)
                    break;
            }
        }
    }

    public void loadAudios(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        int i = 0;
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                i++;
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadAudios(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".mp3")
                            || file.getPath().toLowerCase().endsWith(".m4a")
                            || file.getPath().toLowerCase().endsWith(".wav")
                    )
                        documents.add(makeUploadAbleFile(file));
                }
                if (i == 10)
                    break;
            }
        }
    }

    public void loadDocuments(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        int i = 0;
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                i++;
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadDocuments(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".pdf")
                            || file.getPath().toLowerCase().endsWith(".docx")
                            || file.getPath().toLowerCase().endsWith(".html")
                            || file.getPath().toLowerCase().endsWith(".htm")
                            || file.getPath().toLowerCase().endsWith(".xls")
                            || file.getPath().toLowerCase().endsWith(".xlsx")
                            || file.getPath().toLowerCase().endsWith(".txt")
                            || file.getPath().toLowerCase().endsWith(".ppt")
                            || file.getPath().toLowerCase().endsWith(".pptx")
                    )
                        audios.add(makeUploadAbleFile(file));
                }
                if (i == 10)
                    break;
            }
        }
    }

    private UploadAbleFile makeUploadAbleFile(File file) {
        return new UploadAbleFile(file.getPath(), file.getParent(), null);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

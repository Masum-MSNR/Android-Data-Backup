package com.cloud.apps.services;


import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud.apps.utils.Consts.PHONE_NAME;
import static com.cloud.apps.utils.Consts.mutableLogSet;
import static com.cloud.apps.utils.Consts.token;
import static com.cloud.apps.utils.Functions.convertedTime;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.R;
import com.cloud.apps.driveApi.BGDriveService;
import com.cloud.apps.helpers.DBHelper;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

public class UploaderServiceB extends Service {

    Context context;
    BGDriveService backgroundGoogleDriveServiceHelper;
    DBHelper dbHelper;
    SharedPreferences.Editor editor;

    Queue<UploadAbleFile> queue;
    Queue<File> allFiles;


    String rootId, imageId, videoId, documentId, audioId;
    String[] subFolders = new String[]{
            PHONE_NAME + "/Images",
            PHONE_NAME + "/Videos",
            PHONE_NAME + "/Documents",
            PHONE_NAME + "/Audios",
    };
    int i = 0, count = 0, lCount = 0, iMax = 500;
    int imageI = 0, videoI = 0, documentI = 0, audioI = 0;
    int ai = 0;
    long maxSize = 1073741824;
    long current = 0;
    boolean readyToStop = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new CountDownTimer(3600000, 3600) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                showNotification(convertedTime(System.currentTimeMillis()));
                dbHelper.close();
                stopSelf();
            }
        }.start();
        context = getApplicationContext();
        dbHelper = new DBHelper(context);
        queue = new LinkedList<>();
        allFiles = new LinkedList<>();
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

        backgroundGoogleDriveServiceHelper = new BGDriveService(context, googleDriveService);
        editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        rootId = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("root_id", "");
        Set<String> pathSet = new HashSet<>(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        pathSet.addAll(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "c_selected_paths"));

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
            i = 0;
            ai=(500-i)/4;
            checkImageFolder(subFolders[0], rootId);
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
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        if (fileFolders != null && fileFolders.length > 0) {
                                            for (File file : fileFolders) {
                                                if (file.isDirectory()) {
                                                    uploadFolder(file.getPath(), folderId);
                                                } else {
                                                    backgroundGoogleDriveServiceHelper.isFilePresent(file.getPath(), folderId).addOnSuccessListener(aBoolean -> {
                                                        if (!aBoolean) {
                                                            current += file.length();
                                                            i++;
                                                            if (maxSize >= current && i <= iMax) {
                                                                queue.add(new UploadAbleFile(file.getPath(), folderId, null));
                                                            } else {
                                                                readyToStop = true;
                                                            }
                                                        }
                                                        check();
                                                    }).addOnFailureListener(e -> check());
                                                }
                                            }
                                        }
                                    } else {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                                        if (fileFolders != null && fileFolders.length > 0) {
                                            for (int j = 0; j < fileFolders.length; j++) {
                                                check();
                                            }
                                        }
                                    }
                                }).addOnFailureListener(e -> {
                            Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                            File[] fileFolders = root.listFiles();
                            if (fileFolders != null && fileFolders.length > 0) {
                                for (int j = 0; j < fileFolders.length; j++) {
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
                                    backgroundGoogleDriveServiceHelper.isFilePresent(file.getPath(), id).addOnSuccessListener(aBoolean -> {
                                        if (!aBoolean) {
                                            current += file.length();
                                            i++;
                                            if (maxSize >= current && i <= iMax) {
                                                queue.add(new UploadAbleFile(file.getPath(), id, null));
                                            } else {
                                                readyToStop = true;
                                            }
                                        }
                                        check();
                                    }).addOnFailureListener(e -> check());
                                }
                            }
                        }
                    }
                }).addOnFailureListener(e->{});
    }

    private void check() {
        lCount++;
        if (count == lCount || readyToStop) {
            upload();
        }

    }

    private void upload() {
        if (!queue.isEmpty()) {
            UploadAbleFile uploadAbleFile = queue.peek();
            java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
            backgroundGoogleDriveServiceHelper.isDriveSpaceAvailable(tempFile, token).addOnSuccessListener(integer -> {
                if (integer == 2) {
                    backgroundGoogleDriveServiceHelper.uploadFileToGoogleDriveV2(uploadAbleFile).addOnSuccessListener(aBoolean -> {
                        Log.v("result", aBoolean + "");
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
                    dbHelper.close();
                    stopSelf();
                }
            }).addOnFailureListener(e -> {
                upload();
                showNotification(convertedTime(System.currentTimeMillis()));
            });
        } else {
            if (readyToStop) {
                showNotification(convertedTime(System.currentTimeMillis()));
                dbHelper.close();
                stopSelf();
            } else {
                ai=(500-i)/4;
                checkImageFolder(subFolders[0], rootId);
            }
        }
    }


    private void checkImageFolder(String folderName, String parentId) {
        backgroundGoogleDriveServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        backgroundGoogleDriveServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    if (!folderId.isEmpty()) {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        imageId = folderId;
                                    } else {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                                    }
                                    checkVideoFolder(subFolders[1], rootId);
                                }).addOnFailureListener(e -> {
                            checkVideoFolder(subFolders[1], rootId);
                        });
                    } else {
                        imageId = id;
                        checkVideoFolder(subFolders[1], rootId);
                    }
                }).addOnFailureListener(e -> {
            checkVideoFolder(subFolders[1], rootId);
        });
    }

    private void checkVideoFolder(String folderName, String parentId) {
        backgroundGoogleDriveServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        backgroundGoogleDriveServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    if (!folderId.isEmpty()) {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        videoId = folderId;
                                    } else {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                                    }
                                    checkDocumentFolder(subFolders[2], rootId);
                                }).addOnFailureListener(e -> {
                            checkDocumentFolder(subFolders[2], rootId);
                        });
                    } else {
                        videoId = id;
                        checkDocumentFolder(subFolders[2], rootId);
                    }
                }).addOnFailureListener(e -> {
            checkDocumentFolder(subFolders[2], rootId);
        });
    }

    private void checkDocumentFolder(String folderName, String parentId) {
        backgroundGoogleDriveServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        backgroundGoogleDriveServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    if (!folderId.isEmpty()) {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        documentId = folderId;
                                    } else {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                                    }
                                    checkAudioFolder(subFolders[3], rootId);
                                }).addOnFailureListener(e -> {
                            checkAudioFolder(subFolders[3], rootId);
                        });
                    } else {
                        documentId = id;
                        checkAudioFolder(subFolders[3], rootId);
                    }
                }).addOnFailureListener(e -> {
            checkAudioFolder(subFolders[3], rootId);
        });
    }

    private void checkAudioFolder(String folderName, String parentId) {
        backgroundGoogleDriveServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        backgroundGoogleDriveServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    if (!folderId.isEmpty()) {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                        audioId = folderId;
                                    } else {
                                        Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " failed to create" + "2");
                                    }
                                    loadAllFiles(Environment.getExternalStorageDirectory().getPath());
                                    getUploadAbleFiles();
                                }).addOnFailureListener(e -> {
                            loadAllFiles(Environment.getExternalStorageDirectory().getPath());
                            getUploadAbleFiles();
                        });
                    } else {
                        audioId = id;
                        loadAllFiles(Environment.getExternalStorageDirectory().getPath());
                        getUploadAbleFiles();
                    }
                }).addOnFailureListener(e -> {
            loadAllFiles(Environment.getExternalStorageDirectory().getPath());
            getUploadAbleFiles();
        });
    }

    private void loadAllFiles(String path) {
        File root = new File(path);
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    loadAllFiles(file.getPath());
                } else {
                    allFiles.add(file);
                }
            }
        }
    }

    private void getUploadAbleFiles() {
        if (!allFiles.isEmpty() && !readyToStop) {
            File currentFile = allFiles.remove();
            Log.v("count", currentFile.getName() + " " + i);
            String id = null;
            if (dbHelper.isFileUploaded(currentFile.getPath())) {
                getUploadAbleFiles();
                return;
            }

            if (imageId == null && videoId == null && documentId == null && audioId == null) {
                showNotification(convertedTime(System.currentTimeMillis()));
                dbHelper.close();
                stopSelf();
            }

            if (currentFile.getPath().toLowerCase().endsWith(".jpg")
                    || currentFile.getPath().toLowerCase().endsWith(".jpeg")
                    || currentFile.getPath().toLowerCase().endsWith(".png")
                    || currentFile.getPath().toLowerCase().endsWith(".bmp")
                    || currentFile.getPath().toLowerCase().endsWith(".gif")
                    || currentFile.getPath().toLowerCase().endsWith(".tif")
                    || currentFile.getPath().toLowerCase().endsWith(".tiff")
                    || currentFile.getPath().toLowerCase().endsWith(".svg")
                    || currentFile.getPath().toLowerCase().endsWith(".cr2")
                    || currentFile.getPath().toLowerCase().endsWith(".crw")
                    || currentFile.getPath().toLowerCase().endsWith(".nef")
                    || currentFile.getPath().toLowerCase().endsWith(".nrw")
                    || currentFile.getPath().toLowerCase().endsWith(".sr2")
                    || currentFile.getPath().toLowerCase().endsWith(".dng")
                    || currentFile.getPath().toLowerCase().endsWith(".arw")
                    || currentFile.getPath().toLowerCase().endsWith(".orf")

            ) {
                if (imageI == ai) {
                    getUploadAbleFiles();
                    return;
                }
                imageI++;
                if (imageId != null) {
                    id = imageId;
                } else {
                    getUploadAbleFiles();
                    return;
                }
            } else if (currentFile.getPath().toLowerCase().endsWith(".mp4")
                    || currentFile.getPath().toLowerCase().endsWith(".mkv")
                    || currentFile.getPath().toLowerCase().endsWith(".webm")
                    || currentFile.getPath().toLowerCase().endsWith(".3gp")
                    || currentFile.getPath().toLowerCase().endsWith(".3gpp")
                    || currentFile.getPath().toLowerCase().endsWith(".3gpp2")
                    || currentFile.getPath().toLowerCase().endsWith(".asf")
                    || currentFile.getPath().toLowerCase().endsWith(".avi")
                    || currentFile.getPath().toLowerCase().endsWith(".dv")
                    || currentFile.getPath().toLowerCase().endsWith(".m2t")
                    || currentFile.getPath().toLowerCase().endsWith(".m4v")
                    || currentFile.getPath().toLowerCase().endsWith(".mpeg")
                    || currentFile.getPath().toLowerCase().endsWith(".mpg")
                    || currentFile.getPath().toLowerCase().endsWith(".mts")
                    || currentFile.getPath().toLowerCase().endsWith(".oggthreora")
                    || currentFile.getPath().toLowerCase().endsWith(".ogv")
                    || currentFile.getPath().toLowerCase().endsWith(".rm")
                    || currentFile.getPath().toLowerCase().endsWith(".ts")
                    || currentFile.getPath().toLowerCase().endsWith(".vob")
                    || currentFile.getPath().toLowerCase().endsWith(".wmv")
                    || currentFile.getPath().toLowerCase().endsWith(".flv")
                    || currentFile.getPath().toLowerCase().endsWith(".mov")
                    || currentFile.getPath().toLowerCase().endsWith(".flv")
                    || currentFile.getPath().toLowerCase().endsWith(".avchd")
            ) {
                if (videoI == ai) {
                    getUploadAbleFiles();
                    return;
                }
                videoI++;
                if (videoId != null) {
                    id = videoId;
                } else {
                    getUploadAbleFiles();
                    return;
                }
            } else if (currentFile.getPath().toLowerCase().endsWith(".pdf")
                    || currentFile.getPath().toLowerCase().endsWith(".doc")
                    || currentFile.getPath().toLowerCase().endsWith(".docx")
                    || currentFile.getPath().toLowerCase().endsWith(".epub")
                    || currentFile.getPath().toLowerCase().endsWith(".html")
                    || currentFile.getPath().toLowerCase().endsWith(".htm")
                    || currentFile.getPath().toLowerCase().endsWith(".xls")
                    || currentFile.getPath().toLowerCase().endsWith(".xlsx")
                    || currentFile.getPath().toLowerCase().endsWith(".txt")
                    || currentFile.getPath().toLowerCase().endsWith(".csv")
                    || currentFile.getPath().toLowerCase().endsWith(".rtf")
                    || currentFile.getPath().toLowerCase().endsWith(".md")
                    || currentFile.getPath().toLowerCase().endsWith(".odt")
                    || currentFile.getPath().toLowerCase().endsWith(".ppt")
                    || currentFile.getPath().toLowerCase().endsWith(".pptx")
                    || currentFile.getPath().toLowerCase().endsWith(".pptm")
                    || currentFile.getPath().toLowerCase().endsWith(".pps")
                    || currentFile.getPath().toLowerCase().endsWith(".ppsm")
                    || currentFile.getPath().toLowerCase().endsWith(".ppsx")
                    || currentFile.getPath().toLowerCase().endsWith(".ai")
                    || currentFile.getPath().toLowerCase().endsWith(".eps")
                    || currentFile.getPath().toLowerCase().endsWith(".psd")
            ) {
                if (documentI == ai) {
                    getUploadAbleFiles();
                    return;
                }
                documentI++;
                if (documentId != null) {
                    id = documentId;
                } else {
                    getUploadAbleFiles();
                    return;
                }
            } else if (currentFile.getPath().toLowerCase().endsWith(".mp3")
                    || currentFile.getPath().toLowerCase().endsWith(".m4a")
                    || currentFile.getPath().toLowerCase().endsWith(".wav")
                    || currentFile.getPath().toLowerCase().endsWith(".wma")
                    || currentFile.getPath().toLowerCase().endsWith(".acc")
                    || currentFile.getPath().toLowerCase().endsWith(".flac")
                    || currentFile.getPath().toLowerCase().endsWith(".oga")
            ) {
                if (audioI == ai) {
                    getUploadAbleFiles();
                    return;
                }
                audioI++;
                if (audioId != null) {
                    id = audioId;
                } else {
                    getUploadAbleFiles();
                    return;
                }
            }
            if (id == null) {
                getUploadAbleFiles();
                return;
            }
            current += currentFile.length();
            i++;
            if (maxSize >= current && i <= iMax) {
                queue.add(new UploadAbleFile(currentFile.getPath(), id, null));
            } else {
                readyToStop = true;
            }
            getUploadAbleFiles();
        } else {
            upload2();
        }
    }


    private void upload2() {
        if (!queue.isEmpty()) {
            UploadAbleFile uploadAbleFile = queue.peek();
            java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
            backgroundGoogleDriveServiceHelper.isDriveSpaceAvailable(tempFile, token).addOnSuccessListener(integer -> {
                if (integer == 2) {
                    backgroundGoogleDriveServiceHelper.uploadFileToGoogleDriveV2(uploadAbleFile).addOnSuccessListener(aBoolean -> {
                        Log.v("result", aBoolean + "");
                        queue.remove();
                        upload2();
                    }).addOnFailureListener(e -> {
                        upload2();
                    });
                } else if (integer == 4 || integer == 1) {
                    upload2();
                } else {
                    showNotification();
                    dbHelper.close();
                    stopSelf();
                }
            }).addOnFailureListener(e -> {
                upload2();
            });
        } else {
            showNotification(convertedTime(System.currentTimeMillis()));
            dbHelper.close();
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

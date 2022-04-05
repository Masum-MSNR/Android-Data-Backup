package com.cloud.apps.driveApi;

import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Functions.convertedTime;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.webkit.MimeTypeMap;

import com.cloud.apps.utils.Consts;
import com.cloud.apps.utils.Functions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveServiceHelper {

    private static final String TAG = "GoogleDriveService";
    private final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    private final Drive drive;

    Context context;

    int[] count = new int[100];


    public GoogleDriveServiceHelper(Context context, Drive drive) {
        this.context = context;
        this.drive = drive;
    }

    /**
     * Checks a folder is already present in drive or not.
     */
    public Task<String> isFolderPresent(String folderName, String parentId) {
        final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(1);

        service.execute(() -> {
            FileList result = null;
            try {
                result = drive.files().list()
                        .setQ("mimeType='" + FOLDER_MIME_TYPE + "' and trashed=false and parents='" + parentId + "'")
                        .setSpaces("drive")
                        .execute();
            } catch (IOException e) {
                Functions.saveLog(Consts.out, e.toString() + "line 68");
            }
            String folderId = "";

            if (result != null) {
                for (File file : result.getFiles()) {
                    if (file.getName().equals(folderName)) {
                        folderId = file.getId();
                        Functions.saveLog(Consts.out, folderName + " already present.[Folder]" + "line 68");
                        break;
                    }
                }
            }
            String finalFolderId = folderId;

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalFolderId), 1000);
        });
        return tcs.getTask();
    }


    /**
     * Checks a file is already present in drive or not.
     */
    public Task<Boolean> isFilePresent(String filePath, String folderId) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(2);

        service.execute(() -> {
            boolean result = false;
            java.io.File tempFile = new java.io.File(filePath);
            FileList fileList = null;
            try {
                fileList = drive.files().list()
                        .setQ("mimeType!='application/vnd.google-apps.folder' and trashed=false and parents='" + folderId + "'")
                        .setFields("files(id, name, modifiedTime)")
                        .setSpaces("drive")
                        .execute();
            } catch (IOException e) {
                Functions.saveLog(Consts.out, e.toString() + "line 98");
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            if (fileList != null) {
                for (File file : fileList.getFiles()) {
                    if (file.getName().equals(tempFile.getName()) && convertedDateTime(tempFile.lastModified()).equals(file.getModifiedTime().toString())) {
                        result = true;
                        Functions.saveLog(Consts.out, file.getName() + " already present.[File]" + "line 107");
                        break;
                    }
                }
            }
            boolean finalResult = result;
            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);

        });

        return tcs.getTask();
    }

    /**
     * Creates a folder.
     */
    public Task<String> createNewFolder(String folderName, String parent) {
        final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(1);

        service.execute(() -> {
            File metadata = new File()
                    .setParents(Collections.singletonList(parent))
                    .setMimeType(FOLDER_MIME_TYPE)
                    .setName(folderName);

            File newFolder;
            String id;
            try {
                newFolder = drive.files().create(metadata).execute();
                id = newFolder.getId() != null ? newFolder.getId() : "";
            } catch (IOException e) {
                Functions.saveLog(Consts.out, e.toString() + "line 136");
                id = "";
            }

            String finalId = id;
            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalId), 1000);
        });
        return tcs.getTask();
    }

    /**
     * Uploads file to google drive.
     */
    public Task<Boolean> uploadFileToGoogleDrive(String filePath, String folderId) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(1);

        service.execute(() -> {
            java.io.File tempFile = new java.io.File(filePath);

            File fileMetadata = new File();
            fileMetadata.setName(tempFile.getName());
            fileMetadata.setParents(Collections.singletonList(folderId));
            fileMetadata.setMimeType(getMimeType(Uri.fromFile(tempFile)));
            fileMetadata.setModifiedTime(new DateTime(tempFile.lastModified()));

            FileContent mediaContent = new FileContent(getMimeType(Uri.fromFile(tempFile)), tempFile);

            File file;
            boolean result;
            try {
                file = drive.files().create(fileMetadata, mediaContent)
                        .setFields("id, name, modifiedTime")
                        .execute();
                Functions.saveLog(Consts.out, file.getId() + " uploaded successfully." + "line 170");
                result = true;
            } catch (IOException e) {
                int file_size = Integer.parseInt(String.valueOf(tempFile.length()));
                String bkm = "B";
                if (file_size >= 1024) {
                    file_size = Integer.parseInt(String.valueOf(file_size / 1024));
                    bkm = "KB";
                }
                if (file_size >= 1024) {
                    file_size = Integer.parseInt(String.valueOf(file_size / 1024));
                    bkm = "MB";
                }

                Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + file_size + bkm + ")" + "f");

                Functions.saveLog(Consts.out, e.toString() + "line 173");
                result = false;
            }

            boolean finalResult = result;
            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
        });
        return tcs.getTask();
    }

    public void uploadFolder(String path, String parentId, int fileCount, Animation animation, int p) {
        java.io.File root = new java.io.File(path);
        String folderName = root.getName();
        isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "s");
                                    uploadFolderInside(root, folderId, fileCount, animation, p);
                                })
                                .addOnFailureListener(e -> {
                                    Functions.saveLog(Consts.out, e.toString() + "line 193");
                                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is failed to create." + "f");
                                });
                    } else {
                        uploadFolderInside(root, id, fileCount, animation, p);
                    }

                })
                .addOnFailureListener(e -> Functions.saveLog(Consts.out, e.toString() + "line 199"));
    }

    private void uploadFolderInside(java.io.File root, String id, int fileCount, Animation animation, int p) {
        java.io.File[] fileFolders = root.listFiles();
        if (fileFolders != null && fileFolders.length > 0) {
            for (java.io.File file : fileFolders) {
                if (file.isDirectory()) {
                    uploadFolder(file.getPath(), id, fileCount, animation, p);
                } else {
                    isFilePresent(file.getPath(), id).addOnSuccessListener(aBoolean -> {
                        if (!aBoolean) {
                            uploadFileToGoogleDrive(file.getPath(), id).addOnSuccessListener(aBoolean1 -> {
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

                                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + file.getPath() + " (" + file_size + bkm + ")" + "s");
                                    count[p]++;
                                    terminator(fileCount, p, animation);
                                }
                            });
                        } else {
                            count[p]++;
                            terminator(fileCount, p, animation);
                        }
                    });
                }
            }
        } else {
            terminator(fileCount, p, animation);
        }
    }

    private void terminator(int fileCount, int p, Animation animation) {
        if (fileCount == count[p]) {
            animation.cancel();
            animation.reset();
            count[p] = 0;
            for (int i = 0; i < 100; i++) {
                if (count[i] != 0) {
                    return;
                }
            }
            SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit();
            Consts.LAST_SYNC_TIME.setValue(convertedTime(System.currentTimeMillis()));
            Functions.saveLog(Consts.out, "Synced successful.\n" + Consts.LAST_SYNC_TIME.getValue() + "\n");
            editor.putString("last_sync_time", Consts.LAST_SYNC_TIME.getValue());
            editor.apply();
        }
    }

    public String getMimeType(Uri uri) {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    public String convertedDateTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date(time));
    }

    public void saveLog(OutputStreamWriter out, String newLog) {
        Functions.saveLog(out, newLog);
    }

}

package com.free.filemanagerdemo.driveApi;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.webkit.MimeTypeMap;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveServiceHelper {

    private static final String TAG = "GoogleDriveService";
    private final Drive drive;
    Context context;

    private final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    Map<String, Integer> countMap;

    public GoogleDriveServiceHelper(Context context, Drive drive) {
        this.context = context;
        this.drive = drive;
        countMap = new HashMap<>();
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
                e.printStackTrace();
            }
            String folderId = "";

            for (File file : result.getFiles()) {
                if (file.getName().equals(folderName)) {
                    folderId = file.getId();
                    Log.v(TAG, folderName + " already present.[Folder]");
                    break;
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
                e.printStackTrace();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            for (File file : fileList.getFiles()) {
                if (file.getName().equals(tempFile.getName()) && convertedDateTime(tempFile.lastModified()).equals(file.getModifiedTime().toString())) {
                    result = true;
                    Log.v(TAG, file.getName() + " already present.[File]");
                    break;
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

            File newFolder = null;
            try {
                newFolder = drive.files().create(metadata).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File finalNewFolder = newFolder;
            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalNewFolder.getId()), 1000);
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

            File file = null;
            try {
                file = drive.files().create(fileMetadata, mediaContent)
                        .setFields("id, name, modifiedTime")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("File ID: " + file.getId());
            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(true), 1000);
        });
        return tcs.getTask();
    }

    public void uploadFolder(String rootPath, String path, String parentId, int fileCount, Animation animation) {
        if (!countMap.containsKey(path)) {
            countMap.put(path, 0);
        }
        java.io.File root = new java.io.File(path);
        String folderName = root.getName();
        isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    java.io.File[] fileFolders = root.listFiles();
                                    if (fileFolders != null) {
                                        for (java.io.File file : fileFolders) {
                                            if (file.isDirectory()) {
                                                uploadFolder(rootPath, file.getPath(), folderId, fileCount, animation);
                                            } else {
                                                isFilePresent(file.getPath(), folderId).addOnSuccessListener(aBoolean -> {
                                                    if (!aBoolean) {
                                                        uploadFileToGoogleDrive(file.getPath(), folderId).addOnSuccessListener(aBoolean1 -> {
                                                            if (aBoolean1) {
                                                                int tempCount = countMap.get(rootPath);
                                                                countMap.put(rootPath, ++tempCount);
                                                                if (fileCount == countMap.get(rootPath)) {
                                                                    Log.v(rootPath, fileCount + "");
                                                                    animation.cancel();
                                                                    animation.reset();
                                                                }
                                                            }
                                                        });
                                                    } else {
                                                        int tempCount = countMap.get(rootPath);
                                                        countMap.put(rootPath, ++tempCount);
                                                        if (fileCount == countMap.get(rootPath)) {
                                                            Log.v(rootPath, fileCount + "");
                                                            animation.cancel();
                                                            animation.reset();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(exception -> Log.v("e", exception.toString()));
                    } else {
                        java.io.File[] fileFolders = root.listFiles();
                        if (fileFolders != null) {
                            for (java.io.File file : fileFolders) {
                                if (file.isDirectory()) {
                                    uploadFolder(rootPath, file.getPath(), id, fileCount, animation);
                                } else {
                                    isFilePresent(file.getPath(), id).addOnSuccessListener(aBoolean -> {
                                        if (!aBoolean) {
                                            uploadFileToGoogleDrive(file.getPath(), id).addOnSuccessListener(aBoolean1 -> {
                                                if (aBoolean1) {
                                                    int tempCount = countMap.get(rootPath);
                                                    countMap.put(rootPath, ++tempCount);
                                                    if (fileCount == countMap.get(rootPath)) {
                                                        Log.v(rootPath, fileCount + "");
                                                        animation.cancel();
                                                        animation.reset();
                                                    }
                                                }
                                            });
                                        } else {
                                            int tempCount = countMap.get(rootPath);
                                            countMap.put(rootPath, ++tempCount);
                                            if (fileCount == countMap.get(rootPath)) {
                                                Log.v(rootPath, fileCount + "");
                                                animation.cancel();
                                                animation.reset();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }

                })
                .addOnFailureListener(exception -> Log.v("e", exception.toString()));
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

}

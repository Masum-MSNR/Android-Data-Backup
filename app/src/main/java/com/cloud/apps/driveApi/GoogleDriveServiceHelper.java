package com.cloud.apps.driveApi;

import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Functions.convertedTime;
import static com.cloud.apps.utils.Functions.getSize;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.webkit.MimeTypeMap;

import com.cloud.apps.models.DriveFile;
import com.cloud.apps.repo.UserRepo;
import com.cloud.apps.utils.Consts;
import com.cloud.apps.utils.Functions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    UserRepo userRepo;

    public GoogleDriveServiceHelper(Context context, Drive drive) {
        this.context = context;
        this.drive = drive;
        userRepo = UserRepo.getInstance(context);
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
            }
            String folderId = "";

            if (result != null) {
                for (File file : result.getFiles()) {
                    if (file.getName().equals(folderName)) {
                        folderId = file.getId();
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
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            if (fileList != null) {
                for (File file : fileList.getFiles()) {
                    if (file.getName().equals(tempFile.getName()) && convertedDateTime(tempFile.lastModified()).equals(file.getModifiedTime().toString())) {
                        result = true;
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
                result = true;
            } catch (IOException e) {
                Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "4");
                result = false;
            }

            boolean finalResult = result;
            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
        });
        return tcs.getTask();
    }

    /**
     * Loads data using folder id.
     */
    public Task<ArrayList<DriveFile>> getDataFromDrive(String folderId) {
        final TaskCompletionSource<ArrayList<DriveFile>> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(2);
        ArrayList<DriveFile> files = new ArrayList<>();

        service.execute(() -> {
            FileList fileList = null;
            try {
                fileList = drive.files().list()
                        .setQ("trashed=false and parents='" + folderId + "'")
                        .setFields("files(id, name, modifiedTime,mimeType)")
                        .setSpaces("drive")
                        .execute();

            } catch (IOException e) {
                Log.v("Error", "Something went wrong");
            }

            if (fileList != null) {
                for (File file : fileList.getFiles()) {
                    files.add(new DriveFile(file.getName(), file.getId(), folderDetector(file.getMimeType())));
                }
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(files), 1000);

        });

        return tcs.getTask();
    }

    private boolean folderDetector(String mimeType) {
        return mimeType.equals("application/vnd.google-apps.folder");
    }


    /**
     * Download file from the user's My Drive Folder.
     */
    public Task<Boolean> downloadFile(final java.io.File fileSaveLocation, final String fileId) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            boolean result;
            try {
                OutputStream outputStream = new FileOutputStream(fileSaveLocation);
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                result = true;
            } catch (IOException e) {
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
                                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is created successfully" + "1");
                                    uploadFolderInside(root, folderId, fileCount, animation, p);
                                })
                                .addOnFailureListener(e -> {
                                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + folderName + " is failed to create" + "2");
                                });
                    } else {
                        uploadFolderInside(root, id, fileCount, animation, p);
                    }

                })
                .addOnFailureListener(e -> {
                });
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
                                    Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + " " + file.getPath() + " (" + getSize(file.length()) + ")" + "3");
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

}

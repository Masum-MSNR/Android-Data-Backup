package com.free.filemanagerdemo.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.free.filemanagerdemo.activities.SelectedFolderListActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveServiceHelper {
    String folderId;
    private static final String TAG = "GoogleDriveService";
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    Context context;

    private final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    public GoogleDriveServiceHelper(Context context, Drive mDriveService) {
        this.context = context;
        this.mDriveService = mDriveService;
    }

    public Task<String> isFolderPresent(String folderName) {
        return Tasks.call(mExecutor, () -> {
            FileList result = mDriveService.files().list().setQ("mimeType='application/vnd.google-apps.folder' and trashed=false").execute();
            for (File file : result.getFiles()) {
                if (file.getName().equals(folderName))
                    return file.getId();
            }
            return "";
        });
    }

    public Task<Boolean> isFilePresent(String filePath, String folderName) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(2);

        service.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean result = false;
                        java.io.File tempFile = new java.io.File(filePath);
                        FileList fileList = null;
                        try {
                            fileList = mDriveService.files().list().setQ("mimeType='application/" + folderName + "' and trashed=false name='" + tempFile.getName() + "'").execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        for (File file : fileList.getFiles()) {
                            if (file.getName().equals(tempFile.getName())) {
                                result = true;
                                break;
                            }
                        }
                        boolean finalResult = result;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);

                    }
                });

        return tcs.getTask();
    }

    /**
     * Creates a Folder in the user's My Drive.
     */
    public Task<String> createFolder(String folderName) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType(FOLDER_MIME_TYPE)
                    .setName(folderName);

            File googleFolder = mDriveService.files().create(metadata).execute();
            if (googleFolder == null) {
                throw new IOException("Null result when requesting Folder creation.");
            }

            return googleFolder.getId();
        });
    }

    /**
     * Get all the file present in the user's My Drive Folder.
     */
    public Task<ArrayList<GoogleDriveFileHolder>> getFolderFileList() {

        ArrayList<GoogleDriveFileHolder> fileList = new ArrayList<>();

        if (folderId.isEmpty()) {
            Log.e(TAG, "getFolderFileList: folder id not present");
            isFolderPresent("sad").addOnSuccessListener(id -> folderId = id)
                    .addOnFailureListener(exception -> Log.e(TAG, "Couldn't create file.", exception));
        }

        return Tasks.call(mExecutor, () -> {
            FileList result = mDriveService.files().list()
                    .setQ("trashed=false and parents = '" + folderId + "' ")
                    .setSpaces("drive")
                    .execute();

            for (int i = 0; i < result.getFiles().size(); i++) {
                GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
                googleDriveFileHolder.setId(result.getFiles().get(i).getId());
                googleDriveFileHolder.setName(result.getFiles().get(i).getName());

                fileList.add(googleDriveFileHolder);
            }
            Log.e(TAG, "getFolderFileList: folderFiles: " + fileList);
            return fileList;
        });
    }


    /**
     * Upload the file to the user's My Drive Folder.
     */
//    public Task<Boolean> uploadFileToGoogleDrive(String filePath,String  folderId) {
//        return Tasks.call(mExecutor, () -> {
//
//            Log.e(TAG, "uploadFileToGoogleDrive: path: "+filePath );
//            java.io.File tempFile = new java.io.File(filePath);
//
//            File fileMetadata = new File();
//            fileMetadata.setName(tempFile.getName());
//            fileMetadata.setParents(Collections.singletonList(folderId));
//
//            File file = mDriveService.files().create(fileMetadata)
//                    .setFields("id")
//                    .execute();
//            System.out.println("File ID: " + file.getId());
//
//            return false;
//        });
//    }
    public Task<Boolean> uploadFileToGoogleDrive(String filePath, String folderId) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(1);

        service.execute(
                new Runnable() {
                    @Override
                    public void run() {

                        Log.e(TAG, "uploadFileToGoogleDrive: path: " + filePath);
                        java.io.File tempFile = new java.io.File(filePath);

                        File fileMetadata = new File();
                        fileMetadata.setName(tempFile.getName());
                        fileMetadata.setParents(Collections.singletonList(folderId));
                        fileMetadata.setMimeType(getMimeType(Uri.fromFile(tempFile)));

                        FileContent mediaContent = new FileContent(getMimeType(Uri.fromFile(tempFile)), tempFile);

                        File file = null;
                        try {
                            file = mDriveService.files().create(fileMetadata, mediaContent)
                                    .setFields("id")
                                    .execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("File ID: " + file.getId());
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(true), 1000);
                    }
                });
        return tcs.getTask();

    }

    /**
     * Download file from the user's My Drive Folder.
     */
    public Task<Boolean> downloadFile(final java.io.File fileSaveLocation, final String fileId) {
        return Tasks.call(mExecutor, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Retrieve the metadata as a File object.
                OutputStream outputStream = new FileOutputStream(fileSaveLocation);
                mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                return true;
            }
        });
    }

    /**
     * delete file from the user's My Drive Folder.
     */
    public Task<Boolean> deleteFolderFile(final String fileId) {
        return Tasks.call(mExecutor, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Retrieve the metadata as a File object.
                if (fileId != null) {
                    mDriveService.files().delete(fileId).execute();
                    return true;
                }
                return false;
            }
        });
    }

    public String getMimeType(Uri uri) {
        String mimeType = null;
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

}

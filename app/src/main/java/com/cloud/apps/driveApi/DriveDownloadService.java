package com.cloud.apps.driveApi;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cloud.apps.models.DriveFile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriveDownloadService {


    private final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private Drive drive;

    public DriveDownloadService(Drive drive) {
        this.drive = drive;
    }


    /**
     * Loads data using folder id.
     */
    public Task<ArrayList<DriveFile>> getDataFromDrive(String parentId) {
        final TaskCompletionSource<ArrayList<DriveFile>> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(2);
        ArrayList<DriveFile> files = new ArrayList<>();

        service.execute(() -> {
            FileList fileList = null;
            try {
                fileList = drive.files().list()
                        .setQ("trashed=false and parents='"+parentId+"'")
                        .setFields("files(id, name, mimeType,parents)")
                        .setSpaces("drive")
                        .setPageSize(1000)
                        .execute();

            } catch (IOException e) {
                Log.v("Error", "Something went wrong");
            }

            if (fileList != null) {
                for (File file : fileList.getFiles()) {
                    files.add(new DriveFile(file.getName(), file.getId(), file.getParents().get(0), folderDetector(file.getMimeType())));
                }
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(files), 1000);

        });

        return tcs.getTask();
    }

    private boolean folderDetector(String mimeType) {
        return mimeType.equals(FOLDER_MIME_TYPE);
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
}

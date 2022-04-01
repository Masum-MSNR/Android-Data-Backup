package com.free.filemanagerdemo.broadcastReceivers;

import static android.content.Context.MODE_PRIVATE;
import static com.free.filemanagerdemo.utils.Consts.MY_PREFS_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.free.filemanagerdemo.utils.Functions;
import com.free.filemanagerdemo.utils.GoogleDriveServiceHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestReceiver extends BroadcastReceiver {

    GoogleDriveServiceHelper driverServiceHelper;

    @Override
    public void onReceive(Context context, Intent intent) {

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(context).getAccount());
        Drive googleDriveService =
                new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName("Drive API Migration")
                        .build();
        driverServiceHelper = new GoogleDriveServiceHelper(context, googleDriveService);

        Set<String> pathSet = new HashSet<>(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        for (String path : pathSet) {
            uploadFolder(path);
        }
    }

    private void uploadFolder(String path) {
        File root = new File(path);
        String folderName = root.getName();
        driverServiceHelper.isFolderPresent(folderName)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        driverServiceHelper.createFolder(folderName)
                                .addOnSuccessListener(folderId -> {
                                    File[] fileFolders = root.listFiles();
                                    if (fileFolders != null) {
                                        for (File file : fileFolders) {
                                            if (file.isDirectory()) {
                                                uploadFolder(file.getPath());
                                            } else {
                                                driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), folderId);
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(exception -> Log.v("e", exception.toString()));
                    } else {
                        File[] fileFolders = root.listFiles();
                        if (fileFolders != null) {
                            for (File file : fileFolders) {
                                if (file.isDirectory()) {
                                    uploadFolder(file.getPath());
                                } else {
                                    driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), id);
                                }
                            }
                        }
                    }

                })
                .addOnFailureListener(exception -> Log.v("e", exception.toString()));
    }
}

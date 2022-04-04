package com.free.filemanagerdemo.broadcastReceivers;

import static android.content.Context.MODE_PRIVATE;
import static com.free.filemanagerdemo.utils.Consts.MY_PREFS_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.free.filemanagerdemo.R;
import com.free.filemanagerdemo.driveApi.GoogleDriveServiceHelper;
import com.free.filemanagerdemo.utils.Functions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**Broadcast Receiver class triggers at the set time even the off is not open.*/

public class TaskReceiver extends BroadcastReceiver {

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
                        .setApplicationName(context.getString(R.string.app_name))
                        .build();
        driverServiceHelper = new GoogleDriveServiceHelper(context, googleDriveService);

        Set<String> pathSet = new HashSet<>(Functions.getPaths(context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        for (String path : pathSet) {
            String rootId = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("root_id", "");
            if (rootId.isEmpty())
                break;
            uploadFolder(path, rootId);
        }
    }

    private void uploadFolder(String path, String parentId) {
        File root = new File(path);
        String folderName = root.getName();
        driverServiceHelper.isFolderPresent(folderName, parentId)
                .addOnSuccessListener(id -> {
                    if (id.isEmpty()) {
                        driverServiceHelper.createNewFolder(folderName, parentId)
                                .addOnSuccessListener(folderId -> {
                                    File[] fileFolders = root.listFiles();
                                    if (fileFolders != null) {
                                        for (File file : fileFolders) {
                                            if (file.isDirectory()) {
                                                uploadFolder(file.getPath(), folderId);
                                            } else {
                                                driverServiceHelper.isFilePresent(file.getPath(), folderId).addOnSuccessListener(aBoolean -> {
                                                    if (!aBoolean) {
                                                        driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), folderId);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                });
                    } else {
                        File[] fileFolders = root.listFiles();
                        if (fileFolders != null) {
                            for (File file : fileFolders) {
                                if (file.isDirectory()) {
                                    uploadFolder(file.getPath(), id);
                                } else {
                                    driverServiceHelper.isFilePresent(file.getPath(), id).addOnSuccessListener(aBoolean -> {
                                        if (!aBoolean) {
                                            driverServiceHelper.uploadFileToGoogleDrive(file.getPath(), id);
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
    }
}

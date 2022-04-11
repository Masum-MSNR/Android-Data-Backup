package com.cloud.apps.driveApi;

import static com.cloud.apps.utils.Functions.convertedTime;
import static com.cloud.apps.utils.Functions.getSize;
import static com.cloud.apps.utils.Functions.updateToken;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.cloud.apps.helpers.DBHelper;
import com.cloud.apps.models.UploadAbleFile;
import com.cloud.apps.utils.Functions;
import com.cloud.apps.utils.VolleySingleton;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BGDriveService {

    private final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private final Drive drive;
    Context context;
    DBHelper helper;


    public BGDriveService(Context context, Drive drive) {
        this.context = context;
        this.drive = drive;
        helper = new DBHelper(context);

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
            } catch (IOException ignored) {
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
            } catch (IOException ignored) {
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


    public Task<Boolean> uploadFileToGoogleDriveV2(UploadAbleFile file) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(29);

        AtomicBoolean aResult = new AtomicBoolean(false);

        service.execute(() -> {
            java.io.File tempFile = new java.io.File(file.getFilePath());
            Log.v(tempFile.getName(), tempFile.length() + "");
            File fileMetadata = new File();
            fileMetadata.setName(tempFile.getName());
            fileMetadata.setParents(Collections.singletonList(file.getFolderId()));
            fileMetadata.setMimeType(getMimeType(Uri.fromFile(tempFile)));
            fileMetadata.setModifiedTime(new DateTime(tempFile.lastModified()));

            FileContent mediaContent = new FileContent(getMimeType(Uri.fromFile(tempFile)), tempFile);

            try {
                drive.files().create(fileMetadata, mediaContent)
                        .setFields("id, name, modifiedTime")
                        .execute();
                aResult.set(true);
                String report = convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "3";
                Functions.saveNewLog(context, report);
                helper.insert(tempFile.getPath(), report);
            } catch (IOException e) {
                String report = convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "4";
                Functions.saveNewLog(context, report);
                helper.insert(tempFile.getPath(), report);
                aResult.set(false);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(aResult.get()), 1000);
        });

        return tcs.getTask();
    }

    /**
     * Checks space available for individual file
     */
    public Task<Integer> isDriveSpaceAvailable(java.io.File file, String token) {
        final TaskCompletionSource<Integer> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(69);
        String url = "https://www.googleapis.com/drive/v3/about?fields=storageQuota&access_token=" + token;
        AtomicInteger ai = new AtomicInteger(1);
        service.execute(() -> {
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    json = new JSONObject(json.getString("storageQuota"));
                    long use = Long.parseLong(json.getString("usageInDrive"));
                    long limit = Long.parseLong(json.getString("limit"));
                    if ((limit - use) > file.length()) {
                        ai.set(2);
                    } else {
                        ai.set(3);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
                ai.set(4);
                if(error.toString().toLowerCase().contains("authfailureerror")){
                   updateToken(context);
                }
                Log.e("Error", error.toString());
            });
            VolleySingleton.getInstance(context).addToRequestQueue(context, stringRequest);

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(ai.get()), 1000);
        });

        return tcs.getTask();
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        helper.close();
    }
}

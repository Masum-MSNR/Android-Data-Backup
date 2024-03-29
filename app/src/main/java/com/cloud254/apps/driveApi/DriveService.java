package com.cloud254.apps.driveApi;

import static com.cloud254.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud254.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud254.apps.utils.Consts.token;
import static com.cloud254.apps.utils.Functions.convertedTime;
import static com.cloud254.apps.utils.Functions.getAbout;
import static com.cloud254.apps.utils.Functions.getSize;
import static com.cloud254.apps.utils.Functions.updateToken;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.webkit.MimeTypeMap;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.cloud254.apps.R;
import com.cloud254.apps.helpers.DBHelper;
import com.cloud254.apps.models.RvChildDetails;
import com.cloud254.apps.models.UploadAbleFile;
import com.cloud254.apps.repo.UserRepo;
import com.cloud254.apps.utils.Consts;
import com.cloud254.apps.utils.Functions;
import com.cloud254.apps.utils.VolleySingleton;
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
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DriveService {


    private final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private final Drive drive;
    public Queue<UploadAbleFile> queue;
    Context context;
    UserRepo userRepo;
    DBHelper helper;
    Animation animation;
    SharedPreferences.Editor editor;
    int count = 0;
    boolean animationAssigned = false;


    public DriveService(Context context, Drive drive) {
        this.context = context;
        this.drive = drive;
        userRepo = UserRepo.getInstance(context);
        queue = new LinkedList<>();
        helper = new DBHelper(context);
        editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit();
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
        ExecutorService service = Executors.newFixedThreadPool(1);

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
    public Task<Boolean> uploadFileToGoogleDrive(UploadAbleFile file) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(1);

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
                getAbout(context, userRepo.getToken());
                String report = convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "3";
                Functions.saveNewLog(context, report);
                helper.insert(tempFile.getPath(), report);
            } catch (IOException e) {
                e.printStackTrace();
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
        ExecutorService service = Executors.newFixedThreadPool(1);
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
                if (error.toString().toLowerCase().contains("authfailureerror")) {
                    updateToken(context);
                }
                Log.e("Error", error.toString());
            });
            VolleySingleton.getInstance(context).addToRequestQueue(context, stringRequest);

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(ai.get()), 1000);
        });

        return tcs.getTask();
    }

    public void uploadFolder(String path, String parentId, int fileCount, Animation animation, int p) {
        if (!animationAssigned) {
            animationAssigned = true;
            this.animation = animation;
        }
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
                                    int fc = fileCountInsideFolder(root.getPath());
                                    if (root.listFiles() != null) {
                                        for (int j = 0; j < fc; j++) {
                                            check(fileCount);
                                        }
                                    }
                                });
                    } else {
                        uploadFolderInside(root, id, fileCount, animation, p);
                    }
                })
                .addOnFailureListener(e -> {
                    int fc = fileCountInsideFolder(root.getPath());
                    if (root.listFiles() != null) {
                        for (int j = 0; j < fc; j++) {
                            check(fileCount);
                        }
                    }
                });
    }

    private int fileCountInsideFolder(String path) {
        int i = 0;
        java.io.File file = new java.io.File(path);
        java.io.File[] files = file.listFiles();
        if (files == null)
            return 0;
        for (java.io.File f : files) {
            if (f.isDirectory()) {
                i += fileCountInsideFolder(f.getPath());
            } else
                i++;
        }
        return i;
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
                            queue.add(new UploadAbleFile(file.getPath(), id, new RvChildDetails(p, fileCount, animation)));
                        }
                        check(fileCount);
                    }).addOnFailureListener(e -> {
                        check(fileCount);
                    });
                }
            }
        }
    }

    private void check(int lCount) {
        count++;
        if (count == lCount) {
            upload();
        }

    }

    private void upload() {
        if (!queue.isEmpty()) {
            UploadAbleFile uploadAbleFile = queue.peek();
            java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
            isDriveSpaceAvailable(tempFile, token).addOnSuccessListener(integer -> {
                if (integer == 2) {
                    uploadFileToGoogleDrive(uploadAbleFile).addOnSuccessListener(aBoolean -> {
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
                    helper.close();
                }
            }).addOnFailureListener(e -> {
                upload();
                showNotification(convertedTime(System.currentTimeMillis()));
            });
        } else {
            animation.cancel();
            animation.reset();
            String lastSyncTime = convertedTime(System.currentTimeMillis());
            Consts.LAST_SYNC_TIME.postValue(lastSyncTime);
            showNotification(lastSyncTime);
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        helper.close();
    }
}

package com.cloud.apps.driveApi;

import static android.content.Context.MODE_PRIVATE;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud.apps.utils.Functions.convertedTime;
import static com.cloud.apps.utils.Functions.getAbout;
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

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.cloud.apps.R;
import com.cloud.apps.models.DriveFile;
import com.cloud.apps.models.RvChildDetails;
import com.cloud.apps.models.UploadAbleFile;
import com.cloud.apps.repo.UserRepo;
import com.cloud.apps.utils.Consts;
import com.cloud.apps.utils.Functions;
import com.cloud.apps.utils.VolleySingleton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

public class GoogleDriveServiceHelper {

    private static final String TAG = "GoogleDriveService";
    private final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    private final Drive drive;

    Context context;

    int[] count = new int[100];

    UserRepo userRepo;
    public MutableLiveData<Queue<UploadAbleFile>> mUploadAbleFiles;
    boolean uploading = false;


    public GoogleDriveServiceHelper(Context context, Drive drive, boolean background) {
        this.context = context;
        this.drive = drive;
        userRepo = UserRepo.getInstance(context);
        mUploadAbleFiles = new MutableLiveData<>(new LinkedList<>());
        if (background)
            return;
        mUploadAbleFiles.observe((LifecycleOwner) context, new Observer<Queue<UploadAbleFile>>() {
            @Override
            public void onChanged(Queue<UploadAbleFile> uploadAbleFiles) {
                if (!uploadAbleFiles.isEmpty() && !uploading) {
                    uploading = true;
                    UploadAbleFile uploadAbleFile = uploadAbleFiles.peek();
                    java.io.File tempFile = new java.io.File(uploadAbleFile.getFilePath());
                    isDriveSpaceAvailable(tempFile,userRepo.getToken()).addOnSuccessListener(new OnSuccessListener<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            if (integer == 2) {
                                uploadFileToGoogleDriveV1(uploadAbleFiles.peek());
                            } else if (integer == 3) {
                                showNotification();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "4");
                            uploading = false;
                            uploadAbleFiles.add(uploadAbleFiles.remove());
                            mUploadAbleFiles.postValue(uploadAbleFiles);
                        }
                    });
                }
            }
        });
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
    public Task<Boolean> uploadFileToGoogleDriveV1(UploadAbleFile file) {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ExecutorService service = Executors.newFixedThreadPool(19);

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
                count[file.getDetails().getP()]++;
                terminator(file.getDetails().getCount(), file.getDetails().getP(), file.getDetails().getAnimation());
                getAbout(context, userRepo.getToken());
                Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "3");
                uploading = false;
                Queue<UploadAbleFile> queue = new LinkedList<>(mUploadAbleFiles.getValue());
                queue.remove();
                mUploadAbleFiles.postValue(queue);
            } catch (IOException e) {
                e.printStackTrace();
                Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "4");
                aResult.set(false);
                uploading = false;
                Queue<UploadAbleFile> queue = new LinkedList<>(mUploadAbleFiles.getValue());
                queue.add(queue.remove());
                mUploadAbleFiles.postValue(queue);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(aResult.get()), 1000);
        });

        return tcs.getTask();
    }

    public Task<Boolean> uploadFileToGoogleDriveV2(UploadAbleFile file) {
        Log.v("size", "hoi");

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
                Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "3");
                String lastSyncedTime = convertedTime(System.currentTimeMillis());
                SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString("last_sync_time", lastSyncedTime);
                editor.apply();
                showNotification(lastSyncedTime);
            } catch (IOException e) {
                Functions.saveNewLog(context, convertedTime(System.currentTimeMillis()) + tempFile.getPath() + " (" + getSize(tempFile.length()) + ")" + "4");
                aResult.set(false);
                String lastSyncedTime = convertedTime(System.currentTimeMillis());
                SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString("last_sync_time", lastSyncedTime);
                editor.apply();
                showNotification(lastSyncedTime);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(aResult.get()), 1000);
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
//                        .setQ("trashed=false and parents='" + folderId + "'")
                        .setQ("trashed=false")
                        .setFields("files(id, name, mimeType,parents)")
                        .setSpaces("drive")
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

    /**
     * Checks space available for individual file
     */
    public Task<Integer> isDriveSpaceAvailable(java.io.File file,String token) {
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
                    if ((limit - use) > file.length()){
                        Log.v("asdasdasd", "out");
                        ai.set(2);
                    }
                    else{
                        Log.v("asdasdasd", "out");
                        ai.set(3);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
                ai.set(4);
                uploading = false;
                Queue<UploadAbleFile> queue = new LinkedList<>(mUploadAbleFiles.getValue());
                queue.add(queue.remove());
                mUploadAbleFiles.postValue(queue);
                Log.e("Error", error.toString());
            });
            VolleySingleton.getInstance(context).addToRequestQueue(context, stringRequest);

            new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(ai.get()), 1000);
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
                            Queue<UploadAbleFile> queue = new LinkedList<>(mUploadAbleFiles.getValue());
                            queue.add(new UploadAbleFile(file.getPath(), id, new RvChildDetails(p, fileCount, animation)));
                            mUploadAbleFiles.setValue(queue);
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
            Consts.LAST_SYNC_TIME.postValue(convertedTime(System.currentTimeMillis()));
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentTitle("Last synced")
                .setContentText(lastSyncedTime)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
        m.notify(100, builder.build());
    }
}

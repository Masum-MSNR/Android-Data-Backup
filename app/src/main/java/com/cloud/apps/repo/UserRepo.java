package com.cloud.apps.repo;

import static android.content.Context.MODE_PRIVATE;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.driveAvailAbleStorage;
import static com.cloud.apps.utils.Consts.drivePercentage;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.driveApi.DriveDownloadService;
import com.cloud.apps.driveApi.DriveService;
import com.cloud.apps.utils.Consts;
import com.google.android.gms.auth.api.signin.GoogleSignIn;


public class UserRepo {

    private static UserRepo instance;
    private MutableLiveData<Boolean> login;
    private DriveService driveServiceHelper;
    private DriveDownloadService driveDownloadService;
    private String token, rootFolderId;

    public UserRepo(Context context) {
        driveAvailAbleStorage = new MutableLiveData<>();
        drivePercentage = new MutableLiveData<>();
        login = new MutableLiveData<>(GoogleSignIn.getLastSignedInAccount(context) != null);
        rootFolderId = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("root_id", null);
    }

    public static UserRepo getInstance(Context context) {
        if (instance == null)
            instance = new UserRepo(context);
        return instance;
    }


    public DriveService getDriveServiceHelper() {
        return driveServiceHelper;
    }

    public void setDriveServiceHelper(DriveService driveServiceHelper) {
        this.driveServiceHelper = driveServiceHelper;
    }

    public DriveDownloadService getDriveDownloadService() {
        return driveDownloadService;
    }

    public void setDriveDownloadService(DriveDownloadService driveDownloadService) {
        this.driveDownloadService = driveDownloadService;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        Consts.token = token;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public void setRootFolderId(String rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    public MutableLiveData<Boolean> getLogin() {
        return login;
    }
}

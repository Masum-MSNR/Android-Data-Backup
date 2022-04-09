package com.cloud.apps.repo;

import static android.content.Context.MODE_PRIVATE;
import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.driveAvailAbleStorage;
import static com.cloud.apps.utils.Consts.drivePercentage;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;


public class UserRepo {

    private static UserRepo instance;
    private MutableLiveData<Boolean> login;
    private GoogleDriveServiceHelper driveServiceHelper;
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


    public GoogleDriveServiceHelper getDriveServiceHelper() {
        return driveServiceHelper;
    }

    public void setDriveServiceHelper(GoogleDriveServiceHelper driveServiceHelper) {
        this.driveServiceHelper = driveServiceHelper;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

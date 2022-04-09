package com.cloud.apps.viewModels;

import static com.cloud.apps.utils.Functions.getSize;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.cloud.apps.utils.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ConnectivityActivityViewModel extends ViewModel {



    public void init() {

    }

    public long getFreeStorage() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availBlocks = stat.getAvailableBlocksLong();
        long blockSize = stat.getBlockSizeLong();
        return availBlocks * blockSize;
    }

    public long getTotalStorage() {
        double size = new File("/data").getTotalSpace();

        double sizeSystem = new File("/system").getTotalSpace();

        double sizeDev = new File("/dev").getTotalSpace();

        double sizeCache = new File("/cache").getTotalSpace();

        double sizeMnt = new File("/mnt").getTotalSpace();

        double sizeVendor = new File("/vendor").getTotalSpace();

        return (long) (sizeMnt + sizeDev + sizeSystem + size + sizeVendor + sizeCache);
    }

    public int getUsedPercentage() {
        long free = getFreeStorage();
        long totalStorage = getTotalStorage();
        return (int) (((float) (totalStorage - free) / (float) totalStorage) * 100);
    }
}

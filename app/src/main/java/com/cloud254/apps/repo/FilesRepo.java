package com.cloud254.apps.repo;

import androidx.lifecycle.MutableLiveData;

import com.cloud254.apps.utils.Consts;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class FilesRepo {

    public static FilesRepo instance;
    private ArrayList<String> downloadedFileNames;

    public FilesRepo() {
        downloadedFileNames = new ArrayList<>();
        Consts.folderTrack = new Stack<>();
        Consts.previousId = new MutableLiveData<>();
        loadDownloadedFiles();
    }

    public static FilesRepo getInstance() {
        if (instance == null)
            instance = new FilesRepo();
        return instance;
    }

    public void loadDownloadedFiles() {
        downloadedFileNames.clear();
        File root = new File(Consts.DOWNLOAD_PATH);
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                downloadedFileNames.add(file.getName());
            }
        }
    }

    public ArrayList<String> getDownloadedFileNames() {
        return downloadedFileNames;
    }
}

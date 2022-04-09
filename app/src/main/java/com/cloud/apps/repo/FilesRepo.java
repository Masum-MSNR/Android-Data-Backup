package com.cloud.apps.repo;

import static com.cloud.apps.utils.Consts.DOWNLOAD_PATH;
import static com.cloud.apps.utils.Consts.previousId;
import static com.cloud.apps.utils.Consts.folderTrack;

import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class FilesRepo {

    public static FilesRepo instance;
    private ArrayList<String> downloadedFileNames;

    public FilesRepo() {
        downloadedFileNames = new ArrayList<>();
        folderTrack = new Stack<>();
        previousId = new MutableLiveData<>();
        loadDownloadedFiles();
    }

    public static FilesRepo getInstance() {
        if (instance == null)
            instance = new FilesRepo();
        return instance;
    }

    public void loadDownloadedFiles() {
        downloadedFileNames.clear();
        File root = new File(DOWNLOAD_PATH);
        File[] files = root.listFiles();
        if(files!=null){
            for (File file : files) {
                downloadedFileNames.add(file.getName());
            }
        }
    }

    public ArrayList<String> getDownloadedFileNames() {
        return downloadedFileNames;
    }
}

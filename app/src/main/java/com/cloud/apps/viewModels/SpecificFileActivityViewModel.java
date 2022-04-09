package com.cloud.apps.viewModels;

import static com.cloud.apps.utils.Functions.getSize;

import androidx.lifecycle.ViewModel;

import com.cloud.apps.models.SingleFile;

import java.io.File;
import java.util.ArrayList;

public class SpecificFileActivityViewModel extends ViewModel {

    ArrayList<SingleFile> files;

    public void init() {
        files = new ArrayList<>();
    }

    public void refresh() {
        files.clear();
    }

    public ArrayList<SingleFile> loadImages(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadImages(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".jpg")
                            || file.getPath().toLowerCase().endsWith(".jpeg")
                            || file.getPath().toLowerCase().endsWith(".png")
                    )
                        files.add(makeSingleFile(file));
                }
            }
        }
        return files;
    }

    public ArrayList<SingleFile> loadVideos(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadVideos(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".mp4")
                            || file.getPath().toLowerCase().endsWith(".mkv")
                            || file.getPath().toLowerCase().endsWith(".webm")
                            || file.getPath().toLowerCase().endsWith(".mov")
                            || file.getPath().toLowerCase().endsWith(".flv")
                            || file.getPath().toLowerCase().endsWith(".avchd")
                    )
                        files.add(makeSingleFile(file));
                }
            }
        }
        return files;
    }

    public ArrayList<SingleFile> loadAudios(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadAudios(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".mp3")
                            || file.getPath().toLowerCase().endsWith(".m4a")
                            || file.getPath().toLowerCase().endsWith(".wav")
                    )
                        files.add(makeSingleFile(file));
                }
            }
        }
        return files;
    }

    public ArrayList<SingleFile> loadDocuments(String path) {
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                if (file.isDirectory()) {
                    if (!file.getName().startsWith("."))
                        loadDocuments(file.getPath());
                } else {
                    if (file.getPath().toLowerCase().endsWith(".pdf")
                            || file.getPath().toLowerCase().endsWith(".docx")
                            || file.getPath().toLowerCase().endsWith(".html")
                            || file.getPath().toLowerCase().endsWith(".htm")
                            || file.getPath().toLowerCase().endsWith(".xls")
                            || file.getPath().toLowerCase().endsWith(".xlsx")
                            || file.getPath().toLowerCase().endsWith(".txt")
                            || file.getPath().toLowerCase().endsWith(".ppt")
                            || file.getPath().toLowerCase().endsWith(".pptx")
                    )
                        files.add(makeSingleFile(file));
                }
            }
        }
        return files;
    }

    private SingleFile makeSingleFile(File file) {
        return new SingleFile(file.getName(), file.getParent(), getSize(file.length()));
    }

}

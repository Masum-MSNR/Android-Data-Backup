package com.cloud.apps.models;

public class UploadAbleFile {
    String filePath, folderId;
    RvChildDetails details;

    public UploadAbleFile() {
    }

    public UploadAbleFile(String filePath, String folderId, RvChildDetails details) {
        this.filePath = filePath;
        this.folderId = folderId;
        this.details = details;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public RvChildDetails getDetails() {
        return details;
    }

    public void setDetails(RvChildDetails details) {
        this.details = details;
    }
}

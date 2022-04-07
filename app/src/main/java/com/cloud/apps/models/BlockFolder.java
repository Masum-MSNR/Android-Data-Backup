package com.cloud.apps.models;

public class BlockFolder {
    int folderType;
    String folderName;

    public BlockFolder() {
    }

    public BlockFolder(int folderType, String folderName) {
        this.folderType = folderType;
        this.folderName = folderName;
    }

    public int getFolderType() {
        return folderType;
    }

    public void setFolderType(int folderType) {
        this.folderType = folderType;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}

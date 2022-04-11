package com.cloud.apps.models;

public class DriveFile {
    String name, id, parent;
    boolean folder;

    public DriveFile() {
    }

    public DriveFile(String name, String id, String parent, boolean folder) {
        this.name = name;
        this.id = id;
        this.parent = parent;
        this.folder = folder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }
}

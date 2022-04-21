package com.cloud254.apps.models;

public class SingleFile {
    String name, parent, size;

    public SingleFile() {
    }

    public SingleFile(String name, String parent, String size) {
        this.name = name;
        this.parent = parent;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}

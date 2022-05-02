package com.cloud254.apps.models;

public class LoG {
    String date, details;
    int status;

    public LoG(String log) {
        this.date = log.substring(0, 21);
        this.details = log.substring(21, log.length() - 1);
        this.status = Integer.parseInt(log.substring(log.length() - 1));
    }

    public String getDate() {
        return date;
    }

    public String getDetails() {
        return details;
    }

    public int getStatus() {
        return status;
    }
}

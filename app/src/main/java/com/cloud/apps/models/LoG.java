package com.cloud.apps.models;

public class LoG {
    String date, details;
    boolean sof;

    public LoG(String log) {
        this.date = log.substring(0, 21);
        this.details = log.substring(21, log.length() - 1);
        this.sof = log.endsWith("s");
    }

    public String getDate() {
        return date;
    }

    public String getDetails() {
        return details;
    }

    public boolean isSof() {
        return sof;
    }

}

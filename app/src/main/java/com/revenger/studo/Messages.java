package com.revenger.studo;

public class Messages {
    private String message, type, time, date, name, url_file;

    public Messages() {}

    public Messages(String message, String type, String time, String date, String name, String url_file) {
        this.message = message;
        this.type = type;
        this.time = time;
        this.date = date;
        this.name = name;
        this.url_file = url_file;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl_file() {
        return url_file;
    }

    public void setUrl_file(String url_file) {
        this.url_file = url_file;
    }
}
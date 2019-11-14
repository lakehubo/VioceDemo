package com.lake.speechdemo.bean;

public class ContentData {
    private int contentType;
    private String name;

    public ContentData(String name, int contentType) {
        this.contentType = contentType;
        this.name = name;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.lake.speechdemo.bean;

public class MediaResult {
    public int type = 0;//1：乐乐回答 需要开启听写
    public String path ="";

    public MediaResult(int type, String path) {
        this.type = type;
        this.path = path;
    }

    @Override
    public String toString() {
        return "MediaResult{" +
                "type=" + type +
                ", path='" + path + '\'' +
                '}';
    }
}

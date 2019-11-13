package com.lake.speechdemo.bean;

/**
 * Created by lake on 2018/6/28.
 */

public class Content {
    public static final int PERSON = 1;
    public static final int AIROBOT = 2;
    /**
     * 1-- 人
     * 0-- 机器
     */
    public int role;
    public String content;

    /**
     * 播放语音等级
     * <p>
     * 1、语音对话
     * 2、人脸识别结果
     */
    public int range;

    public Content(int role, String content) {
        this.role = role;
        this.content = content;
    }
}

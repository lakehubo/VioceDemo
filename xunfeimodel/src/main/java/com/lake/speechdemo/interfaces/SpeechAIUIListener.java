package com.lake.speechdemo.interfaces;

/**
 * 语音各子线程-管理线程回调接口
 */
public interface SpeechAIUIListener {
    void showStatus();

    void isComplete();

    void onError();
}

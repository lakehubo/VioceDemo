package com.lake.speechdemo.view;

/**
 * Created by lake on 2018/6/27.
 */

public interface IXunfeiView {
    void showQuestionText(String content);

    void showAnswerText(String content);

    void showVolume(int level);

    void showStatus(SpeechVoiceButton.State state);
}

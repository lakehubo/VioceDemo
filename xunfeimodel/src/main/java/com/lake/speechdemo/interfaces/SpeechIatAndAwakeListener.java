package com.lake.speechdemo.interfaces;

public interface SpeechIatAndAwakeListener {
    void onVolChange(int vol);

    void onShowAwakeText();

    void showQuestionText(String text);

    void isListening();

    void isComplete();

    void noBodySay();
}

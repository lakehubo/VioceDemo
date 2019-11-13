package com.lake.speechdemo.interfaces;

import com.lake.speechdemo.bean.TTSSignal;

public interface SpeechTTSListener {
    void onAnswerText(TTSSignal ttsSignal);

    void isParsing();

    void isComplete(String text, String time);
}

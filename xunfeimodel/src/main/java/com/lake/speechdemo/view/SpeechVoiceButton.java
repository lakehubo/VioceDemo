package com.lake.speechdemo.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.lake.speechdemo.R;

public class SpeechVoiceButton extends FrameLayout {
    private State mState = State.sDefault;

    public SpeechVoiceButton(@NonNull Context context) {
        this(context, null);
    }

    public SpeechVoiceButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpeechVoiceButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.btn_speech, this);
    }

    public void showDefault() {
        mState = State.sDefault;
    }

    public void showListening() {
        mState = State.sListening;
    }

    public void showStop() {
        mState = State.sStop;
    }

    public void showDoHandle() {
        mState = State.sDoing;
    }

    public State getState() {
        return mState;
    }

    /**
     * 按钮状态
     */
    public enum State {
        sDefault, sListening, sDoing, sStop
    }
}

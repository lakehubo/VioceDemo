package com.lake.speechdemo.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.lake.speechdemo.R;
import com.lake.speechdemo.interfaces.OnSpeechBtnClickListener;
import com.lake.speechdemo.present.XunfeiPresent;


public class BaseBottomSheetView extends BottomSheetDialog implements IXunfeiView, OnSpeechBtnClickListener {
    private Context context;
    private SpeechVoiceButton speechVoiceButton;
    private AnimatedRecordingView animatedRecordingView;
    private XunfeiPresent xunfeiPresent;

    public BaseBottomSheetView(@NonNull Context context) {
        super(context);
        this.context = context;
        xunfeiPresent = new XunfeiPresent(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        initData();
    }

    private void initData() {
        setContentView(R.layout.bottom_base_layout);
        speechVoiceButton = findViewById(R.id.speech_btn);
        animatedRecordingView = findViewById(R.id.animation_view);

        speechVoiceButton.setOnClickListener(v -> {
            this.OnSpeechBtnClick(speechVoiceButton.getState());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        xunfeiPresent.bindXunfeiService(context);
    }

    @Override
    public void showQuestionText(String content) {
        Log.e("lake", "showQuestionText: " + content);
    }

    @Override
    public void showAnswerText(String content) {
        Log.e("lake", "showAnswerText: " + content);
    }

    @Override
    public void showVolume(int level) {
        animatedRecordingView.setVolume((float) level*5f);
    }

    @Override
    public void showStatus(SpeechVoiceButton.State state) {
        switch (state) {
            case sDefault:
                animatedRecordingView.stop();
                speechVoiceButton.showDefault();
                break;
            case sListening:
                speechVoiceButton.setVisibility(View.GONE);
                animatedRecordingView.start();
                speechVoiceButton.showListening();
                break;
            case sDoing:
                animatedRecordingView.loading();
                speechVoiceButton.showDoHandle();
                break;
            case sStop:
                animatedRecordingView.stop();
                speechVoiceButton.showStop();
                speechVoiceButton.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void OnSpeechBtnClick(SpeechVoiceButton.State state) {
        switch (state) {
            case sDefault:
                //直接进入听写模式
                xunfeiPresent.startIATListener();
                break;
            case sStop:
                //停止播放
                xunfeiPresent.stopVoicePlayer();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStop() {
        xunfeiPresent.unBindXunfeiService(context);
        super.onStop();
    }

    @Override
    public void setContentView(int layoutResId) {
        super.setContentView(layoutResId);
        Window window = getWindow();
        View view = window.findViewById(R.id.design_bottom_sheet);
        view.setBackgroundResource(android.R.color.transparent);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.BOTTOM);
    }

    @Override
    public void show() {
        super.show();

    }
}

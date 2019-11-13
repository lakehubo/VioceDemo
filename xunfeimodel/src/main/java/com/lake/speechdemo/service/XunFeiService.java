package com.lake.speechdemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import com.lake.speechdemo.bean.MediaResult;
import com.lake.speechdemo.bean.TTSSignal;
import com.lake.speechdemo.present.XunfeiPresent;
import com.lake.speechdemo.view.SpeechVoiceButton;


public class XunFeiService extends Service {
    public static final int CONNECT_SERVICE_SUCCESS = 1001;
    private XunfeiManager xunfeiManager;
    public static final int RECEIVE_WORD_TO_WAVE = 1002;
    public static final int RECEIVE_UST_TASK = 1003;
    public static final int START_IAT_MODE = 1004;
    public static final int STOP_AUDIO_PLAY = 1005;
    public static final int PAUSE_IAT_MODE = 1006;
    public static final int CONTINUE_IAT_MODE = 1007;

    private Messenger clientMessenger;//与其他组件通讯的msg
    private Handler mHandler = new Handler((msg) -> {
        switch (msg.what) {
            case CONNECT_SERVICE_SUCCESS: //service服务器绑定成功
                clientMessenger = msg.replyTo;
                xunfeiManager.start();
                break;
            case RECEIVE_WORD_TO_WAVE://直接合成的语音 播放完成不进行听写
                //非iat线程产生的语音tts任务，pair中，first为待合成的文字，seconde为rec_p_id或标识符
                Log.e("XunfeiModel", (String) msg.obj);
                xunfeiManager.setTTSparseWord(XunfeiManager.NO_LISTENING, (String) msg.obj,false);
                break;
            case RECEIVE_UST_TASK:
                //非iat线程产生的语音ust任务
                Log.e("lake", "RECEIVE_UST_TASK: " + (String) msg.obj);
                xunfeiManager.setAIUIparseWords((String) msg.obj);
                break;
            case START_IAT_MODE:
                //强制进入听写模式
                xunfeiManager.startIATListner();
                break;
            case STOP_AUDIO_PLAY:
                xunfeiManager.stopMediaPlay();
                break;
        }
        return false;
    });

    private Messenger serverMessenger = new Messenger(mHandler);

    @Override
    public void onCreate() {
        super.onCreate();
        //语音管理类初始化 传入回调接口
        xunfeiManager = new XunfeiManager(this, xunfeiServiceListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serverMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        xunfeiManager.stop();
        super.onDestroy();
    }

    //接收回调处理相应事务
    private XunfeiManager.XunfeiServiceListener xunfeiServiceListener = new XunfeiManager.XunfeiServiceListener() {

        @Override
        public void onVolChange(int vol) {
            Log.e("lake", "onVolChange: " + vol);
            sendMessage(vol, XunfeiPresent.VOLNUMCHANGE);
        }

        @Override
        public void sendAudioPath(TTSSignal _audio_signal) {
            //接收到结果
            Log.e("lake", "sendAudioPath: " + _audio_signal.dst_wave_path);
            MediaResult mediaResult = new MediaResult(_audio_signal.type, _audio_signal.dst_wave_path);
            xunfeiManager.playTTSAudio(mediaResult);
        }

        @Override
        public void showQuestionText(String content) {
            sendMessage(content, XunfeiPresent.SHOW_QUESTION_TEXT);
        }

        @Override
        public void showAnswerText(String content) {
            sendMessage(content, XunfeiPresent.SHOW_ANSWER_TEXT);
        }

        @Override
        public void showStatus(SpeechVoiceButton.State state) {
            sendMessage(state, XunfeiPresent.SHOW_STATUS_TEXT);
        }
    };

    //发送消息给present
    private synchronized void sendMessage(Object obj, int MESSAGE_ID) {
        Message msg = Message.obtain();
        msg.what = MESSAGE_ID;
        msg.obj = obj;
        try {
            clientMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

package com.lake.speechdemo.present;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import com.lake.speechdemo.base.BasePresent;
import com.lake.speechdemo.service.XunFeiService;
import com.lake.speechdemo.view.IXunfeiView;
import com.lake.speechdemo.view.SpeechVoiceButton;

public class XunfeiPresent extends BasePresent<IXunfeiView> {

    private IXunfeiView mXunfeiView;
    /**
     * 听写音频
     */
    public static final int VOLNUMCHANGE = 1001;
    /**
     * 问字符
     */
    public static final int SHOW_QUESTION_TEXT = 1002;
    /**
     * 答字符
     */
    public static final int SHOW_ANSWER_TEXT = 1003;
    /**
     * 显示按钮状态
     */
    public static final int SHOW_STATUS_TEXT = 1006;

    public XunfeiPresent(IXunfeiView pXunfeiView) {
        this.mXunfeiView = pXunfeiView;
    }

    private Handler mHandler = new Handler((msg) -> {
        switch (msg.what) {
            case VOLNUMCHANGE:
                mXunfeiView.showVolume((int) msg.obj);
                break;
            case SHOW_QUESTION_TEXT:
                mXunfeiView.showQuestionText((String) msg.obj);
                break;
            case SHOW_ANSWER_TEXT:
                mXunfeiView.showAnswerText((String) msg.obj);
                break;
            case SHOW_STATUS_TEXT:
                mXunfeiView.showStatus((SpeechVoiceButton.State) msg.obj);
                break;
        }
        return false;
    });


    /*-----------------  mXunfeiIntent 开始  ----------------*/

    private Intent mXunfeiIntent;
    //客户端的Messnger
    private Messenger mXunfeiClientMessenger = new Messenger(mHandler);

    //服务端传来的Messenger
    private Messenger mXunfeiServerMessenger;

    private ServiceConnection xunfeiConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("lake", "==连接xunfeiService成功");
            mXunfeiServerMessenger = new Messenger(service);
            Message message = Message.obtain();
            message.what = XunFeiService.CONNECT_SERVICE_SUCCESS;
            message.replyTo = mXunfeiClientMessenger;
            try {
                mXunfeiServerMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("lake", "==连接xunfeiService失败");
        }
    };

    public void bindXunfeiService(Context context) {
        if (mXunfeiIntent == null) {
            mXunfeiIntent = new Intent(context, XunFeiService.class);
        }
        context.startService(mXunfeiIntent);
        context.bindService(mXunfeiIntent, xunfeiConnection, Context.BIND_AUTO_CREATE);
    }

    public void unBindXunfeiService(Context context) {
        context.unbindService(xunfeiConnection);
        context.stopService(mXunfeiIntent);
    }

    /**
     * 强制进入听写模式
     */
    public void startIATListener() {
        sendMessage(null, XunFeiService.START_IAT_MODE);
    }

    /**
     * 暂停听写模式
     */
    public void pauseIATListener() {
        sendMessage(null, XunFeiService.PAUSE_IAT_MODE);
    }

    /**
     * 继续听写模式
     */
    public void continueIATListener() {
        sendMessage(null, XunFeiService.CONTINUE_IAT_MODE);
    }

    /**
     * 停止播放音频
     */
    public void stopVoicePlayer() {
        sendMessage(null, XunFeiService.STOP_AUDIO_PLAY);
    }

    /**
     * 直接播放不显示文字
     *
     * @param words
     */
    public void playTextVoice(String words) {
        sendMessage(words, XunFeiService.RECEIVE_WORD_TO_WAVE);
    }


    //发送消息给service
    private void sendMessage(Object obj, int MID) {
        Message msg = Message.obtain();
        msg.what = MID;
        msg.obj = obj;
        try {
            mXunfeiServerMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

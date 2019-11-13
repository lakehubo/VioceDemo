package com.lake.speechdemo.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.lake.speechdemo.bean.IATResult;
import com.lake.speechdemo.bean.MediaResult;
import com.lake.speechdemo.bean.TTSSignal;
import com.lake.speechdemo.bean.USTSignal;
import com.lake.speechdemo.bean.USTbean;
import com.lake.speechdemo.interfaces.MediaPlayerListener;
import com.lake.speechdemo.interfaces.SpeechAIUIListener;
import com.lake.speechdemo.interfaces.SpeechIatAndAwakeListener;
import com.lake.speechdemo.interfaces.SpeechTTSListener;
import com.lake.speechdemo.threads.AIUIThread;
import com.lake.speechdemo.threads.IatAndAwakeThread;
import com.lake.speechdemo.threads.MediaPlayerThread;
import com.lake.speechdemo.threads.TtsThread;
import com.lake.speechdemo.utils.Constants;
import com.lake.speechdemo.view.SpeechVoiceButton;

public class XunfeiManager {
    private XunfeiServiceListener serviceListener;
    private ManageTask mManageThread;
    private Context mContext;
    public static final int NO_LISTENING = 0;
    private static final int GO_ON_LISTENING = 1;

    //实现管理类
    public XunfeiManager(Context context, XunfeiServiceListener listener) {
        this.mContext = context;
        this.serviceListener = listener;
    }

    //初始化线程
    private void initManageThread() {
        if (mManageThread == null || mManageThread.getState().equals(Thread.State.TERMINATED))
            mManageThread = new ManageTask();
    }

    //开启线程
    public void start() {
        stop();
        initManageThread();
        mManageThread.start();
    }

    //关闭线程
    public void stop() {
        if (mManageThread != null) {
            mManageThread.doStop();
            mManageThread = null;
        }
    }


    //管理线程实现
    class ManageTask extends Thread {
        private volatile boolean stop = false;
        //唤醒听写线程
        private IatAndAwakeThread iatAndAwakeThread;
        //语音理解线程
        private AIUIThread aiuiThread;
        //合成线程
        private TtsThread ttsThread;
        //语音播放线程
        private MediaPlayerThread mediaPlayerThread;

        boolean pR = false;

        private boolean IatProcess() {
            if (iatAndAwakeThread == null && !iatAndAwakeThread.isOpen()) {
                return false;
            }

            int wakeup_signal_iat_tmp = iatAndAwakeThread.get_wakeup_signal_iat();
            if (wakeup_signal_iat_tmp >= 0) {
                iatAndAwakeThread.init_wakeup_signal_iat();
            }

            IATResult get_iat_result = iatAndAwakeThread.read_iat_questions_list();
            if (get_iat_result == null) {
                return false;
            }
            //发送给字母显示

            //会话完成时，才进行语义理解，不然，只作为字幕显示
            if (!get_iat_result.is_last) {
                return false;
            } else {
                //待写入
                if (!TextUtils.isEmpty(get_iat_result.iat_question_string)) {
                    setAIUIparseWords(get_iat_result.iat_question_string);
                    return true;
                }
            }
            return false;
        }

        //语义读取线程 语义解出来的，播放完成后继续监听
        private boolean AIUIProcess() {

            if (aiuiThread == null || !aiuiThread.isOpen()) {
                return false;
            }
            USTSignal ust_task_signal_tmp = aiuiThread.read_ust_result_list();
            if (ust_task_signal_tmp == null) {
                return false;
            }
            if (ust_task_signal_tmp.is_attend_task) {
                //专门给薄言做的打卡语义
            }

            if (ust_task_signal_tmp.usTbean != null) {
                USTbean usTbean = ust_task_signal_tmp.usTbean;
                if (!TextUtils.isEmpty(usTbean.text)) {
                    //塞入合成
                    if (TextUtils.isEmpty(usTbean.url)) {
                        setTTSparseWord(NO_LISTENING, usTbean.text, true);
                    }
                }
                return true;
            }
            return false;
        }

        private boolean TTSProcess() {

            if (ttsThread == null || !ttsThread.isOpen()) {
                return false;
            }

            TTSSignal tts_result_signal_tmp = ttsThread.read_tts_result_list();
            if (tts_result_signal_tmp != null && !TextUtils.isEmpty(tts_result_signal_tmp.tts_task_text)) {
                Log.e("lake", "TTSProcess: " + tts_result_signal_tmp.tts_task_text);
                serviceListener.sendAudioPath(tts_result_signal_tmp);
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            doStart();
        }

        //线程具体事务
        public void doStart() {
            try {
                if (iatAndAwakeThread == null) {
                    //初始化听写唤醒子线程
                    iatAndAwakeThread = new IatAndAwakeThread(mContext, speechIatAndAwakeListener);
                }
                //执行子线程
                iatAndAwakeThread.start();

                if (aiuiThread == null) {
                    //初始化语义理解子线程
                    aiuiThread = new AIUIThread(mContext, speechAIUIListener);
                }
                aiuiThread.start();

                if (ttsThread == null) {
                    //初始化语音合成子线程
                    ttsThread = new TtsThread(mContext, speechTTSListener);
                }
                ttsThread.start();

                if (mediaPlayerThread == null) {
                    mediaPlayerThread = new MediaPlayerThread(mediaPlayerListener);
                }
                mediaPlayerThread.start();

                while (!stop) {

                    pR = IatProcess();
                    if (!pR) {
                        sleep(50);
                    }
                    //只读取最新的问题
                    pR = AIUIProcess();
                    if (!pR) {
                        sleep(50);
                    }
                    //tts结果查询
                    pR = TTSProcess();
                    if (!pR) {
                        sleep(50);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //释放线程
        public void doStop() {
            stop = true;
            try {
                if (iatAndAwakeThread != null) {
                    iatAndAwakeThread.doStop();
                }
                iatAndAwakeThread = null;
                if (aiuiThread != null) {
                    aiuiThread.doStop();
                    aiuiThread.destroyAgent();
                }
                aiuiThread = null;
                if (ttsThread != null) {
                    ttsThread.doStop();
                }
                ttsThread = null;

                if (mediaPlayerThread != null) {
                    mediaPlayerThread.doStop();
                }
                mediaPlayerThread = null;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private MediaPlayerListener mediaPlayerListener = new MediaPlayerListener() {
            @Override
            public void isPlaying() {
                serviceListener.showStatus(SpeechVoiceButton.State.sStop);
            }

            @Override
            public void isComplete() {
                serviceListener.showStatus(SpeechVoiceButton.State.sDefault);
            }

            @Override
            public void isOpenListening() {//乐乐回答‘我在’后，打开听写
                startIATListner();
            }
        };

        //听写/唤醒子线程回调到当前管理线程接口实现
        private SpeechIatAndAwakeListener speechIatAndAwakeListener = new SpeechIatAndAwakeListener() {
            @Override
            public void showQuestionText(String text) {
                if (TextUtils.isEmpty(text)) {
                    return;
                }
                serviceListener.showQuestionText(text);
            }

            @Override
            public void onVolChange(int vol) {
                serviceListener.onVolChange(vol);
            }

            @Override
            public void onShowAwakeText() {
                //听写线程被唤醒！！！
            }

            @Override
            public void isListening() {
                //清空播放队列
                resetPlayMedia();
                serviceListener.showStatus(SpeechVoiceButton.State.sListening);
            }

            @Override
            public void isComplete() {
                serviceListener.showStatus(SpeechVoiceButton.State.sDefault);
            }

            @Override
            public void noBodySay() {

            }
        };
        //语义理解子线程回调到当前管理线程接口实现
        private SpeechAIUIListener speechAIUIListener = new SpeechAIUIListener() {
            @Override
            public void showStatus() {
                serviceListener.showStatus(SpeechVoiceButton.State.sDoing);
            }

            @Override
            public void isComplete() {
                serviceListener.showStatus(SpeechVoiceButton.State.sDefault);
            }

            @Override
            public void onError() {
                //aiui解析错误
                serviceListener.showStatus(SpeechVoiceButton.State.sDefault);
                setTTSparseWord(GO_ON_LISTENING, "抱歉，我暂时不知道该如何回答你呢！", true);
            }
        };
        //语音合成子线程回调到当前管理线程接口实现
        private SpeechTTSListener speechTTSListener = new SpeechTTSListener() {
            @Override
            public void onAnswerText(TTSSignal ttsSignal) {
                if (ttsSignal.show_text) {
                    serviceListener.showAnswerText(ttsSignal.tts_task_text);
                }
            }

            @Override
            public void isParsing() {
                serviceListener.showStatus(SpeechVoiceButton.State.sDoing);
            }

            @Override
            public void isComplete(String text, String time) {
                serviceListener.showStatus(SpeechVoiceButton.State.sDefault);
                //保存合成结果到数据库
            }
        };

        //语义解析
        public void putUSTWords(USTSignal ustSignal) {
            if (ustSignal != null) {
                aiuiThread.write_ust_task_list(ustSignal);
            }
        }

        //语音合成
        public void putTTSWords(TTSSignal ttsSignal, boolean show) {
            ttsThread.write_tts_task_list(ttsSignal);
            if (show && ttsSignal.show_text) {//显示文字
                serviceListener.showAnswerText(ttsSignal.tts_task_text);
            }
        }

        //语音播放
        public void putAudioPlayer(MediaResult mediaResult) {
            if (!TextUtils.isEmpty(mediaResult.path)) {
                mediaPlayerThread.putAudioInQueue(mediaResult);
            }
        }

        //强制进入听写模式
        public void resetIatToListener() {
            if (mediaPlayerThread != null && mediaPlayerThread.isOpen()) {
                mediaPlayerThread.resetPlayer();
            }
            if (iatAndAwakeThread != null && iatAndAwakeThread.isOpen()) {
                iatAndAwakeThread.setOutWake();
            }
        }

        //停止播放音频
        public void stopPalyVoice() {
            if (mediaPlayerThread != null && mediaPlayerThread.isOpen()) {
                mediaPlayerThread.stopPlayer();
            }
        }

        //重置播放器
        public void resetPlayMedia() {
            if (mediaPlayerThread != null && mediaPlayerThread.isOpen()) {
                mediaPlayerThread.resetPlayer();
            }
        }
    }

    //解析text语义
    public void setAIUIparseWords(String text) {
        USTSignal ustSignal = new USTSignal();
        ustSignal.ust_task_text = text;
        mManageThread.putUSTWords(ustSignal);
    }

    //合成语音
    public void setTTSparseWord(int type, String text, boolean show) {
        TTSSignal ttsSignal = new TTSSignal();
        boolean showText = false;
        ttsSignal.tts_task_text = text;
        ttsSignal.type = type;
        ttsSignal.show_text = show;
        ttsSignal.tts_time = String.valueOf(System.currentTimeMillis());
        ttsSignal.dst_wave_path = Constants.Audio + "/" + ttsSignal.tts_time + ".wav";
        mManageThread.putTTSWords(ttsSignal, showText);
    }

    //强制进入听写模式
    public void startIATListner() {
        //强行打断语音播放
        mManageThread.resetIatToListener();
    }

    //停止播放
    public void stopMediaPlay() {
        mManageThread.stopPalyVoice();
    }


    //播放合成的语音
    public void playTTSAudio(MediaResult mediaResult) {
        mManageThread.putAudioPlayer(mediaResult);
    }

    //回调接口 -》回调给service进程
    public interface XunfeiServiceListener {
        //将tts的音频合成结果，发送给音频播放器控件播放
        void sendAudioPath(TTSSignal _audio_signal);

        //音量回调
        void onVolChange(int vol);

        void showQuestionText(String content);

        void showAnswerText(String content);

        void showStatus(SpeechVoiceButton.State state);
    }
}

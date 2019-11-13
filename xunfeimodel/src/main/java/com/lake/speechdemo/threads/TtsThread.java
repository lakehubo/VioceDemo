package com.lake.speechdemo.threads;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.lake.speechdemo.bean.TTSSignal;
import com.lake.speechdemo.bean.WavePriorityLevel;
import com.lake.speechdemo.interfaces.SpeechTTSListener;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 语音合成线程
 */
public class TtsThread extends Thread {
    private static final String TAG = TtsThread.class.getName();
    private volatile boolean stop = false;
    private Context mContext;
    private static String mEngineType = SpeechConstant.TYPE_CLOUD;//在线合成 离线合成 SpeechConstant.TYPE_LOCAL
    // 语音合成对象
    private SpeechSynthesizer mTts;

    // 默认发音人
    private String voicer = "xiaoyan";
    private TTSSignal ttsSignal;
    private boolean isTTS = false;
    private SpeechTTSListener speechTTSListener;
    private BlockingQueue<TTSSignal> tts_task_list = new LinkedBlockingQueue<>(20);
    private BlockingQueue<TTSSignal> tts_result_list = new LinkedBlockingQueue<>(20);

    public synchronized void write_tts_task_list(TTSSignal _tts_task_signal) {
        if (_tts_task_signal != null) {
            tts_task_list.offer(_tts_task_signal);
        }
    }

    private synchronized TTSSignal read_tts_task_list() {
        int size_tmp = tts_task_list.size();
        if (size_tmp == 0) {
            return null;
        }
        TTSSignal tts_task_signal_tmp = null;
        //只读取最新的问题//除非有first标记，会有后续到来
        while (!tts_task_list.isEmpty()) {
            try {
                tts_task_signal_tmp = tts_task_list.take();
                if (tts_task_signal_tmp.wave_priority_level == WavePriorityLevel.WELCOME_LEVEL_FIRST) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return tts_task_signal_tmp;
    }

    private synchronized void write_tts_result_list(TTSSignal _tts_result_signal) {
        if (_tts_result_signal != null) {
            tts_result_list.offer(_tts_result_signal);
        }
    }

    public boolean isOpen() {
        return !stop;
    }

    public synchronized TTSSignal read_tts_result_list() {
        int size_tmp = tts_result_list.size();
        if (size_tmp == 0) {
            return null;
        }
        TTSSignal tts_result_list_tmp = null;
        //只读取最新的问题//除非有first标记，会有后续到来
        while (!tts_result_list.isEmpty()) {
            try {
                tts_result_list_tmp = tts_result_list.take();
                if (tts_result_list_tmp.wave_priority_level == WavePriorityLevel.WELCOME_LEVEL_FIRST) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return tts_result_list_tmp;
    }

    public TtsThread(Context context, SpeechTTSListener listener) {
        // 初始化合成对象
        this.mContext = context;
        mTts = SpeechSynthesizer.createSynthesizer(mContext, (code) -> {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "初始化失败,错误码：" + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        });
        //初始化合成参数
        setParam();
        speechTTSListener = listener;
    }

    @Override
    public void run() {//线程执行内容
        while (!stop) {
            try {
                //避免频繁消耗cpu 先睡个觉
                if (isTTS) {
                    sleep(160);
                    continue;
                }
                TTSSignal tts_task_signal_tmp = read_tts_task_list();
                if (tts_task_signal_tmp != null && !TextUtils.isEmpty(tts_task_signal_tmp.tts_task_text)) {
                    isTTS = true;
                    ttsSignal = tts_task_signal_tmp;
                    File file = new File(tts_task_signal_tmp.dst_wave_path);
                    if (file.exists() && file.isFile() && file.length() > 0) {//已经存在 直接塞入结果
                        write_tts_result_list(tts_task_signal_tmp);
                        isTTS = false;
                        continue;
                    }
                    Log.e(TAG, "run: 合成的文字=" + tts_task_signal_tmp.tts_task_text);
                    //tts_task_text：合成的文字
                    //dst_wave_path：合成的目标文件名（全路径）
                    //int ret = tts_offline(tts_task_signal_tmp.tts_task_text.c_str(), NULL, tts_task_signal_tmp.dst_wave_path.c_str()/*, conversition_path_full_ini.c_str()*/);
//                    int ret = tts_online(tts_task_signal_tmp.tts_task_text, NULL, tts_task_signal_tmp.dst_wave_path.c_str()/*, conversition_path_full_ini.c_str()*/);
                    int code = mTts.synthesizeToUri(tts_task_signal_tmp.tts_task_text, tts_task_signal_tmp.dst_wave_path, mTtsListener);
                    if (code != ErrorCode.SUCCESS) {
                        Log.e(TAG, "语音合成失败,错误码: " + code);
                        isTTS = false;
                    }
                } else {
                    sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void doStop() {
        stop = true;
    }

    /**
     * 默认参数设置
     *
     * @return
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "50");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "50");
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
            /**
             * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
             * 开发者如需自定义参数，请参考在线合成参数设置
             */
        }
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        //mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Constants.Audio + "/tts.pcm");
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            Log.e(TAG, "onSpeakBegin:开始播放");
        }

        @Override
        public void onSpeakPaused() {
            Log.e(TAG, "onSpeakPaused:暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            Log.e(TAG, "onSpeakResumed:继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            speechTTSListener.isParsing();
            // 合成进度
            Log.e(TAG, "onBufferProgress:percent=" + percent + ",beginPos=" + beginPos + ",endPos=" + endPos + ",info=" + info);
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            Log.e(TAG, "onSpeakProgress:percent=" + percent + ",beginPos=" + beginPos + ",endPos=" + endPos);
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                Log.e(TAG, "onCompleted:播放完成");
                //写入tts_result_list
                Log.e(TAG, "onCompleted: " + ttsSignal.dst_wave_path);
                write_tts_result_list(ttsSignal);
                speechTTSListener.isComplete(ttsSignal.tts_task_text, ttsSignal.tts_time);
                speechTTSListener.onAnswerText(ttsSignal);
            } else if (error != null) {
                Log.e(TAG, "onCompleted:error" + error.getPlainDescription(true));
            }
            isTTS = false;
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
                byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
//                Log.e("MscSpeechLog", "buf is =" + buf);
            }
        }
    };
}

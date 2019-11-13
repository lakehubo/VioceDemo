package com.lake.speechdemo.threads;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.lake.speechdemo.bean.IATResult;
import com.lake.speechdemo.bean.IATResults;
import com.lake.speechdemo.interfaces.SpeechIatAndAwakeListener;
import com.lake.speechdemo.utils.Constants;
import com.lake.speechdemo.utils.ParseIATQuestions;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 听写/唤醒 线程
 */
public class IatAndAwakeThread extends Thread {
    private static final String TAG = IatAndAwakeThread.class.getName();
    private volatile boolean stop = false;
    private boolean isListening = true;
    private Context mContext;

    public boolean isOpen() {
        return !stop;
    }

    // 语音唤醒对象
    private VoiceWakeuper mIvw;
    // 唤醒结果内容
    private String resultString;

    // 设置门限值 ： 门限值越低越容易被唤醒
    private TextView tvThresh;
    private SeekBar seekbarThresh;
    private final static int MAX = 3000;
    private final static int MIN = 0;
    private int curThresh = 1450;
    private String threshStr = "门限值：";
    private String keep_alive = "1";
    private String ivwNetMode = "0";

    //chenchen
    private int wakeup_signal_to_outside = -1;//0 中文唤醒；1 英文唤醒；
    private int wakeup_signal_from_outside = -1;//比如通过屏幕上的按钮唤醒
    private int ch_en_from_outside = -1;//比如通过屏幕上的中英文按钮切换//暂时没使用，使用的话在run中使用

    private int speech_volume_to_outside = -1;
    private int speech_state_to_outside;

    //可能需要写接口，这样
    private int IAT_AWAKE_STATUS = 0;//听写/唤醒状态  0：监听唤醒词状态 1：已唤醒进入听写状态
    private int wakeup_signal_inside_use = -1;

    private List<IATResults> iatResultsList = new ArrayList<>();

    //外部get后，init
    public int get_wakeup_signal_iat() {
        return wakeup_signal_to_outside;
    }

    public void init_wakeup_signal_iat() {
        wakeup_signal_to_outside = -1;
    }

    public int get_speech_volume_iat() {
        return speech_volume_to_outside;
    }

    public int get_speech_state_iat() {
        return speech_state_to_outside;
    }

    public void set_ch_en_from_outside(int _ch_en) {
        ch_en_from_outside = _ch_en;
    }
    //chenchen

    // 语音听写对象
    private IATResult iatResultParts;
    private IATResult iat_question_read_tmp;
    private SpeechRecognizer mIat;
    private SpeechIatAndAwakeListener speechIatAndAwakeListener;
    // 引擎类型
    private boolean mTranslateEnable = false;
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private BlockingQueue<IATResult> iat_questions_list = new LinkedBlockingQueue<>(20);

    private synchronized void write_iat_questions_list(IATResult iat_question_parts) {
        if (iat_question_parts != null) {
            iat_questions_list.offer(iat_question_parts);
        }
    }

    public synchronized IATResult read_iat_questions_list() {
        int size_tmp = iat_questions_list.size();
        if (size_tmp <= 0) {
            return null;
        }

        //只读取最新的问题
        while (!iat_questions_list.isEmpty()) {
            try {
                return iat_questions_list.take();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return null;
    }

    public IatAndAwakeThread(Context context, SpeechIatAndAwakeListener listener) {
        this.mContext = context;
        //初始化听写对象
        mIat = SpeechRecognizer.createRecognizer(context, (code) -> {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "初始化失败，错误码：" + code);
            }
        });
        // 初始化唤醒对象
        mIvw = VoiceWakeuper.createWakeuper(mContext, null);
        speechIatAndAwakeListener = listener;
        initData();
    }

    private void initData() {
        iatResultParts = new IATResult();
        iat_question_read_tmp = new IATResult();
    }

    /**
     * 唤醒参数设置
     *
     * @return
     */
    public void setWakeupParam() {
        //setRadioEnable(false);
        resultString = "";
        //textView.setText(resultString);
        // 清空参数
        mIvw.setParameter(SpeechConstant.PARAMS, null);
        // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
        mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
        // 设置唤醒模式
        mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
        // 设置持续进行唤醒
        mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
        // 设置闭环优化网络模式
        mIvw.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
        // 设置唤醒资源路径
        mIvw.setParameter(SpeechConstant.IVW_RES_PATH, getResource());
        // 设置唤醒录音保存路径，保存最近一分钟的音频
        //mIvw.setParameter( SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath()+"/msc/ivw.wav" );
        //mIvw.setParameter( SpeechConstant.AUDIO_FORMAT, "wav" );
        // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
//        mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );
    }

    /**
     * 听写参数设置
     *
     * @return
     */
    public void setIatParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");//mandarin：中文cantonese：粤语language为英文时，可以不用设置此参数

        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        //mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));
        mIat.setParameter(SpeechConstant.VAD_BOS, "3000");//

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        //mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        //mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
//        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

    public void setOutWake() {
        wakeup_signal_from_outside = 0;
    }

    @Override
    public void run() {//线程执行内容
        //非空判断，防止因空指针使程序崩溃
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            setWakeupParam();
            // 启动唤醒
            mIvw.startListening(mWakeuperListener);
        } else {
            Log.e(TAG, "唤醒未初始化");
        }

        // 移动数据分析，收集开始听写事件
        if (null != mIat) {
            //FlowerCollector.onEvent(IatDemo.this, "iat_recognize");
            iatResultParts.clear();
            // 设置参数
            setIatParam();
            // 不显示听写对话框
        } else {
            Log.e(TAG, "听写未初始化");
        }


        while (!stop) {
            try {
                //避免频繁消耗cpu 先睡个觉
                if (!isListening) {
                    sleep(50);
                    continue;
                }
                //do somethings
                if (wakeup_signal_inside_use >= 0 || wakeup_signal_from_outside >= 0) {//唤醒词已捕获
                    if (wakeup_signal_inside_use >= 0) {
                        //目前wakeup_signal_inside_use强制为中文
                        if (wakeup_signal_inside_use == 0) {
                            //sr_change_session_begin_params_iat(sr, SESSION_BEGIN_PARAMS_IAT_RECORD_CN);
                            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
                            mIat.setParameter(SpeechConstant.ACCENT, "mandarin");//mandarin：中文cantonese：粤语language为英文时，可以不用设置此参数
                        } else if (wakeup_signal_inside_use == 1) {
                            //sr_change_session_begin_params_iat(sr, SESSION_BEGIN_PARAMS_IAT_RECORD_EN);
                            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
                            mIat.setParameter(SpeechConstant.ACCENT, null);
                        }
                        //WaitForSingleObject(m_mutex_wakup_cb, INFINITE);
                        wakeup_signal_inside_use = -1;
                        //::ReleaseMutex(m_mutex_wakup_cb);
                    } else if (wakeup_signal_from_outside >= 0) {
                        //外部按钮触发，仅唤醒，不修改iat中英文参数
                        wakeup_signal_from_outside = -1;
                    }

                    if (null != mIat) {
                        //唤醒后关闭唤醒录音状态
                        Log.e("lakexunfei", "run: 开启听写模式");
                        stopWakeUp();
                        //开启听写模式
                        // 不显示听写对话框
                        startRecogniz();
                        if (IAT_AWAKE_STATUS == 0) {//听写打开失败
                            startWakeUp();
                        }
                    }
                    sleep(20);
                } else {
                    sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //开启监听唤醒词状态
    private void startWakeUp() {
        if (mIvw != null) {
            mIvw.startListening(mWakeuperListener);
        }
        wakeup_signal_inside_use = -1;
        wakeup_signal_from_outside = -1;
        IAT_AWAKE_STATUS = 0;
    }

    //关闭监听唤醒词状态
    private void stopWakeUp() {
        if (mIvw != null) {
            mIvw.stopListening();
        }
        wakeup_signal_inside_use = -1;
        wakeup_signal_from_outside = -1;
    }

    //开启听写
    private void startRecogniz() {
        if (mIat != null) {
            int ret = mIat.startListening(mRecognizerListener);
            if (ret != ErrorCode.SUCCESS) {
                //还需要加个已经STARTED的条件
                Log.d(TAG, "听写失败,错误码：" + ret);
                IAT_AWAKE_STATUS = 0;
            } else {
                IAT_AWAKE_STATUS = 1;//已经唤醒了
            }
        }
    }

    //关闭听写
    private void stopRecogniz() {
        if (null != mIat) {
            mIat.stopListening();
        }
    }

    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            Log.d(TAG, "onResult");
            if (!"1".equalsIgnoreCase(keep_alive)) {
                //setRadioEnable(true);
            }
            try {
                String text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 " + text);
                buffer.append("\n");
                buffer.append("【操作类型】" + object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】" + object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                resultString = buffer.toString();

                //判断唤醒得分，然后开始iat的start
                if (Integer.parseInt(object.optString("score")) >= 1200) {//如果得分高于1200
                    speechIatAndAwakeListener.onShowAwakeText();
                }

            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            Log.e(TAG, resultString);
        }

        @Override
        public void onError(SpeechError error) {
            Log.e(TAG, error.getPlainDescription(true));
        }

        @Override
        public void onBeginOfSpeech() {

        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            switch (eventType) {
                // EVENT_RECORD_DATA 事件仅在 NOTIFY_RECORD_DATA 参数值为 真 时返回
                case SpeechEvent.EVENT_RECORD_DATA:
                    final byte[] audio = obj.getByteArray(SpeechEvent.KEY_EVENT_RECORD_DATA);
                    Log.i(TAG, "ivw audio length: " + audio.length);
                    break;
            }
        }

        @Override
        public void onVolumeChanged(int volume) {
        }
    };


    private String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(mContext.getApplicationContext(), ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + Constants.APP_ID + ".jet");
        Log.d(TAG, "resPath: " + resPath);
        return resPath;
    }

    /**
     * 听写监听器。 5S内没有说话进入唤醒监听模式
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            iatResultsList.clear();
            Log.d(TAG, "mRecognizerListener 开始说话");
            speechIatAndAwakeListener.isListening();
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if(error.getErrorCode() == 10118){
                speechIatAndAwakeListener.noBodySay();
            }
            if (mTranslateEnable && error.getErrorCode() == 14002) {
                Log.d(TAG, "mRecognizerListener " + error.getPlainDescription(true) + "\n请确认是否已开通翻译功能");
            } else {
                Log.d(TAG, "mRecognizerListener " + error.getPlainDescription(true));
            }
        }

        @Override
        public void onEndOfSpeech() {
            speechIatAndAwakeListener.isComplete();
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Log.d(TAG, "mRecognizerListener " + "结束说话");
            //打开唤醒词监听状态
            stopRecogniz();
            startWakeUp();
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, "mRecognizerListener " + results.getResultString());
            iatResultParts.clear();
            iatResultParts.is_last = isLast;
            iatResultsList.add(ParseIATQuestions.parseStrToBean(results.getResultString()));
            if (isLast) {
                iatResultParts.iat_question_string = ParseIATQuestions.parseBeanArrayToStr(iatResultsList.toArray(new IATResults[0]));
                write_iat_questions_list(iatResultParts);
                speechIatAndAwakeListener.showQuestionText(iatResultParts.iat_question_string);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            Log.d(TAG, "mRecognizerListener " + "当前正在说话，音量大小：" + volume);
            Log.d(TAG, "mRecognizerListener " + "返回音频数据：" + data.length);
            speech_volume_to_outside = volume;
            speechIatAndAwakeListener.onVolChange(volume);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    public void doStop() {
        stop = true;
    }
}

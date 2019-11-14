package com.lake.speechdemo.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.lake.speechdemo.R;
import com.lake.speechdemo.bean.ContentData;
import com.lake.speechdemo.bean.Controller;
import com.lake.speechdemo.bean.SpeechContent;
import com.lake.speechdemo.dapater.VoiceContentItemAdapter;
import com.lake.speechdemo.interfaces.OnSpeechBtnClickListener;
import com.lake.speechdemo.present.XunfeiPresent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lake.speechdemo.dapater.VoiceContentItemAdapter.TYPE_ANSWER;
import static com.lake.speechdemo.dapater.VoiceContentItemAdapter.TYPE_DEVICE_CAONTROL;
import static com.lake.speechdemo.dapater.VoiceContentItemAdapter.TYPE_QUESTION;

/**
 * 语音交互界面
 */
public class BaseBottomSheetView extends BottomSheetDialog implements IXunfeiView, OnSpeechBtnClickListener {
    private Context context;
    private SpeechVoiceButton speechVoiceButton;
    private ImageView questionBtn;
    private AnimatedRecordingView animatedRecordingView;
    private XunfeiPresent xunfeiPresent;
    private View view;
    private BottomSheetBehavior mBehavior;
    private FrameLayout content;
    private List<SpeechContent> mdatas = new ArrayList<>();
    private List<ContentData> realData = new ArrayList<>();

    private VoiceContentItemAdapter adapter;
    private RecyclerView recyclerView;

    private View pageFirstView;
    private View pageSecondView;
    private View pageHelpView;

    private static final int PAGE_FIRST = 0;
    private static final int PAGE_SECOND = 1;
    private static final int PAGE_HELP = 2;
    private int pageShow = PAGE_FIRST;//当前页面

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

        mBehavior = BottomSheetBehavior.from(view);
        speechVoiceButton = findViewById(R.id.speech_btn);
        animatedRecordingView = findViewById(R.id.animation_view);
        questionBtn = findViewById(R.id.question_btn);
        mBehavior.setSkipCollapsed(true);
        content = findViewById(R.id.dialog_content);
        initPageViews();
        setDialogContentView(pageFirstView);

        speechVoiceButton.setOnClickListener(v -> {
            showHelpTipContent(false);
            this.OnSpeechBtnClick(speechVoiceButton.getState());
        });

        questionBtn.setOnClickListener(v -> {
            showHelpTipContent(true);
        });
    }

    /**
     * 初始化子view
     */
    private void initPageViews() {
        pageFirstView = getLayoutInflater().inflate(R.layout.first_tip_layout, null);
        pageSecondView = getLayoutInflater().inflate(R.layout.second_tip_layout, null);
        pageHelpView = getLayoutInflater().inflate(R.layout.help_tip_layout, null);

        if (pageSecondView != null) {
            initSecondView(pageSecondView);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mBehavior != null) {
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        xunfeiPresent.bindXunfeiService(context);
    }

    @Override
    public void showQuestionText(String content) {//非空
        Log.e("lake", "showQuestionText: " + content);
        SpeechContent speechContent = new SpeechContent();
        speechContent.setType(0);
        speechContent.setTiltle(content);
        mdatas.add(speechContent);
        refreshContent();
    }

    @Override
    public void showAnswerText(String content) {
        Log.e("lake", "showAnswerText: " + content);
        SpeechContent speechContent = new SpeechContent();
        speechContent.setType(1);
        speechContent.setTiltle(content);

        List<Controller> list = new ArrayList<>();
        Map<String, Integer> cmap = new LinkedHashMap<>();
        Map<String, Integer> cmap1 = new LinkedHashMap<>();
        Map<String, Integer> cmap2 = new LinkedHashMap<>();
        cmap.put("制热", 1);
        cmap.put("制冷", 2);
        cmap1.put("自动", 3);
        cmap1.put("低速", 4);
        cmap1.put("中速", 5);
        cmap1.put("高速", 6);
        cmap2.put("更多功能", 10);
        list.add(new Controller(cmap));
        list.add(new Controller(cmap1));
        list.add(new Controller(cmap2));
        speechContent.setControllerList(list);

        mdatas.add(speechContent);
        refreshContent();
    }

    @Override
    public void showVolume(int level) {
        animatedRecordingView.setVolume((float) level * 3.2f);
    }

    @Override
    public void showStatus(SpeechVoiceButton.State state) {
        switch (state) {
            case sDefault://初始状态
                animatedRecordingView.stop();
                speechVoiceButton.showDefault();
                showVoiceBtn(true);
                break;
            case sListening://听写状态
                showVoiceBtn(false);
                animatedRecordingView.start();
                speechVoiceButton.showListening();
                break;
            case sDoing://解析问题状态
                animatedRecordingView.loading();
                speechVoiceButton.showDoHandle();
                break;
            case sStop://正在播放语音状态
                animatedRecordingView.stop();
                speechVoiceButton.showStop();
                showVoiceBtn(true);
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
        view = window.findViewById(R.id.design_bottom_sheet);
        view.setBackgroundResource(android.R.color.transparent);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.BOTTOM);
    }

    private void showVoiceBtn(boolean show) {
        questionBtn.setVisibility(show ? View.VISIBLE : View.GONE);
        speechVoiceButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    /**
     * 交互界面初始化
     *
     * @param view
     */
    private void initSecondView(View view) {
        recyclerView = view.findViewById(R.id.recycler_list);
        adapter = new VoiceContentItemAdapter(context, realData);
        adapter.setOnDeviceControlListener((choose, name, position) -> {
            Toast.makeText(context, "click to " + name + " is " + choose + ",position is " + position, Toast.LENGTH_SHORT).show();
        });
        ContentGridLayout contentGridLayout = new ContentGridLayout(context, adapter);
        int space = dip2px(context, 8);
        recyclerView.setHasFixedSize(true);
        if (recyclerView.getItemDecorationCount() == 0)
            recyclerView.addItemDecoration(new SpacesItemDecoration(space));
        recyclerView.setLayoutManager(contentGridLayout);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 刷新对话列表
     */
    private synchronized void refreshContent() {
        synchronized (realData) {
            if (pageShow != PAGE_SECOND) {
                setDialogContentView(pageSecondView);
                pageShow = PAGE_SECOND;
            }
            if (adapter != null) {
                initRealData(mdatas);
                adapter.notifyDataSetChanged();
                if (recyclerView != null)
                    recyclerView.smoothScrollToPosition(realData.size() == 0 ? 0 : realData.size() - 1);
            }
        }
    }

    /**
     * 显示帮助界面
     */
    private void showHelpTipContent(boolean show) {
        if (show) {
            pageShow = PAGE_HELP;
            setDialogContentView(pageHelpView);
        } else {
            if (mdatas.size() == 0) {
                pageShow = PAGE_FIRST;
                setDialogContentView(pageFirstView);
            } else {
                pageShow = PAGE_SECOND;
                setDialogContentView(pageSecondView);
            }
        }
        questionBtn.setVisibility(show ? View.GONE : View.VISIBLE);

    }

    /**
     * 设置内容布局
     *
     * @param view
     */
    private void setDialogContentView(View view) {
        if (content != null && view != null) {
            content.removeAllViews();
            content.addView(view);
        }
    }

    /**
     * 数据转换
     *
     * @param mdatas
     */
    private void initRealData(List<SpeechContent> mdatas) {
        realData.clear();
        for (SpeechContent content : mdatas) {
            if (content.getType() == 0) {//问题
                realData.add(new ContentData(content.getTiltle(), TYPE_QUESTION));
                continue;
            }
            if (content.getType() == 1) {//答案
                realData.add(new ContentData(content.getTiltle(), TYPE_ANSWER));
                List<Controller> controllerList = content.getControllerList();
                if (controllerList != null && controllerList.size() != 0) {
                    for (Controller controller : controllerList) {
                        Map<String, Integer> cmap = controller.getDeviceControlMap();//操作集合 每个集合最多四个
                        int i = 0;
                        for (Map.Entry<String, Integer> entry : cmap.entrySet()) {
                            realData.add(new ContentData(entry.getKey(), TYPE_DEVICE_CAONTROL));
                            i++;
                        }
                        while (i < 4) {//补全行数
                            i++;
                            realData.add(new ContentData("", TYPE_DEVICE_CAONTROL));
                        }
                    }
                }
            }
        }
    }

    /**
     * dp转px
     *
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    }
}

package com.lake.speechdemo.view;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;

import com.lake.speechdemo.dapater.VoiceContentItemAdapter;

/**
 * 网格布局
 */
public class ContentGridLayout extends GridLayoutManager {

    private VoiceContentItemAdapter adapter;

    public ContentGridLayout(Context context, VoiceContentItemAdapter adapter) {
        this(context, 4, VERTICAL, false);
        this.adapter = adapter;
        this.setSpanSizeLookup(new SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                switch (adapter.getItemViewType(i)) {
                    case VoiceContentItemAdapter.TYPE_QUESTION:
                    case VoiceContentItemAdapter.TYPE_ANSWER:
                        return 4;
                    case VoiceContentItemAdapter.TYPE_DEVICE_CAONTROL:
                        return 1;
                }
                return 1;
            }
        });
    }

    private ContentGridLayout(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }
}

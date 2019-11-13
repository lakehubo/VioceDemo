package com.lake.speechdemo.base;

import java.lang.ref.WeakReference;

public abstract class BasePresent<T> {
    protected WeakReference<T> mWeakRef;

    public void attchView(T view) {
        mWeakRef = new WeakReference<T>(view);
    }

    public void detachView() {
        if (mWeakRef != null)
            mWeakRef.clear();
        mWeakRef = null;
    }
}

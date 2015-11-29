package com.pluscubed.plustimer;

import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

//Taken from Mosby
public class MvpPresenter<V extends MvpView> {

    private WeakReference<V> mViewRef;

    public void attachView(V view) {
        mViewRef = new WeakReference<>(view);
    }

    @Nullable
    public V getView() {
        return mViewRef == null ? null : mViewRef.get();
    }

    public boolean isViewAttached() {
        return mViewRef != null && mViewRef.get() != null;
    }

    public void detachView() {
        if (mViewRef != null) {
            mViewRef.clear();
            mViewRef = null;
        }
    }
}
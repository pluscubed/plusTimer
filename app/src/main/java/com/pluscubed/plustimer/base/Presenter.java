package com.pluscubed.plustimer.base;

import android.support.annotation.Nullable;

public abstract class Presenter<V> {

    private V mView;

    public void onViewAttached(V view) {
        mView = view;
    }

    @Nullable
    public V getView() {
        return mView;
    }

    public boolean isViewAttached() {
        return mView != null;
    }

    public void onViewDetached() {
        mView = null;
    }

    public void onDestroyed() {

    }
}
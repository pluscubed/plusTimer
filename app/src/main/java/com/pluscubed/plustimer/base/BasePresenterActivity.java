package com.pluscubed.plustimer.base;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public abstract class BasePresenterActivity<P extends Presenter<V>, V> extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<P> {

    private static final String TAG = "base-activity";
    private static final int LOADER_ID = 101;
    @Nullable
    protected P mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart-" + tag());
        mPresenter.onViewAttached(getPresenterView());
    }

    @Override
    protected void onStop() {
        mPresenter.onViewDetached();
        super.onStop();
        Log.i(TAG, "onStop-" + tag());
    }

    @Override
    public final Loader<P> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCreateLoader");
        return new PresenterLoader<>(this, getPresenterFactory(), tag());
    }

    @Override
    public final void onLoadFinished(Loader<P> loader, P presenter) {
        Log.i(TAG, "onLoadFinished");
        this.mPresenter = presenter;
        onPresenterPrepared(presenter);
    }

    @Override
    public final void onLoaderReset(Loader<P> loader) {
        Log.i(TAG, "onLoaderReset");
        this.mPresenter = null;
        onPresenterDestroyed();
    }

    protected String tag() {
        return getClass().getName();
    }

    protected abstract PresenterFactory<P> getPresenterFactory();

    protected abstract void onPresenterPrepared(P presenter);

    protected final void onPresenterDestroyed() {
        // hook for subclasses
    }

    // Override in case of Activity not implementing Presenter<View> interface
    protected V getPresenterView() {
        return (V) this;
    }
}
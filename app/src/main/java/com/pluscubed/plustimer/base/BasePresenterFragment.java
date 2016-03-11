package com.pluscubed.plustimer.base;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;

public abstract class BasePresenterFragment<P extends Presenter<V>, V> extends Fragment{

    private static final String TAG = "base-fragment";
    private static final int LOADER_ID = 101;
    protected P presenter;
    // boolean flag to avoid delivering the result twice. Calling initLoader in onActivityCreated makes
    // onLoadFinished will be called twice during configuration change.
    private boolean delivered = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // LoaderCallbacks as an object, so no hint regarding loader will be leak to the subclasses.
        getLoaderManager().initLoader(LOADER_ID, null, new LoaderManager.LoaderCallbacks<P>() {
            @Override
            public final Loader<P> onCreateLoader(int id, Bundle args) {
                return new PresenterLoader<>(getActivity(), getPresenterFactory());
            }

            @Override
            public final void onLoadFinished(Loader<P> loader, P presenter) {
                if (!delivered) {
                    BasePresenterFragment.this.presenter = presenter;
                    delivered = true;
                    onPresenterPrepared(presenter);
                }
            }

            @Override
            public final void onLoaderReset(Loader<P> loader) {
                BasePresenterFragment.this.presenter = null;
                onPresenterDestroyed();
            }
        });
    }



    @Override
    public void onResume() {
        super.onResume();
        presenter.onViewAttached(getPresenterView());
    }

    @Override
    public void onStop() {
        presenter.onViewDetached();
        super.onStop();
    }

    protected abstract PresenterFactory<P> getPresenterFactory();

    protected abstract void onPresenterPrepared(P presenter);

    protected void onPresenterDestroyed() {
        // hook for subclasses
    }

    // Override in case of fragment not implementing Presenter<View> interface
    protected V getPresenterView() {
        return (V) this;
    }
}
package com.pluscubed.plustimer.ui.solvelist;

import android.os.Bundle;

import com.pluscubed.plustimer.base.PresenterFactory;

public class SolveListPresenterFactory implements PresenterFactory<SolveListPresenter> {

    private Bundle arguments;

    public SolveListPresenterFactory(Bundle bundle) {
        arguments = bundle;
    }

    @Override
    public SolveListPresenter create() {
        return new SolveListPresenter(arguments);
    }
}

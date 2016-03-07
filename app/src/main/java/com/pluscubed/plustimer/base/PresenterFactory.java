package com.pluscubed.plustimer.base;

/**
 * Creates a Presenter object.
 *
 * @param <T> mPresenter type
 */
public interface PresenterFactory<T extends Presenter> {
    T create();
}
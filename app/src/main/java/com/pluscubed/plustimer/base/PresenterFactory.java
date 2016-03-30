package com.pluscubed.plustimer.base;

/**
 * Creates a Presenter object.
 *
 * @param <T> presenter type
 */
public interface PresenterFactory<T extends Presenter> {
    T create();
}
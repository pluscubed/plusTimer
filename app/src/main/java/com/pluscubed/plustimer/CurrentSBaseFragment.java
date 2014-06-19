package com.pluscubed.plustimer;

import android.app.Activity;
import android.support.v4.app.Fragment;

/**
 * Created by DC on 6/18/2014.
 */
public class CurrentSBaseFragment extends Fragment {

    public Activity getAttachedActivity() {
        if (getParentFragment() != null)
            return getParentFragment().getActivity();
        return getActivity();
    }

    public void onSolveItemClick(int position) {
        CurrentSessionActivityCallback callback;
        try {
            callback = (CurrentSessionActivityCallback) getAttachedActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getAttachedActivity().toString()
                    + " must implement CurrentSessionActivityCallback");
        }
        callback.showCurrentSolveDialog(position);
    }

    public interface CurrentSessionActivityCallback {
        public void showCurrentSolveDialog(int position);

        public void lockOrientation(boolean lock);
    }
}

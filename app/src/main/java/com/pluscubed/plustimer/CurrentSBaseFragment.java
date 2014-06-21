package com.pluscubed.plustimer;

import android.app.Activity;
import android.support.v4.app.Fragment;

/**
 * Created by DC on 6/18/2014.
 */
public abstract class CurrentSBaseFragment extends Fragment {

    public Activity getAttachedActivity() {
        if (getParentFragment() != null)
            return getParentFragment().getActivity();
        return getActivity();
    }

    public void onSolveItemClick(int position) {
        OnSolveItemClickListener listener;
        try {
            listener = (OnSolveItemClickListener) getAttachedActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getAttachedActivity().toString()
                    + " must implement OnSolveItemClickListener");
        }
        listener.showCurrentSolveDialog(position);
    }

    abstract void onSessionSolvesChanged();

    abstract void onSessionChanged();

    public interface OnSolveItemClickListener {
        public void showCurrentSolveDialog(int position);
    }
}

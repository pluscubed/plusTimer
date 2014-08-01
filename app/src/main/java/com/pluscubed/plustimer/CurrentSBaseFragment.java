package com.pluscubed.plustimer;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

/**
 * Abstract class
 */
public abstract class CurrentSBaseFragment extends Fragment {


    public void setUpPuzzleSpinner(Menu menu) {
        final Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_current_s_puzzletype_spinner));

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        ((ActionBarActivity) getAttachedActivity()).getSupportActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);


        menuPuzzleSpinner.post(new Runnable() {
            @Override
            public void run() {
                menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleType.get(PuzzleType.CURRENT)), true);
                menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        PuzzleType.setCurrentPuzzleType((PuzzleType) parent.getItemAtPosition(position));
                        GetChildFragmentsListener listener;
                        try {
                            listener = (GetChildFragmentsListener) getParentFragment();
                        } catch (ClassCastException e) {
                            throw new ClassCastException(getAttachedActivity().toString()
                                    + " must implement GetChildFragmentsListener");
                        }
                        for (CurrentSBaseFragment i : listener.getChildFragments()) {
                            i.onSessionChanged();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });
    }

    public Activity getAttachedActivity() {
        if (getParentFragment() != null) return getParentFragment().getActivity();
        return getActivity();
    }

    public void onSolveItemClick(String displayName, int sessionIndex, int solveIndex) {
        OnSolveItemClickListener listener;
        try {
            listener = (OnSolveItemClickListener) getAttachedActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getAttachedActivity().toString()
                    + " must implement OnSolveItemClickListener");
        }
        listener.showCurrentSolveDialog(displayName, sessionIndex, solveIndex);
    }

    abstract void onSessionSolvesChanged();

    abstract void onSessionChanged();

    public interface GetChildFragmentsListener {
        ArrayList<CurrentSBaseFragment> getChildFragments();
    }

    public interface OnSolveItemClickListener {
        void showCurrentSolveDialog(String displayName, int sessionIndex, int solveIndex);
    }
}

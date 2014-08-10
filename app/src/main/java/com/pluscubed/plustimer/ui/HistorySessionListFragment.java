/*
package com.pluscubed.plustimer.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;

import java.io.IOException;
import java.util.ArrayList;

*/
/**
 * History Fragment
 *//*

public class HistorySessionListFragment extends ListFragment {

    private static final String STATE_PUZZLETYPE_DISPLAYNAME = "puzzletype_displayname";
    private static final String STATE_CAB_BOOLEAN = "cab_displayed";

    private String mPuzzleTypeDisplayName;

    private ActionMode mActionMode;

    @Override
    public void onPause() {
        super.onPause();
        PuzzleType.get(mPuzzleTypeDisplayName).saveHistorySessionsToFile(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeDisplayName);
        outState.putBoolean(STATE_CAB_BOOLEAN, mActionMode != null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mPuzzleTypeDisplayName = savedInstanceState.getString(STATE_PUZZLETYPE_DISPLAYNAME);
        } else {
            mPuzzleTypeDisplayName = PuzzleType.CURRENT;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_history_sessionlist, menu);
        final Spinner menuPuzzleSpinner = (Spinner) menu.findItem(R.id.menu_history_sessionlist_puzzltype_spinner).getActionView();

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        getActivity().getActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);

        menuPuzzleSpinner.post(new Runnable() {
            @Override
            public void run() {
                menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleType.get(mPuzzleTypeDisplayName)), true);
                menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (menuPuzzleSpinner.getSelectedItemPosition() != puzzleTypeSpinnerAdapter.getPosition(PuzzleType.get(mPuzzleTypeDisplayName))) {
                            mPuzzleTypeDisplayName = (parent.getItemAtPosition(position)).toString();
                            ((SessionListAdapter) getListAdapter()).onSessionListChanged();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history_sessionlist, container, false);
        try {
            setListAdapter(new SessionListAdapter());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_CAB_BOOLEAN)) {
            getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            getActivity().startActionMode(new SolveListActionModeCallback());
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }

                getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                getListView().setItemChecked(position, true);
                ((SessionListAdapter) getListAdapter()).onSessionListChanged();
                getActivity().startActionMode(new SolveListActionModeCallback());
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SessionListAdapter) getListAdapter()).onSessionListChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode == null) {
            Intent i = new Intent(getActivity(), HistorySessionListActivity.class);
            i.putExtra(HistorySessionListActivity.EXTRA_HISTORY_SESSION_POSITION, position);
            i.putExtra(HistorySessionListActivity.EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeDisplayName);
            startActivity(i);
        } else {
            ((SessionListAdapter) getListAdapter()).onSessionListChanged();
        }
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    private class SolveListActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            getActivity().getMenuInflater().inflate(R.menu.context_solve_or_session_list, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            SparseBooleanArray checked;
            switch (item.getItemId()) {
                case R.id.context_solvelist_delete_menuitem:
                    checked = getListView().getCheckedItemPositions();
                    ArrayList<Session> toDelete = new ArrayList<Session>();
                    for (int i = 0; i < checked.size(); i++) {
                        final int index = checked.keyAt(i);
                        if (checked.get(index)) {
                            toDelete.add(PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity()).get(index));
                        }
                    }
                    for (Session i : toDelete) {
                        PuzzleType.get(mPuzzleTypeDisplayName).deleteHistorySession(i, getActivity());
                    }
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getListView().clearChoices();
            getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            mActionMode = null;
            ((SessionListAdapter) getListAdapter()).onSessionListChanged();
        }
    }

    public class SessionListAdapter extends ArrayAdapter<Session> {
        SessionListAdapter() throws IOException {
            super(getActivity(), 0, PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_history_sessionlist, parent, false);
            }
            Session session = getItem(position);
            TextView text = (TextView) convertView.findViewById(R.id.list_item_history_sessionlist_textview);
            text.setText(session.getTimestampStringOfLastSolve(getActivity()));
            if (mActionMode != null && getListView().getCheckedItemPositions().get(position)) {
                convertView.setBackgroundColor(Color.parseColor("#aaaaaa"));
            } else {
                convertView.setBackgroundResource(0);
            }

            return convertView;
        }

        public void onSessionListChanged() {
            clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addAll(PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity()));
            else {
                for (Session i : PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity())) {
                    add(i);
                }
            }
            notifyDataSetChanged();
        }

    }

}
*/

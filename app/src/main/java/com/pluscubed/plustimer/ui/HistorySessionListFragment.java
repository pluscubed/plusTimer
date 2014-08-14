package com.pluscubed.plustimer.ui;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

/**
 * History SessionList Fragment
 */

public class HistorySessionListFragment extends ListFragment {

    private static final String STATE_PUZZLETYPE_DISPLAYNAME = "puzzletype_displayname";

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.context_solve_or_session_list, menu);
                mActionMode = mode;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.context_solvelist_delete_menuitem:
                        for (int i = getListView().getCount() - 1; i >= 0; i--) {
                            if (getListView().isItemChecked(i)) {
                                PuzzleType.get(mPuzzleTypeDisplayName).deleteHistorySession((Session) getListView().getItemAtPosition(i), getActivity());
                            }
                        }
                        mode.finish();
                        ((SessionListAdapter) getListAdapter()).onSessionListChanged();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
            }
        });
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_history_sessionlist, menu);
        final Spinner menuPuzzleSpinner = (Spinner) menu.findItem(R.id.menu_history_sessionlist_puzzletype_spinner).getActionView();

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        getActivity().getActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history_sessionlist, container, false);
        try {
            setListAdapter(new SessionListAdapter());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Update list when session is deleted in HistorySolveList
        ((SessionListAdapter) getListAdapter()).onSessionListChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), HistorySolveListActivity.class);
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_SESSION_POSITION, position);
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeDisplayName);
        startActivity(i);
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

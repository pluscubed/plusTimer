package com.pluscubed.plustimer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;

/**
 * History Fragment
 */
public class HistorySessionListFragment extends ListFragment {

    private static final String STATE_PUZZLETYPE_DISPLAYNAME = "puzzletype_displayname";

    private String mPuzzleTypeDisplayName;

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_history_sessionlist, menu);
        final Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_history_sessionlist_puzzltype_spinner));

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        ((ActionBarActivity) getActivity()).getSupportActionBar().getThemedContext(),
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
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
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
            super(getActivity(), android.R.layout.simple_list_item_1, PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            Session session = getItem(position);
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(session.getTimestampStringOfLastSolve(getActivity()));
            return convertView;
        }

        public void onSessionListChanged() {
            clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                try {
                    addAll(PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            else {
                try {
                    for (Session i : PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity())) {
                        add(i);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            notifyDataSetChanged();
        }

    }

}

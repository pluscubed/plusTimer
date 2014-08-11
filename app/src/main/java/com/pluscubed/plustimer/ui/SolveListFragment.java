package com.pluscubed.plustimer.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Session tab
 */
public class SolveListFragment extends Fragment {
    public static final String TAG = "SOLVE_LIST_FRAGMENT";

    private static final String ARG_PUZZLETYPE_DISPLAYNAME = "com.pluscubed.plustimer.solvelist.display_name";
    private static final String ARG_SESSION_POSITION = "com.pluscubed.plustimer.solvelist.session_position";
    private static final String ARG_CURRENT_BOOLEAN = "com.pluscubed.plustimer.solvelist.current_boolean";

    private Session mSession;

    private TextView mQuickStats;
    private ListView mListView;
    private TextView mEmptyView;

    private SolveListAdapter mListAdapter;

    private int mSessionIndex;
    private String mPuzzleTypeDisplayName;
    private boolean mCurrentToggle;

    private ActionMode mActionMode;

    private LinearLayout mResetSubmitLinearLayout;

    public static SolveListFragment newInstance(boolean current, String displayName, int sessionIndex) {
        SolveListFragment f = new SolveListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SESSION_POSITION, sessionIndex);
        args.putString(ARG_PUZZLETYPE_DISPLAYNAME, displayName);
        args.putBoolean(ARG_CURRENT_BOOLEAN, current);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(
                Intent.EXTRA_TEXT,
                mSession.toString(getActivity(), PuzzleType.get(mPuzzleTypeDisplayName).toString(), mCurrentToggle, true)
        );
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_dialog_title)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentToggle = getArguments().getBoolean(ARG_CURRENT_BOOLEAN);
        mPuzzleTypeDisplayName = getArguments().getString(ARG_PUZZLETYPE_DISPLAYNAME);
        mSessionIndex = getArguments().getInt(ARG_SESSION_POSITION);
        mSession = PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_activity_current_session_share_menuitem:
                share();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void updateStats() {
        mQuickStats.setText(mSession.toString(getActivity(), PuzzleType.get(mPuzzleTypeDisplayName).toString(), mCurrentToggle, false));
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_solvelist, container, false);


        if (mCurrentToggle) {
            mResetSubmitLinearLayout = (LinearLayout) v.findViewById(R.id.fragment_solvelist_submit_reset_linearlayout);

            Button reset = (Button) v.findViewById(R.id.fragment_solvelist_reset_button);
            reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PuzzleType.get(mPuzzleTypeDisplayName).resetCurrentSession();
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.session_reset), Toast.LENGTH_SHORT).show();
                    onSessionSolvesChanged();
                }
            });
            Button submit = (Button) v.findViewById(R.id.fragment_solvelist_submit_button);
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSession.getNumberOfSolves() == 0) {
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.session_no_solves), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.session_submitted), Toast.LENGTH_SHORT).show();

                    PuzzleType.get(mPuzzleTypeDisplayName).submitCurrentSession();

                    onSessionSolvesChanged();
                }
            });

        }

        mListView = (ListView) v.findViewById(android.R.id.list);
        mListAdapter = new SolveListAdapter();
        View header = inflater.inflate(R.layout.solvelist_header, null);
        mQuickStats = (TextView) header.findViewById(R.id.solvelist_header_stats_textview);
        mListView.addHeaderView(header, null, false);
        mListView.setAdapter(mListAdapter);

        mEmptyView = (TextView) v.findViewById(android.R.id.empty);

        //Getting CAB to work API9+: Doctoror Drive's answer - http://stackoverflow.com/questions/14737519/how-can-you-implement-multi-selection-and-contextual-actionmode-in-actionbarsher

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    CreateDialogCallback callback = (CreateDialogCallback) getActivity();
                    callback.createSolveDialog(mPuzzleTypeDisplayName, mSessionIndex, mSession.getPosition((Solve) mListView.getItemAtPosition(position)));
                } catch (ClassCastException e) {
                    throw new ClassCastException(getActivity().toString()
                            + " must implement CreateDialogCallback");
                }
            }
        });

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

            }

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
                switch (item.getItemId()) {
                    case R.id.context_solvelist_delete_menuitem:
                        for (int i = mListView.getCount() - 1; i >= 0; i--) {
                            if (mListView.isItemChecked(i)) {
                                mSession.deleteSolve((Solve) mListView.getItemAtPosition(i));
                            }
                        }
                        mode.finish();
                        onSessionSolvesChanged();
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

        onSessionSolvesChanged();
        return v;
    }

    public void enableResetSubmitButtons(boolean enable) {
        mResetSubmitLinearLayout.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        PuzzleType.get(mPuzzleTypeDisplayName).saveHistorySessionsToFile(getActivity());
    }

    public void onSessionSolvesChanged() {
        mSession = PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getActivity());
        if (!mCurrentToggle && mSession.getNumberOfSolves() <= 0) {
            PuzzleType.get(mPuzzleTypeDisplayName).deleteHistorySession(mSessionIndex, getActivity());
            getActivity().finish();
            return;
        }
        mListAdapter.updateSolvesList();
        if (mListAdapter.getCount() == 0) {
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
        updateStats();
        if (mCurrentToggle)
            enableResetSubmitButtons(PuzzleType.get(mPuzzleTypeDisplayName).getCurrentSession().getNumberOfSolves() > 0);
    }

    public void onSessionChanged() {
        onSessionSolvesChanged();
    }


    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveListAdapter() {
            super(getActivity(), 0, new ArrayList<Solve>());
            updateSolvesList();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_solvelist, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.list_item_solvelist_title_textview);
            TextView desc = (TextView) convertView.findViewById(R.id.list_item_solvelist_desc_textview);


            time.setText("");

            for (Solve a : mBestAndWorstSolves) {
                if (a == s) {
                    time.setText("(" + s.getDescriptiveTimeString() + ")");
                }
            }

            if (time.getText() == "") {
                time.setText(s.getDescriptiveTimeString());
            }

            desc.setText(s.getScrambleAndSvg().scramble);

            return convertView;
        }

        public void updateSolvesList() {
            clear();
            List<Solve> solves = mSession.getSolves();
            Collections.reverse(solves);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addAll(solves);
            else {
                for (Solve i : solves) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Session.getBestSolve(mSession.getSolves()));
            mBestAndWorstSolves.add(Session.getWorstSolve(mSession.getSolves()));
            notifyDataSetChanged();
        }


    }
}

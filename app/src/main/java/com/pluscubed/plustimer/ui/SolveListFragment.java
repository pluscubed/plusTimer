package com.pluscubed.plustimer.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Session tab
 */
public class SolveListFragment extends Fragment {

    public static final String TAG = "SOLVE_LIST_FRAGMENT";

    private static final String ARG_PUZZLETYPE_DISPLAYNAME
            = "com.pluscubed.plustimer.solvelist.display_name";

    private static final String ARG_SESSION_POSITION
            = "com.pluscubed.plustimer.solvelist.session_position";

    private static final String ARG_CURRENT_BOOLEAN
            = "com.pluscubed.plustimer.solvelist.current_boolean";

    private final Session.Observer sessionObserver = new Session.Observer() {
        @Override
        public void onSolveAdded() {
            onSessionSolvesChanged();
        }

        @Override
        public void onSolveChanged(int index) {
            onSessionSolvesChanged();
        }

        @Override
        public void onSolveRemoved(int index) {
            onSessionSolvesChanged();
        }

        @Override
        public void onReset() {
            onSessionSolvesChanged();
        }
    };

    private final PuzzleType.Observer puzzleTypeObserver = new PuzzleType
            .Observer() {
        @Override
        public void onPuzzleTypeChanged() {
            onSessionSolvesChanged();
            mSession.registerObserver(sessionObserver);
        }
    };

    private Session mSession;
    private boolean mMillisecondsEnabled;
    private boolean mSignEnabled;
    private TextView mQuickStats;
    private ListView mListView;
    private TextView mEmptyView;
    private SolveListAdapter mListAdapter;
    private int mSessionIndex;
    private String mPuzzleTypeName;
    private boolean mCurrentToggle;
    private ActionMode mActionMode;
    private LinearLayout mResetSubmitLinearLayout;

    public static SolveListFragment newInstance(boolean current,
                                                String puzzleTypeName,
                                                int sessionIndex) {
        SolveListFragment f = new SolveListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SESSION_POSITION, sessionIndex);
        args.putString(ARG_PUZZLETYPE_DISPLAYNAME, puzzleTypeName);
        args.putBoolean(ARG_CURRENT_BOOLEAN, current);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onResume() {
        super.onResume();
        initSharedPrefs();

        //When Settings change
        onSessionSolvesChanged();
    }

    private void initSharedPrefs() {
        mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(getActivity());
        mSignEnabled = PrefUtils.isSignEnabled(getActivity());
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
                mSession.toString(getActivity(), mPuzzleTypeName,
                        mCurrentToggle, true, mMillisecondsEnabled,
                        mSignEnabled)
        );
        startActivity(Intent.createChooser(intent, getResources().getString(R
                .string.share_dialog_title)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentToggle = getArguments().getBoolean(ARG_CURRENT_BOOLEAN);
        mPuzzleTypeName = getArguments().getString(ARG_PUZZLETYPE_DISPLAYNAME);
        mSessionIndex = getArguments().getInt(ARG_SESSION_POSITION);
        mSession = getPuzzleType().getSession(mSessionIndex);
        PuzzleType.registerObserver(puzzleTypeObserver);
        mSession.registerObserver(sessionObserver);
        setHasOptionsMenu(true);
    }

    private PuzzleType getPuzzleType() {
        if (mCurrentToggle) {
            return PuzzleType.getCurrent();
        } else {
            return PuzzleType.valueOf(mPuzzleTypeName);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_solvelist_share_menuitem:
                share();
                return true;
            case R.id.menu_history_solvelist_delete_menuitem:
                getPuzzleType().getHistorySessions().deleteSession
                        (mSessionIndex, getActivity());
                getActivity().finish();
                return true;
            case R.id.menu_solvelist_add_menuitem:
                try {
                    CreateDialogCallback callback = (CreateDialogCallback) getActivity();
                    callback.createSolveAddDialog(mPuzzleTypeName,
                            mSessionIndex);
                } catch (ClassCastException e) {
                    throw new ClassCastException(getActivity().toString()
                            + " must implement CreateDialogCallback");
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void updateStats() {
        mQuickStats.setText(
                mSession.toString(getActivity(), getPuzzleType().toString(),
                        mCurrentToggle, false, mMillisecondsEnabled,
                        mSignEnabled));
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_solvelist, container,
                false);
        initSharedPrefs();

        if (mCurrentToggle) {
            mResetSubmitLinearLayout = (LinearLayout) v
                    .findViewById(R.id
                            .fragment_solvelist_submit_reset_linearlayout);

            Button reset = (Button) v.findViewById(R.id
                    .fragment_solvelist_reset_button);
            reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder
                            (getActivity());
                    builder.setMessage(getString(R.string
                            .reset_warning_message))
                            .setIcon(R.drawable.ic_action_warning)
                            .setTitle(getString(R.string.reset_warning))
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface
                                                                    dialog,
                                                            int id) {
                                            getPuzzleType()
                                                    .resetCurrentSession();
                                            Toast.makeText(getActivity()
                                                            .getApplicationContext(),
                                                    getResources().getText(R
                                                            .string
                                                            .session_reset),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface
                                                                    dialog,
                                                            int id) {
                                        }
                                    });
                    builder.create().show();
                }
            });
            Button submit = (Button) v.findViewById(R.id
                    .fragment_solvelist_submit_button);
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSession.getNumberOfSolves() == 0) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getResources().getText(R.string
                                        .session_no_solves),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(getActivity().getApplicationContext(),
                            getResources().getText(R.string
                                    .session_submitted), Toast.LENGTH_SHORT)
                            .show();

                    getPuzzleType().submitCurrentSession(getActivity());
                }
            });

        }

        mListView = (ListView) v.findViewById(android.R.id.list);
        mListAdapter = new SolveListAdapter();
        View header = inflater.inflate(R.layout.solvelist_header, mListView,
                false);
        mQuickStats = (TextView) header.findViewById(R.id
                .solvelist_header_stats_textview);
        mListView.addHeaderView(header, null, false);
        mListView.setAdapter(mListAdapter);

        mEmptyView = (TextView) v.findViewById(android.R.id.empty);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                try {
                    CreateDialogCallback callback = (CreateDialogCallback)
                            getActivity();
                    callback.createSolveDisplayDialog(mPuzzleTypeName,
                            mSessionIndex, mSession.getPosition((Solve)
                                    mListView.getItemAtPosition(position)));
                } catch (ClassCastException e) {
                    throw new ClassCastException(getActivity().toString()
                            + " must implement CreateDialogCallback");
                }
            }
        });

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mListView.setMultiChoiceModeListener(new AbsListView
                .MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id,
                                                  boolean checked) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mActionMode = mode;
                getActivity().getMenuInflater().inflate(R.menu
                        .context_solve_or_session_list, menu);
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
                                mSession.deleteSolve((Solve) mListView
                                        .getItemAtPosition(i));
                            }
                        }
                        mode.finish();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.unregisterObserver(sessionObserver);
        PuzzleType.unregisterObserver(puzzleTypeObserver);
    }

    public void enableResetSubmitButtons(boolean enable) {
        mResetSubmitLinearLayout.setVisibility(enable ? View.VISIBLE : View
                .GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPuzzleType().getHistorySessions().save(getActivity());
        if (mCurrentToggle) getPuzzleType().saveCurrentSession(getActivity());
    }

    public void onSessionSolvesChanged() {
        mSession = getPuzzleType().getSession(mSessionIndex);
        if (!mCurrentToggle && mSession.getNumberOfSolves() <= 0) {
            getPuzzleType().getHistorySessions().deleteSession(mSessionIndex,
                    getActivity());
            getActivity().finish();
            return;
        }
        mListAdapter.onSolveListChanged();
        if (mListAdapter.getCount() == 0) {
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
        updateStats();
        if (mCurrentToggle) {
            enableResetSubmitButtons(getPuzzleType().getSession(PuzzleType
                    .CURRENT_SESSION).getNumberOfSolves() > 0);
        } else {
            getActivity().setTitle(PuzzleType.valueOf(mPuzzleTypeName)
                    .getSession(mSessionIndex).getTimestampString(getActivity
                            ()));
        }
    }


    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;
        private boolean mSignEnabled;

        public SolveListAdapter() {
            super(getActivity(), 0, new ArrayList<Solve>());
            onSolveListChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.list_item_solvelist, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView
                    .findViewById(R.id.list_item_solvelist_title_textview);
            TextView desc = (TextView) convertView
                    .findViewById(R.id.list_item_solvelist_desc_textview);

            time.setText("");

            for (Solve a : mBestAndWorstSolves) {
                if (a == s) {
                    time.setText("(" + s.getDescriptiveTimeString
                            (mMillisecondsEnabled) + ")");
                }
            }

            if (time.getText() == "") {
                time.setText(s.getDescriptiveTimeString(mMillisecondsEnabled));
            }

            desc.setText(s.getScrambleAndSvg().getUiScramble(mSignEnabled,
                    mPuzzleTypeName));

            return convertView;
        }

        public void onSolveListChanged() {
            clear();
            List<Solve> solves = mSession.getSolves();
            Collections.reverse(solves);
            addAll(solves);
            mBestAndWorstSolves = new ArrayList<>();
            mBestAndWorstSolves.add(Utils.getBestSolveOfList(mSession
                    .getSolves()));
            mBestAndWorstSolves.add(Utils.getWorstSolveOfList(mSession
                    .getSolves()));
            mSignEnabled = PrefUtils.isSignEnabled(getActivity());
            notifyDataSetChanged();
        }


    }
}

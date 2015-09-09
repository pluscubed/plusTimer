package com.pluscubed.plustimer.ui;

import android.app.Fragment;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;
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
    private static final String ARG_SESSION_ID
            = "com.pluscubed.plustimer.solvelist.session_position";
    private static final String ARG_CURRENT_BOOLEAN
            = "com.pluscubed.plustimer.solvelist.current_boolean";

    private Session mSession;
    private boolean mMillisecondsEnabled;
    private boolean mSignEnabled;
    private TextView mQuickStats;
    private ListView mListView;
    private TextView mEmptyView;
    private SolveListAdapter mListAdapter;
    private String mSessionId;
    private String mPuzzleTypeId;
    private boolean mCurrentToggle;
    private final ChildEventListener sessionObserver = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            onSessionSolvesChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            onSessionSolvesChanged();

            //TODO
            /*PuzzleType.getDataSource().updateSolve(PuzzleType.valueOf(mPuzzleTypeId).getSession(mSessionId).getSolveByPosition(index),
                    PuzzleType.valueOf(mPuzzleTypeId),
                    mSessionId,
                    index);*/
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            onSessionSolvesChanged();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    };
    private final ValueEventListener puzzleTypeObserver = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            onSessionSolvesChanged();
            //TODO
            /*mSession.registerObserver(sessionObserver);*/
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    };
    private ActionMode mActionMode;
    private LinearLayout mResetSubmitLinearLayout;

    public static SolveListFragment newInstance(boolean current,
                                                String puzzleTypeName,
                                                String sessionIndex) {
        SolveListFragment f = new SolveListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION_ID, sessionIndex);
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

    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(
                Intent.EXTRA_TEXT,
                mSession.toString(getActivity(), mPuzzleTypeId,
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
        mPuzzleTypeId = getArguments().getString(ARG_PUZZLETYPE_DISPLAYNAME);
        mSessionId = getArguments().getString(ARG_SESSION_ID);
        mSession = getPuzzleType().getSession(mSessionId);
        //TODO
        /*PuzzleType.registerObserver(puzzleTypeObserver);
        mSession.registerObserver(sessionObserver);*/
        setHasOptionsMenu(true);
    }

    private PuzzleType getPuzzleType() {
        if (mCurrentToggle) {
            return PuzzleType.getCurrent();
        } else {
            return PuzzleType.get(mPuzzleTypeId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_solvelist_share_menuitem:
                share();
                return true;
            case R.id.menu_history_solvelist_delete_menuitem:
                //TODO
               /* if (getPuzzleType().getSortedHistorySessions().size() >= mSessionId) {*/
                getPuzzleType().deleteSession(getPuzzleType().getSession(mSessionId));
                /*} else {
                    NullPointerException e = new NullPointerException(
                            "SolveListFragment onOptionsItemSelected: " +
                                    "delete failed for history session #"
                                    + mSessionId + ", is null");
                    ErrorUtils.showErrorDialog(getActivity(), "Error: ", e, true);
                    ErrorUtils.logCrashlytics(e);
                }*/
                getActivity().finish();
                return true;
            case R.id.menu_solvelist_add_menuitem:
                SolveDialogUtils.createSolveDialog(getActivity(), true, mPuzzleTypeId, mSessionId, null);
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    void updateStats() {
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
                    MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                    builder.content(getString(R.string.reset_warning_message))
                            .title(getString(R.string.reset_warning))
                            .positiveText(R.string.reset)
                            .negativeText(android.R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    getPuzzleType().resetCurrentSession();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            getResources().getText(R.string.session_reset),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                    builder.show();
                }
            });
            Button submit = (Button) v.findViewById(R.id
                    .fragment_solvelist_submit_button);
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                    builder.content(getString(R.string.submit_warning_message))
                            .positiveText(R.string.submit)
                            .negativeText(android.R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    //TODO
                                    /*if (mSession.getNumberOfSolves() == 0) {
                                        Toast.makeText(getActivity().getApplicationContext(),
                                                getResources().getText(R.string.session_no_solves),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        return;
                                    }*/

                                    Toast.makeText(getActivity().getApplicationContext(),
                                            getResources().getText(R.string.session_submitted),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    getPuzzleType().submitCurrentSession(getActivity());
                                }
                            });
                    builder.show();
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
                SolveDialogUtils.createSolveDialog(getActivity(), false, mPuzzleTypeId, mSessionId,
                        ((Solve) mListView.getItemAtPosition(position)).getId());
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
                                        .getItemAtPosition(i), PuzzleType.get(mPuzzleTypeId));
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
        //TODO
        /*mSession.unregisterObserver(sessionObserver);
        PuzzleType.unregisterObserver(puzzleTypeObserver);*/
    }

    void enableResetSubmitButtons(boolean enable) {
        mResetSubmitLinearLayout.setVisibility(enable ? View.VISIBLE : View
                .GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        //getPuzzleType().getHistorySessions().save(getActivity());
        // if (mCurrentToggle) getPuzzleType().saveCurrentSession(getActivity());
    }

    void onSessionSolvesChanged() {
        mSession = getPuzzleType().getSession(mSessionId);
        //TODO
        if (!mCurrentToggle /*&& mSession.getNumberOfSolves() <= 0*/) {
            getPuzzleType().deleteSession(mSession);
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
            //TODO
            /*enableResetSubmitButtons(getPuzzleType().getCurrentSession().getNumberOfSolves() > 0);*/
        } else {
            getActivity().setTitle(PuzzleType.get(mPuzzleTypeId).getSession(mSessionId)
                    .getTimestampString(getActivity()));
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

            String scramble = s.getScramble();
            //TODO
            /*String uiScramble = ErrorUtils.getUiScramble(getActivity(), position, scramble,
                    mSignEnabled, mPuzzleTypeId);*/
            String uiScramble = scramble;

            desc.setText(uiScramble);

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

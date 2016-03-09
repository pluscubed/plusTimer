package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.BasePresenterFragment;
import com.pluscubed.plustimer.base.PresenterFactory;


/**
 * Session tab
 */
public class SolveListFragment extends BasePresenterFragment<SolveListPresenter, SolveListView> implements SolveListView {

    public static final String TAG = "SOLVE_LIST_FRAGMENT";

    private RecyclerView mRecyclerView;
    private SolveListAdapter mSolveListAdapter;
    private TextView mEmptyView;

    private ActionMode mActionMode;
    private LinearLayout mResetSubmitLinearLayout;

    public SolveListAdapter getSolveListAdapter() {
        return mSolveListAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        initSharedPrefs();

        //TODO
        //When Settings change
        //onPuzzleTypeChanged();
    }

    private void initSharedPrefs() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    public SolveListPresenter getPresenter() {
        return mPresenter;
    }


    /*private PuzzleType getPuzzleType() {
        if (mCurrentToggle) {
            return PuzzleType.getCurrent();
        } else {
            return PuzzleType.get(mPuzzleTypeId);
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_solvelist_share_menuitem:
                mPresenter.share();
                return true;
            case R.id.menu_history_solvelist_delete_menuitem:
                //TODO
               /* if (getPuzzleType().getSortedHistorySessions().size() >= mSessionId) {*/
                /*getPuzzleType().deleteSession(getPuzzleType().getSession(mSessionId));*/
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
                mPresenter.onToolbarAddSolvePressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public Activity getContextCompat() {
        return getActivity();
    }

    @Override
    public void setInitialized() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_solvelist, container, false);
        initSharedPrefs();

        mResetSubmitLinearLayout = (LinearLayout)
                v.findViewById(R.id.fragment_solvelist_submit_reset_linearlayout);

        Button reset = (Button) v.findViewById(R.id.fragment_solvelist_reset_button);
        reset.setOnClickListener(view -> {
            getPresenter().onResetButtonClicked();
        });
        Button submit = (Button) v.findViewById(R.id.fragment_solvelist_submit_button);
        submit.setOnClickListener(view -> {
            MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
            builder.content(getString(R.string.submit_warning_message))
                    .positiveText(R.string.submit)
                    .negativeText(android.R.string.cancel)
                    .onPositive((dialog, which) -> {
                        getPresenter().onSubmitButtonClicked();
                    });
            builder.show();
        });

        mRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_solvelist_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mEmptyView = (TextView) v.findViewById(android.R.id.empty);

        /*mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
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
                                //TODO
                                *//*mSession.deleteSolve((Solve) mListView
                                        .getItemAtPosition(i), PuzzleType.get(mPuzzleTypeId));*//*
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
        });*/

        //TODO
        //onPuzzleTypeChanged();
        return v;
    }

    @Override
    public void setAdapter(SolveListAdapter adapter) {
        mSolveListAdapter = adapter;
        mRecyclerView.setAdapter(mSolveListAdapter);
    }

    public void showSessionSubmitted() {
        Toast.makeText(getActivity().getApplicationContext(),
                getResources().getText(R.string.session_submitted),
                Toast.LENGTH_SHORT
        ).show();
    }

    public void showResetWarningDialog() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.content(getString(R.string.reset_warning_message))
                .title(getString(R.string.reset_warning))
                .positiveText(R.string.reset)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> {
                    getPresenter().onResetDialogConfirmed();
                    showSessionResetToast();
                });
        builder.show();
    }

    public void showSessionResetToast() {
        Toast.makeText(getActivity(),
                getResources().getText(R.string.session_reset),
                Toast.LENGTH_SHORT).show();
    }

    public void enableResetSubmitButtons(boolean enable) {
        mResetSubmitLinearLayout.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        //mPresenter.onPause();
    }

    @Override
    protected PresenterFactory<SolveListPresenter> getPresenterFactory() {
        return new SolveListPresenterFactory(getArguments());
    }

    @Override
    protected void onPresenterPrepared(SolveListPresenter presenter) {

    }


    public void showList(boolean show) {
        mRecyclerView.setVisibility(show ? View.VISIBLE : View.GONE);
        mEmptyView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /*void onSessionSolvesChanged() {
        *//*mSession = getPuzzleType().getSession(mSessionId);*//*
        if (!mCurrentToggle *//*&& mSession.getNumberOfSolves() <= 0*//*) {
            getPuzzleType().deleteSession(mSession);
            getActivity().finish();
            return;
        }
        mSolveListAdapter.onSolveListChanged();
        if (mSolveListAdapter.getCount() == 0) {
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
        updateStats();
        if (mCurrentToggle) {
            *//*enableResetSubmitButtons(getPuzzleType().getCurrentSession().getNumberOfSolves() > 0);*//*
        } else {
            *//*getActivity().setTitle(PuzzleType.get(mPuzzleTypeId).getSession(mSessionId)
                    .getTimestampString(getActivity()));*//*
        }
    }*/

    public void scrollRecyclerView(int position) {
        mRecyclerView.scrollToPosition(position);
    }

}

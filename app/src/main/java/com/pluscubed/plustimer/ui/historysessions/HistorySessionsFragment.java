package com.pluscubed.plustimer.ui.historysessions;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.BasePresenterFragment;
import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.SpinnerPuzzleTypeAdapter;

import java.util.List;

/**
 * History SessionList Fragment
 */

public class HistorySessionsFragment extends BasePresenterFragment<HistorySessionsPresenter, HistorySessionsView>
        implements HistorySessionsView {

    private RecyclerView mRecyclerView;
    private HistorySessionsAdapter mAdapter;
    private TextView mEmptyView;
    private SpinnerPuzzleTypeAdapter mPuzzleSpinnerAdapter;
    private Spinner mPuzzleSpinner;
    private List<PuzzleType> mPuzzleSpinnerList;
    private int mPuzzleSpinnerPosition;


    @Override
    protected PresenterFactory<HistorySessionsPresenter> getPresenterFactory() {
        return new HistorySessionsPresenter.Factory();
    }

    @Override
    protected void onPresenterPrepared(HistorySessionsPresenter presenter) {
        mAdapter.onPresenterPrepared(presenter);
    }

    @Override
    protected void onPresenterDestroyed() {
        mAdapter.onPresenterDestroyed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history_sessionlist, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.history_sessions_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new HistorySessionsAdapter(getActivity(), savedInstanceState);
        mRecyclerView.setAdapter(mAdapter);

        mEmptyView = (TextView) v.findViewById(android.R.id.empty);

        return v;
    }

    @Override
    public Activity getContextCompat() {
        return getActivity();
    }

    @Override
    public void showList(boolean show) {
        mRecyclerView.setVisibility(show ? View.VISIBLE : View.GONE);
        mEmptyView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public HistorySessionsAdapterView getHistorySessionsAdapter() {
        return mAdapter;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mAdapter.onSaveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    /*

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSharedPrefs();
        *//*getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView
                .MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id,
                                                  boolean checked) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu
                        .context_solve_or_session_list, menu);
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
                        for (int i = getListView().getCount() - 1; i >= 0;
                             i--) {
                            if (getListView().isItemChecked(i)) {
                                //TODO
                                *//**//*PuzzleType.get(mPuzzleTypeId).deleteSession(
                                        ((Session) getListView().getItemAtPosition(i)).getId());*//**//*
                            }
                        }
                        mode.finish();
                        onSessionListChanged();
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
    }*/


    /*public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_history_sessionlist, menu);

        mPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(
                menu.findItem(R.id.menu_activity_history_sessionlist_puzzletype_spinner));
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        Context themedContext;
        if (activity.getSupportActionBar() != null) {
            themedContext = activity.getSupportActionBar().getThemedContext();
        } else {
            themedContext = activity;
        }
        mPuzzleSpinnerAdapter = new SpinnerPuzzleTypeAdapter(
                getActivity().getLayoutInflater(),
                themedContext
        );
        mPuzzleSpinner.setAdapter(mPuzzleSpinnerAdapter);
        if (mPuzzleSpinnerList != null) {
            mPuzzleSpinnerAdapter.addAll(mPuzzleSpinnerList);
            mPuzzleSpinner.setSelection(mPuzzleSpinnerPosition);
        }

        presenter.onCreateOptionsMenu();
    }

    @Override
    public void initPuzzleSpinner(List<PuzzleType> puzzleTypes, int selectedPosition) {
        mPuzzleSpinnerList = puzzleTypes;
        mPuzzleSpinnerPosition = selectedPosition;

        mPuzzleSpinnerAdapter.clear();
        mPuzzleSpinnerAdapter.addAll(puzzleTypes);
        mPuzzleSpinnerAdapter.notifyDataSetChanged();
        mPuzzleSpinner.setSelection(selectedPosition, true);
        mPuzzleSpinner.setOnItemSelectedListener(new AdapterView
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                presenter.onPuzzleSelected((PuzzleType) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

}

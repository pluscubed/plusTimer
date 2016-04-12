package com.pluscubed.plustimer.ui.currentsessiontimer;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.RecyclerViewUpdate;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class TimeBarRecyclerAdapter extends RecyclerView.Adapter<TimeBarRecyclerAdapter.ViewHolder>
        implements TimeBarRecyclerAdapterView {

    private static final String STATE_SOLVES = "state_solves";
    private static final String STATE_BEST = "state_best";
    private static final String STATE_WORST = "state_worst";
    private static final String STATE_INITIALIZED = "state_initialized";
    private Solve mBest;
    private Solve mWorst;
    private Context mContext;
    private List<Solve> mSolves;
    private boolean mMillisecondsEnabled;

    private CurrentSessionTimerPresenter mPresenter;

    private boolean mInitialized;

    public TimeBarRecyclerAdapter(Context view, Bundle savedInstanceState) {
        mContext = view;

        if (savedInstanceState != null) {
            mBest = savedInstanceState.getParcelable(STATE_BEST);
            mWorst = savedInstanceState.getParcelable(STATE_WORST);
            mSolves = savedInstanceState.getParcelableArrayList(STATE_SOLVES);
            mInitialized = savedInstanceState.getBoolean(STATE_INITIALIZED);
        } else {
            mSolves = new ArrayList<>();
            mInitialized = false;
        }

        updateMillisecondsMode();

        setHasStableIds(true);
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item_solve, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Solve s = mSolves.get(position);
        String timeString = s.getTimeString(mMillisecondsEnabled);
        if (s == mBest || s == mWorst) {
            holder.textView.setText(String.format("(%s)", timeString));
        } else {
            holder.textView.setText(timeString);
        }
    }

    @Override
    public int getItemCount() {
        return mSolves.size();
    }

    @Override
    public long getItemId(int position) {
        return mSolves.get(position).getId().hashCode();
    }

    public void notifyChange(RecyclerViewUpdate mode, Solve solve) {

        switch (mode) {
            case DATA_RESET:
                notifyDataSetChanged();
                break;
            case INSERT:
                mSolves.add(solve);
                notifyDataSetChanged();
                break;
            case REMOVE:
                mSolves.remove(solve);
                notifyDataSetChanged();
                break;
            case SINGLE_CHANGE:
                for (int i = 0; i < mSolves.size(); i++) {
                    Solve foundSolve = mSolves.get(i);
                    if (foundSolve.getId().equals(solve.getId())) {
                        mSolves.set(i, solve);
                        notifyItemChanged(i);
                        break;
                    }
                }
                break;
            case REMOVE_ALL:
                mSolves.clear();
                notifyDataSetChanged();
                break;
        }

        if (mode != RecyclerViewUpdate.REMOVE_ALL) {
            Solve oldBest = mBest;
            Solve oldWorst = mWorst;
            mBest = Utils.getBestSolveOfList(mSolves);
            mWorst = Utils.getWorstSolveOfList(mSolves);

            if (oldBest != null && !oldBest.equals(mBest)) {
                notifyItemChanged(mSolves.indexOf(oldBest));
                notifyItemChanged(mSolves.indexOf(mBest));
            }
            if (oldWorst != null && !oldWorst.equals(mWorst)) {
                notifyItemChanged(mSolves.indexOf(oldWorst));
                notifyItemChanged(mSolves.indexOf(mWorst));
            }
        }
    }

    @Override
    public void scrollRecyclerViewToLast(CurrentSessionTimerView view) {
        if (mSolves.size() >= 1)
            view.scrollRecyclerView(mSolves.size() - 1);
    }

    @Override
    public void updateMillisecondsMode() {
        boolean millisecondsWasEnabled = mMillisecondsEnabled;

        mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(mContext);

        if (mMillisecondsEnabled != millisecondsWasEnabled) {
            notifyItemRangeChanged(0, mSolves.size());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_BEST, mBest);
        outState.putParcelable(STATE_WORST, mWorst);
        outState.putParcelableArrayList(STATE_SOLVES, (ArrayList<Solve>) mSolves);
        outState.putBoolean(STATE_INITIALIZED, mInitialized);
    }

    public void onPresenterPrepared(CurrentSessionTimerPresenter presenter) {
        mPresenter = presenter;
    }

    public void onPresenterDestroyed() {
        mPresenter = null;
    }

    @Override
    public void setSolves(List<Solve> solves) {
        mSolves = solves;
        mInitialized = true;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(TextView v) {
            super(v);
            textView = v;
            textView.setOnClickListener(v1 ->
                    mPresenter.onSolveClicked(mSolves.get(getAdapterPosition()))
            );
        }
    }


}

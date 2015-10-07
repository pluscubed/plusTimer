package com.pluscubed.plustimer.ui;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.currentsession.CurrentSessionTimerView;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;


public class TimeBarRecyclerAdapter
        extends RecyclerView.Adapter<TimeBarRecyclerAdapter.ViewHolder> {

    private final Activity mContext;
    private final CurrentSessionTimerView mView;
    private final List<Solve> mSolves;
    private final Solve[] mBestAndWorstSolves;

    public TimeBarRecyclerAdapter(Activity context, CurrentSessionTimerView view) {
        mContext = context;
        mView = view;
        mBestAndWorstSolves = new Solve[2];
        mSolves = new ArrayList<>();
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
        String timeString = s.getTimeString(PrefUtils.isDisplayMillisecondsEnabled(mContext));
        holder.textView.setText(timeString);
        for (Solve a : mBestAndWorstSolves) {
            if (a == s) {
                holder.textView.setText("(" + timeString + ")");
            }
        }
    }

    @Override
    public int getItemCount() {
        return mSolves.size();
    }

    public void notifyChange(DataSnapshot solveDataSnapshot, Update mode) {
        int oldSize = mSolves.size();

        PuzzleType.getCurrent().getCurrentSession()
                .flatMap(session -> {
                    if (mode != Update.REMOVE) {
                        return session.getSolve(solveDataSnapshot.getKey()).toObservable();
                    } else {
                        return Observable.empty();
                    }
                })
                .defaultIfEmpty(null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(solve -> {
                    //Collections.reverse(mSolves);

                    int solvePosition = 0;
                    if (mode == Update.REMOVE || mode == Update.SINGLE_CHANGE) {
                        for (int i = 0; i < mSolves.size(); i++) {
                            Solve foundSolve = mSolves.get(i);
                            if (foundSolve.getId().equals(solveDataSnapshot.getKey())) {
                                solvePosition = i;
                            }
                        }
                    }

                    switch (mode) {
                        case DATA_RESET:
                            notifyDataSetChanged();
                            if (mSolves.size() >= 1)
                                mView.scrollRecyclerView(mSolves.size() - 1);
                            break;
                        case INSERT:
                            mSolves.add(solve);
                            notifyItemInserted(mSolves.size() - 1);
                            if (mSolves.size() >= 1)
                                mView.scrollRecyclerView(mSolves.size() - 1);
                            break;
                        case REMOVE:
                            //TODO
                            mSolves.remove(solvePosition);
                            notifyItemRemoved(solvePosition);
                            break;
                        case SINGLE_CHANGE:
                            mSolves.set(solvePosition, solve);
                            notifyItemChanged(solvePosition);
                            break;
                        case REMOVE_ALL:
                            notifyItemRangeRemoved(0, oldSize);
                            break;
                    }

                    Solve oldBest = mBestAndWorstSolves[0];
                    Solve oldWorst = mBestAndWorstSolves[1];
                    Solve newBest = Utils.getBestSolveOfList(mSolves);
                    Solve newWorst = Utils.getWorstSolveOfList(mSolves);
                    mBestAndWorstSolves[0] = newBest;
                    mBestAndWorstSolves[1] = newWorst;

                    if (mode != Update.DATA_RESET && mode != Update.REMOVE_ALL) {
                        if (oldBest != null && !oldBest.equals(newBest)) {
                            notifyItemChanged(mSolves.indexOf(oldBest));
                            notifyItemChanged(mSolves.indexOf(newBest));
                        }
                        if (oldWorst != null && !oldWorst.equals(newWorst)) {
                            notifyItemChanged(mSolves.indexOf(oldWorst));
                            notifyItemChanged(mSolves.indexOf(newWorst));
                        }
                    }
                });
    }

    public enum Update {
        INSERT, REMOVE, REMOVE_ALL, SINGLE_CHANGE, DATA_RESET
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(TextView v) {
            super(v);
            textView = v;
            textView.setOnClickListener(v1 -> SolveDialogUtils.createSolveDialog(
                    mContext,
                    false,
                    PuzzleType.getCurrent().getId(),
                    PuzzleType.getCurrent().getCurrentSessionId(),
                    mSolves.get(getAdapterPosition()).getId()
            ));
        }
    }


}

package com.pluscubed.plustimer.ui.currentsessiontimer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;


public class TimeBarRecyclerAdapter
        extends RecyclerView.Adapter<TimeBarRecyclerAdapter.ViewHolder> {

    //TODO: WeakReference? Check for leaks
    private final CurrentSessionTimerView mView;
    private final Solve[] mBestAndWorstSolves;
    private List<Solve> mSolves;
    private boolean mMillisecondsEnabled;

    public TimeBarRecyclerAdapter(CurrentSessionTimerView view) {
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
        String timeString = s.getTimeString(mMillisecondsEnabled);
        holder.textView.setText(timeString);
        for (Solve a : mBestAndWorstSolves) {
            if (a == s) {
                holder.textView.setText("(" + timeString + ")");
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mSolves.size();
    }

    public void initialize(List<Solve> solves) {
        mSolves = new ArrayList<>(solves);

        notifyChange(null, RecyclerViewUpdate.DATA_RESET);
    }

    private Observable<Solve> getSolve(DataSnapshot snapshot) {
        if (snapshot != null) {
            return Session.getSolve(snapshot.getKey()).toObservable();
        } else {
            return Observable.empty();
        }
    }

    public void notifyChange(DataSnapshot sessionSolveDataSnapshot, RecyclerViewUpdate mode) {
        mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(mView.getContextCompat());

        int oldSize = mSolves.size();

        getSolve(sessionSolveDataSnapshot)
                .defaultIfEmpty(null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(solve -> {
                    //Collections.reverse(mSolves);

                    int solvePosition = 0;
                    if (mode == RecyclerViewUpdate.REMOVE || mode == RecyclerViewUpdate.SINGLE_CHANGE) {
                        for (int i = 0; i < mSolves.size(); i++) {
                            Solve foundSolve = mSolves.get(i);
                            if (foundSolve.getId().equals(sessionSolveDataSnapshot.getKey())) {
                                solvePosition = i;
                                break;
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

                    if (mode != RecyclerViewUpdate.DATA_RESET && mode != RecyclerViewUpdate.REMOVE_ALL) {
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

    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(TextView v) {
            super(v);
            textView = v;
            textView.setOnClickListener(v1 -> SolveDialogUtils.createSolveDialog(
                    mView.getContextCompat(),
                    false,
                    PuzzleType.getCurrent().getId(),
                    PuzzleType.getCurrent().getCurrentSessionId(),
                    mSolves.get(getAdapterPosition()).getId()
            ));
        }
    }


}

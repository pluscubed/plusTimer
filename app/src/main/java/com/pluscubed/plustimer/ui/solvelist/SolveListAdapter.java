package com.pluscubed.plustimer.ui.solvelist;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class SolveListAdapter extends RecyclerView.Adapter<SolveListAdapter.ViewHolder> {

    private static final int HEADER_VIEWTYPE = 2;
    private static final int HEADER_ID = -1;

    private final Solve[] mBestAndWorstSolves;
    private SolveListView mView;
    private List<Solve> mSolves;
    private String mPuzzleTypeId;
    private String mSessionId;
    private boolean mSignEnabled;
    private boolean mMillisecondsEnabled;

    public SolveListAdapter(SolveListView view) {
        mBestAndWorstSolves = new Solve[2];
        mSolves = new ArrayList<>();
        mView = view;

        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == HEADER_VIEWTYPE) {
            view = LayoutInflater.from(mView.getContextCompat()).inflate(R.layout.list_item_solvelist_header, parent, false);
        } else {
            view = LayoutInflater.from(mView.getContextCompat()).inflate(R.layout.list_item_solvelist, parent, false);
        }
        return new ViewHolder(view, viewType == HEADER_VIEWTYPE);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position > 0) {
            position = position - 1;

            Solve s = mSolves.get(position);
            String timeString = s.getTimeString(mMillisecondsEnabled);
            holder.textView.setText(timeString);

            for (Solve solve : mBestAndWorstSolves) {
                if (s == solve) {
                    holder.textView.setText("(" + timeString + ")");
                    break;
                }
            }

            String uiScramble = Utils.getUiScramble(s.getScramble(), mSignEnabled, mPuzzleTypeId);

            holder.desc.setText(uiScramble);

        } else {
            //TODO Header stats
            holder.header.setText(
                /*mSession.toString(
                        getActivity(),
                        getPuzzleType().toString(),
                        mCurrentToggle,
                        false,
                        mMillisecondsEnabled,
                        mSignEnabled)*/""
            );
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return HEADER_VIEWTYPE;
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return mSolves.size() + 1;
    }

    @Override
    public long getItemId(int position) {
        return position == 0 ? HEADER_ID : mSolves.get(position - 1).getId().hashCode();
    }

    public void notifyChange(String puzzleTypeId, String sessionId,
                             Solve solve, RecyclerViewUpdate mode) {

        mSignEnabled = PrefUtils.isSignEnabled(mView.getContextCompat());
        mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(mView.getContextCompat());
        mPuzzleTypeId = puzzleTypeId;
        mSessionId = sessionId;

        int oldSize = mSolves.size();

        //Collections.reverse(mSolves);

        switch (mode) {
            case DATA_RESET:
                notifyDataSetChanged();
                if (mSolves.size() >= 1)
                    mView.scrollRecyclerView(0);
                break;
            case INSERT:
                mSolves.add(0, solve);
                notifyItemInserted(0);
                if (mSolves.size() >= 1)
                    mView.scrollRecyclerView(0);
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
                        notifyItemChanged(i + 1);
                        break;
                    }
                }
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
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public TextView desc;
        public TextView header;

        public ViewHolder(View v, boolean header) {
            super(v);
            if (header) {
                this.header = (TextView) v.findViewById(R.id.solvelist_header_stats_textview);
            } else {
                textView = (TextView) v.findViewById(R.id.list_item_solvelist_title_textview);
                desc = (TextView) v.findViewById(R.id.list_item_solvelist_desc_textview);

                v.setOnClickListener(view -> {
                    SolveDialogUtils.createSolveDialog(
                            mView.getContextCompat(),
                            false,
                            mPuzzleTypeId,
                            mSessionId,
                            mSolves.get(getAdapterPosition() - 1)
                    );

                });
            }
        }
    }


}

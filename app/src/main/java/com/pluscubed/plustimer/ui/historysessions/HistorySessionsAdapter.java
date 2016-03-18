package com.pluscubed.plustimer.ui.historysessions;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.LineData;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class HistorySessionsAdapter extends RecyclerView.Adapter<HistorySessionsAdapter.ViewHolder>
        implements HistorySessionsAdapterView {

    private static final int HEADER_VIEWTYPE = 2;
    private static final int HEADER_ID = -1;
    private static final String STATE_SESSIONS = "state_sessions";
    private static final String STATE_STATS = "state_stats";

    private final Context mContext;

    private boolean mMillisecondsEnabled;
    private String mStats;
    private LineData mLineChartData;

    private HistorySessionsPresenter mPresenter;

    private List<Session> mSessions;

    public HistorySessionsAdapter(Context context, Bundle savedInstanceState) {
        mContext = context;

        if (savedInstanceState == null) {
            mSessions = new ArrayList<>();
        } else {
            mSessions = savedInstanceState.getParcelableArrayList(STATE_SESSIONS);
            mStats = savedInstanceState.getString(STATE_STATS);
        }

        mLineChartData = new LineData();

        setHasStableIds(true);
    }

    @Override
    public void setSessions(List<Session> sessions) {
        mSessions = sessions;
    }

    @Override
    public void setStats(String string) {
        mStats = string;
    }

    @Override
    public void setLineData(LineData data) {
        mLineChartData = data;
    }

    @Override
    public void notifyHeaderChanged() {
        notifyItemChanged(0);
    }


    @Override
    public void setMillisecondsEnabled(boolean millisecondsEnabled) {
        mMillisecondsEnabled = millisecondsEnabled;
    }

    @Override
    public void onPresenterPrepared(HistorySessionsPresenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void onPresenterDestroyed() {
        mPresenter = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        //TODO: TransactionTooLargeException is possible, but this is linked to the more serious problem of how much data is stored in memory in general
        outState.putParcelableArrayList(STATE_SESSIONS, (ArrayList<Session>) mSessions);
        outState.putString(STATE_STATS, mStats);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER_VIEWTYPE : 0;
    }

    @Override
    public long getItemId(int position) {
        return position == 0 ? HEADER_ID : mSessions.get(position - 1).getId().hashCode();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(
                viewType == HEADER_VIEWTYPE ?
                        R.layout.list_item_sessions_header :
                        R.layout.list_item_single_line, parent, false);

        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position == 0) {
            holder.textView.setText(mStats);

            holder.graph.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
            holder.graph.getXAxis().setAvoidFirstLastClipping(true);
            holder.graph.getAxisLeft().setAxisMinValue(0);
            holder.graph.setDescription("");
            holder.graph.getAxisLeft().setValueFormatter(
                    (value, yAxis) -> Utils.timeStringFromNs((long) value, mMillisecondsEnabled));
            holder.graph.getAxisRight().setEnabled(false);

            holder.graph.invalidate();

            holder.graph.setData(mLineChartData);
            holder.graph.getXAxis().setLabelsToSkip(mLineChartData.getXValCount() - 2);

        } else {
            String timestamp = mSessions.get(position - 1).getTimestampString(mContext).toBlocking().value();
            holder.textView.setText(timestamp);
        }

    }

    @Override
    public int getItemCount() {
        return 1 + mSessions.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public LineChart graph;

        public ViewHolder(View v, int viewType) {
            super(v);


            if (viewType == HEADER_VIEWTYPE) {
                graph = (LineChart) v.findViewById(R.id.history_sessions_linechart);
                textView = (TextView) v.findViewById(R.id.history_sessions_stats);

            } else {
                textView = (TextView) v.findViewById(android.R.id.text1);
                v.setOnClickListener(v1 -> {
                    mPresenter.onSessionClicked(mSessions.get(getAdapterPosition() - 1));
                });
            }

        }
    }
}

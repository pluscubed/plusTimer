package com.pluscubed.plustimer.ui.historysessions;

import android.os.Bundle;

import com.github.mikephil.charting.data.LineData;
import com.pluscubed.plustimer.model.Session;

import java.util.List;

public interface HistorySessionsAdapterView {

    void setSessions(List<Session> sessions);

    void notifyDataSetChanged();

    void setStats(String string);

    void setLineData(LineData data);

    void notifyHeaderChanged();

    void setMillisecondsEnabled(boolean millisecondsEnabled);

    void onSaveInstanceState(Bundle outState);

    void onPresenterPrepared(HistorySessionsPresenter presenter);

    void onPresenterDestroyed();
}

package com.pluscubed.plustimer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;

/**
 * History Fragment
 */
public class HistoryFragment extends ListFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        try {
            setListAdapter(new SessionListAdapter());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), HistorySessionActivity.class);
        i.putExtra(HistorySessionActivity.EXTRA_HISTORY_SESSION_POSITION, position);
        startActivity(i);
    }

    public class SessionListAdapter extends ArrayAdapter<Session> {
        SessionListAdapter() throws IOException {
            super(getActivity(), android.R.layout.simple_list_item_1, PuzzleType.sCurrentPuzzleType.getHistorySessions(getActivity()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            Session session = getItem(position);
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(session.getTimestampStringOfLastSolve(getActivity()));
            return convertView;
        }
    }

}

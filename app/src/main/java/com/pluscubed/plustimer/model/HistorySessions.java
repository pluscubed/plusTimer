package com.pluscubed.plustimer.model;

import android.content.Context;

import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Container for history sessions
 */
public class HistorySessions {

    private String mFilename;

    private List<Session> mHistorySessionsList;

    /**
     * Constructs a new HistorySessions that will save to a file.
     *
     * @param filename the filename to use for saving and loading
     */
    public HistorySessions(String filename) {
        mFilename = filename;
    }

    public void setFilename(String filename) {
        mFilename = filename;
    }

    /**
     * Deletes a session according to its index and save.
     */
    public void deleteSession(int index, Context context) {
        mHistorySessionsList.remove(index);
        Utils.saveSessionListToFile(context, mFilename, mHistorySessionsList);
    }

    /**
     * Deletes a session matching the parameter and save.
     */
    public void deleteSession(Session session, Context context) {
        mHistorySessionsList.remove(session);
        Utils.saveSessionListToFile(context, mFilename, mHistorySessionsList);
    }

    /**
     * Adds the Session to the history list and save.
     */
    public void addSession(Session session, Context context) {
        mHistorySessionsList.add(new Session(session));
        Utils.saveSessionListToFile(context, mFilename, mHistorySessionsList);
    }

    /**
     * Save the list to a file.
     */
    public void save(Context context) {
        Utils.saveSessionListToFile(context, mFilename, mHistorySessionsList);
    }

    /**
     * Gets a copy of the list.
     *
     * @return a copy of the history sessions
     */
    public List<Session> getList() {
        return new ArrayList<>(mHistorySessionsList);
    }

    /**
     * Load up the history sessions stored in the list. If the file doesn't
     * exist, create an empty
     * list.
     */
    public void init(Context context) {
        mHistorySessionsList = Utils.getSessionListFromFile(context, mFilename);
    }

    public void sort() {
        Collections.sort(mHistorySessionsList, new Comparator<Session>() {
            @Override
            public int compare(Session lhs, Session rhs) {
                if (lhs.getTimestamp() > rhs.getTimestamp()) {
                    return 1;
                } else if (lhs.getTimestamp() < rhs.getTimestamp()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }
}

package com.pluscubed.plustimer;

import java.util.ArrayList;

/**
 * Session data
 */
public class Session {
    private static Session sSession;
    private ArrayList<Solve> mSolves;

    public Session() {
        mSolves = new ArrayList<Solve>();
    }

    public static Session get() {
        if (sSession == null) {
            sSession = new Session();
        }

        return sSession;
    }

    public void addSolve(Solve s) {
        mSolves.add(s);
    }


    public Solve getLastSolve() {
        try {
            return mSolves.get(mSolves.size() - 1);
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<Solve> getSolves() {
        return mSolves;
    }

}

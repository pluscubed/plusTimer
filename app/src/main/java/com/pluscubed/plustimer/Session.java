package com.pluscubed.plustimer;

import java.util.ArrayList;

/**
 * Session data
 */
public class Session {
    private ArrayList<Solve> mSolves;

    public Session() {
        mSolves = new ArrayList<Solve>();
    }

    public ArrayList<Solve> getSolves() {
        return mSolves;
    }

    public void addSolve(Solve s) {
        mSolves.add(s);
    }

    public int numberOfSolves() {
        return mSolves.size();
    }

    public String getLatestSolveTime() {
        return mSolves.get(mSolves.size() - 1).getTime();
    }


}

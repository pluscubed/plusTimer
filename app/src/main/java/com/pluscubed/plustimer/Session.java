package com.pluscubed.plustimer;

import java.util.ArrayList;
import java.util.Collections;

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

    public int getNumberOfSolves() {
        return mSolves.size();
    }

    public Solve getLatestSolve() {
        return mSolves.get(mSolves.size() - 1);
    }

    public long getCurrentAverageOf(int number) {
        if (number >= 5) {
            long sum = 0;
            ArrayList<Long> times = new ArrayList<Long>();

            for (int i = 1; i < number + 1; i++) {
                times.add(mSolves.get(mSolves.size() - i).getTime());
            }
            times.remove(times.indexOf(Collections.min(times)));
            times.remove(times.indexOf(Collections.max(times)));

            for (Long l : times) {
                sum += l;
            }

            return sum / (number - 2L);
        }
        return 0;
    }


    public long getMean() {
        long sum = 0;
        for (Solve i : mSolves) {
            sum += i.getTime();
        }
        return sum / mSolves.size();
    }
}

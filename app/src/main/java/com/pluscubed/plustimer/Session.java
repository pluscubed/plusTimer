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
        return new ArrayList<Solve>(mSolves);
    }

    public void addSolve(Solve s) {
        mSolves.add(s);
    }

    public int getNumberOfSolves() {
        return mSolves.size();
    }

    public ArrayList<Long> getListTimeTwoNdnf(ArrayList<Solve> solveList) {
        ArrayList<Long> timeTwo = new ArrayList<Long>();
        for (Solve i : solveList) {
            if (!(i.getPenalty() == Solve.Penalty.DNF))
                timeTwo.add(i.getTimeTwo());
        }
        return timeTwo;
    }

    public Solve getLatestSolve() {
        return mSolves.get(mSolves.size() - 1);
    }

    public Solve getSolveByPosition(int position) {
        return mSolves.get(position);
    }

    public String getStringCurrentAverageOf(int number) {
        if (number >= 5) {
            long sum = 0;
            ArrayList<Solve> solves = new ArrayList<Solve>();

            int dnfcount = 0;

            for (int i = 1; i < number + 1; i++) {
                Solve x = mSolves.get(mSolves.size() - i);
                solves.add(x);
                if (x.getPenalty() == Solve.Penalty.DNF)
                    dnfcount++;
            }

            if (dnfcount < 2) {
                solves.remove(getBestSolve(solves));
                solves.remove(getWorstSolve(solves));
                for (Solve i : solves) {
                    sum += i.getTimeTwo();
                }
                return Solve.timeStringFromLong(sum / (number - 2L));
            } else {
                return "DNF";
            }
        }
        return "";
    }

    public Solve getBestSolve(ArrayList<Solve> solveList) {
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            ArrayList<Long> times = getListTimeTwoNdnf(solveList);
            if (times.size() > 0) {
                long bestTimeTwo = Collections.min(times);
                for (Solve i : solveList) {
                    if (!(i.getPenalty() == Solve.Penalty.DNF) && i.getTimeTwo() == bestTimeTwo)
                        return i;
                }

            }
        }
        return null;
    }

    public Solve getWorstSolve(ArrayList<Solve> solveList) {
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            for (Solve i : solveList) {
                if (i.getPenalty() == Solve.Penalty.DNF) {
                    return i;
                }
            }
            ArrayList<Long> times = getListTimeTwoNdnf(solveList);
            if (times.size() > 0) {
                long worstTimeTwo = Collections.max(times);
                for (Solve i : solveList) {
                    if (i.getTimeTwo() == worstTimeTwo)
                        return i;
                }
            }
        }
        return null;
    }

    public String getStringMean() {
        long sum = 0;
        boolean dnf = false;
        for (Solve i : mSolves) {
            if (!(i.getPenalty() == Solve.Penalty.DNF)) {
                sum += i.getTimeTwo();
            } else
                dnf = true;
        }
        if (!dnf)
            return Solve.timeStringFromLong(sum / mSolves.size());
        else
            return "DNF";
    }

    public void deleteSolve(int position) {
        mSolves.remove(position);
    }

}

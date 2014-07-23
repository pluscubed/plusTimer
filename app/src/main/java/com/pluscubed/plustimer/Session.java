package com.pluscubed.plustimer;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Session data
 */
public class Session {
    private ArrayList<Solve> mSolves;

    public Session() {
        mSolves = new ArrayList<Solve>();
    }

    public static List<Long> getListTimeTwoNdnf(List<Solve> solveList) {
        ArrayList<Long> timeTwo = new ArrayList<Long>();
        for (Solve i : solveList) {
            if (!(i.getPenalty() == Solve.Penalty.DNF))
                timeTwo.add(i.getTimeTwo());
        }
        return timeTwo;
    }

    public static Solve getBestSolve(List<Solve> solveList) {
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            List<Long> times = getListTimeTwoNdnf(solveList);
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

    public static Solve getWorstSolve(ArrayList<Solve> solveList) {
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            for (Solve i : solveList) {
                if (i.getPenalty() == Solve.Penalty.DNF) {
                    return i;
                }
            }
            List<Long> times = getListTimeTwoNdnf(solveList);
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

    public ArrayList<Solve> getSolves() {
        return new ArrayList<Solve>(mSolves);
    }

    public void addSolve(Solve s) {
        mSolves.add(s);
    }

    public int getNumberOfSolves() {
        return mSolves.size();
    }

    public Solve getLastSolve() {
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
                solves.remove(getBestSolve(new ArrayList<Solve>(solves)));
                solves.remove(getWorstSolve(new ArrayList<Solve>(solves)));
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

    public long getAverageOf(List<Solve> list, int number) {
        if (number >= 5 && list.size() == number) {
            long sum = 0;
            ArrayList<Solve> solves = new ArrayList<Solve>(list);

            int dnfcount = 0;

            for (Solve s : list) {
                if (s.getPenalty() == Solve.Penalty.DNF) {
                    dnfcount++;
                }
            }

            if (dnfcount < 2) {
                solves.remove(getBestSolve(new ArrayList<Solve>(solves)));
                solves.remove(getWorstSolve(new ArrayList<Solve>(solves)));
                for (Solve i : solves) {
                    sum += i.getTimeTwo();
                }
                return sum / (number - 2L);
            } else {
                return Long.MAX_VALUE;
            }
        }
        return -2;
    }

    public String getStringBestAverageOf(int number) {
        long bestAverage = -1;
        for (int i = 0; mSolves.size() - (number + i) >= 0; i++) {
            long average = getAverageOf(mSolves.subList(mSolves.size() - (number + i), mSolves.size() - i), number);
            if (average != -2) {
                if (average < bestAverage || bestAverage == -1) {
                    bestAverage = average;
                }
            }
        }
        if (bestAverage != -1 && bestAverage != Long.MAX_VALUE) {
            return Solve.timeStringFromLong(bestAverage);
        } else if (bestAverage == Long.MAX_VALUE) {
            return "DNF";
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

    public String getTimestampStringOfLastSolve(Context context) {
        return Solve.timeDateStringFromTimestamp(context, getLastSolve().getTimestamp());
    }

    public void deleteSolve(int position) {
        mSolves.remove(position);
    }

}

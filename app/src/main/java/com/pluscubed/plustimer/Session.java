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

    public static Solve getBestSolve(List<Solve> list) {
        List<Solve> solveList = new ArrayList<Solve>(list);
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
            return solveList.get(solveList.size() - 1);
        }
        return null;
    }

    public static Solve getWorstSolve(List<Solve> list) {
        List<Solve> solveList = new ArrayList<Solve>(list);
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

    public List<Solve> getSolves() {
        return new ArrayList<Solve>(Collections.unmodifiableList(mSolves));
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
        if (number > 2 && list.size() == number) {
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

    public void deleteSolve(Solve i) {
        mSolves.remove(i);
    }

    public String toString(Context context, String puzzleTypeDisplayName, boolean current, boolean displaySolves) {
        StringBuilder s = new StringBuilder();
        if (displaySolves) {
            s.append(puzzleTypeDisplayName).append("\n\n");
        }
        s.append(context.getString(R.string.number_solves)).append(getNumberOfSolves());
        if (getNumberOfSolves() > 0) {
            s.append("\n").append(context.getString(R.string.best)).append(Session.getBestSolve(mSolves).getDescriptiveTimeString()).append("\n");
            s.append(context.getString(R.string.worst)).append(Session.getWorstSolve(mSolves).getDescriptiveTimeString()).append("\n");
            s.append(context.getString(R.string.mean)).append(getStringMean());
            if (getNumberOfSolves() > 2) {
                s.append("\n").append(context.getString(R.string.average)).append(Solve.timeStringFromLong(getAverageOf(mSolves, mSolves.size())));

                int[] averages = {1000, 100, 50, 12, 5};
                for (int i : averages) {
                    if (getNumberOfSolves() >= i) {
                        if (current) {
                            s.append("\n").append(context.getString(R.string.cao)).append(i).append(": ").append(getStringCurrentAverageOf(i));
                        } else {
                            s.append("\n").append(context.getString(R.string.lao)).append(i).append(": ").append(getStringCurrentAverageOf(i));
                        }
                        s.append("\n").append(context.getString(R.string.bao)).append(i).append(": ").append(getStringBestAverageOf(i));
                    }
                }
            }
            if (displaySolves) {
                s.append("\n\n");
                int c = 1;
                for (Solve i : mSolves) {
                    s.append(c).append(". ").append(i.getDescriptiveTimeString()).append("\n")
                            .append("     ").append(Solve.timeDateStringFromTimestamp(context, i.getTimestamp())).append("\n")
                            .append("     ").append(i.getScrambleAndSvg().scramble).append("\n\n");
                    c++;
                }
            }
        }
        return s.toString();
    }

}

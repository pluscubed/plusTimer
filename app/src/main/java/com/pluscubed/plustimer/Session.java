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

    public Solve getSolve(int position) {
        return mSolves.get(position);
    }

    public String getStringCurrentAverageOf(int number) {
        if (number >= 5) {
            long sum = 0;
            ArrayList<Solve> solves = new ArrayList<Solve>();

            int dnfcount=0;

            for (int i = 1; i < number + 1; i++) {
                Solve x=mSolves.get(mSolves.size() - i);
                if(!x.isDnf())
                    solves.add(x);
                else
                    dnfcount++;
            }

            if (dnfcount<2){
                ArrayList<Solve> invalid= getBestWorstDNFSolves(solves);
                solves.removeAll(invalid);
                for (Solve i : solves) {
                    sum += i.getTime();
                }
                return TimerFragment.convertNanoToTime(sum / (number - 2L));
            }else{
                return "DNF";
            }
        }
        return "";
    }

    public ArrayList<Solve> getBestWorstDNFSolves(ArrayList<Solve> solveList){
        ArrayList<Solve> best = new ArrayList<Solve>();
        ArrayList<Solve> worst = new ArrayList<Solve>();
        ArrayList<Long> times = new ArrayList<Long>();
        for (Solve i : solveList) {
            if (!i.isDnf())
                times.add(i.getTime());
            else
                worst.add(i);
        }
        if(times.size()>0) {
            long tempWorst = Collections.max(times);
            long tempBest = Collections.min(times);
            for (Solve i : solveList) {
                if (!i.isDnf()&&i.getTime() == tempBest)
                    best.add(i);
            }

            if (worst.size() == 0) {
                for (Solve i : solveList) {
                    if (i.getTime() == tempWorst)
                        worst.add(i);
                }
            }
        }
        best.addAll(worst);
        return best;
    }

    public String getMean() {
        long sum = 0;
        for (Solve i : mSolves) {
            sum += i.getTime();
        }
        return TimerFragment.convertNanoToTime( sum / mSolves.size());
    }

    public void deleteSolve(int position) {
        mSolves.remove(position);
    }

}

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

    public ArrayList<Long> getListTimeTwoNdnf(){
        ArrayList<Long> timeTwo=new ArrayList<Long>();
        for(Solve i:mSolves){
            if(!i.isDnf())
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

    public ArrayList<Solve> getNdnfSolvesWithTimeTwo(long time){
        ArrayList<Solve> solves=new ArrayList<Solve>();
        for (Solve i:mSolves){
            if(!i.isDnf()&&time==i.getTimeTwo()){
                solves.add(i);
            }
        }
        return solves;
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
                ArrayList<Solve> invalid= getBestAndWorstSolves(solves);
                solves.removeAll(invalid);
                for (Solve i : solves) {
                    sum += i.getTimeTwo();
                }
                return Solve.timeStringFromLong(sum / (number - 2L));
            }else{
                return "DNF";
            }
        }
        return "";
    }

    public ArrayList<Solve> getBestAndWorstSolves(ArrayList<Solve> solveList){
        ArrayList<Solve> best = new ArrayList<Solve>();
        ArrayList<Solve> worst = new ArrayList<Solve>();
        ArrayList<Long> times = getListTimeTwoNdnf();
        for (Solve i : solveList) {
            if (i.isDnf()) {
                if(worst.size()==0)
                    worst.add(i);
                else
                    worst.set(0, i);
            }
        }
        if(times.size()>0) {
            long tempWorst = Collections.max(times);
            long tempBest = Collections.min(times);
            for (Solve i : solveList) {
                if (!i.isDnf()&&i.getTimeTwo() == tempBest)
                    best.add(i);
            }

            if (worst.size() == 0) {
                for (Solve i : solveList) {
                    if (i.getTimeTwo() == tempWorst)
                        worst.add(i);
                }
            }
        }
        best.addAll(worst);
        return best;
    }

    public String getStringMean() {
        long sum = 0;
        for (Solve i : mSolves) {
            sum += i.getTimeTwo();
        }
        return Solve.timeStringFromLong(sum / mSolves.size());
    }

    public void deleteSolve(int position) {
        mSolves.remove(position);
    }

}

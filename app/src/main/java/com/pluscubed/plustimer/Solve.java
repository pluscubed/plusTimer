package com.pluscubed.plustimer;

/**
 * solve times data object
 */
public class Solve {
    private ScrambleAndSvg mScrambleAndSvg;

    private long mRawTime;
    private boolean dnf;
    private boolean plusTwo;

    public static String timeStringFromLong(long nano) {
        int minutes = (int) ((nano / (60 * 1000000000L)) % 60);
        int hours = (int) ((nano / (3600 * 1000000000L)) % 24);
        float seconds = (nano / 1000000000F) % 60;

        if (hours != 0) {
            return String.format("%d:%02d:%06.3f", hours, minutes, seconds);
        } else if (minutes != 0) {
            return String.format("%d:%06.3f", minutes, seconds);
        } else {
            return String.format("%.3f", seconds);
        }

    }

    public Solve(ScrambleAndSvg scramble, long time) {
        this.mScrambleAndSvg = scramble;
        this.mRawTime = time;
        dnf=false;
        plusTwo=false;
    }

    public ScrambleAndSvg getScrambleAndSvg() {
        return mScrambleAndSvg;
    }

    public long getTimeTwo() {
        if(plusTwo){
            return mRawTime +2000000000L;
        }
        return mRawTime;
    }

    public String getTimeString(){
        if(dnf)
            return "DNF";
        if(plusTwo)
            return timeStringFromLong(mRawTime+2000000000L)+"+";
        return timeStringFromLong(mRawTime);
    }

    public String getDescriptiveTimeString(){
        if(dnf)
            return "DNF("+timeStringFromLong(mRawTime)+")";
        if(plusTwo)
            return timeStringFromLong(mRawTime)+"+2";
        return timeStringFromLong(mRawTime);
    }

    public void setRawTime(long time) {
        this.mRawTime = time;
    }

    public void setDnf(boolean dnf) {
        if(dnf)
            this.plusTwo=false;
        this.dnf = dnf;
    }

    public boolean isPlusTwo() {
        return plusTwo;
    }

    public boolean isDnf() {
        return dnf;
    }

    public void setPlusTwo(boolean plusTwo){
        if(plusTwo)
            this.dnf=false;
        this.plusTwo=plusTwo;
    }

}

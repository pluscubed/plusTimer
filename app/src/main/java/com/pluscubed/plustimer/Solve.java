package com.pluscubed.plustimer;

/**
 * solve times data object
 */
public class Solve {
    private ScrambleAndSvg scrambleAndSvg;
    private long time;
    private boolean dnf;
    private boolean plusTwo;

    public Solve(ScrambleAndSvg scramble, long time) {
        this.scrambleAndSvg = scramble;
        this.time = time;
        dnf=false;
        plusTwo=false;
    }

    public ScrambleAndSvg getScrambleAndSvg() {
        return scrambleAndSvg;
    }

    public long getTime() {
        if(plusTwo){
            return time+2000000000L;
        }
        return time;
    }

    public String getTimeString(){
        if(dnf)
            return "DNF";
        if(plusTwo)
            return TimerFragment.convertNanoToTime(time+2000000000L)+"+";
        return TimerFragment.convertNanoToTime(time);
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setDnf(boolean dnf) {
        this.dnf = dnf;
    }

    public boolean isPlusTwo() {
        return plusTwo;
    }

    public boolean isDnf() {
        return dnf;
    }

    public void setPlusTwo(boolean plusTwo){
        this.plusTwo=plusTwo;
    }

}

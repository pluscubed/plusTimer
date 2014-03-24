package com.pluscubed.plustimer;

/**
 * solve times data object
 */
public class Solve {
    private String scramble;
    private String time;

    public Solve(String scramble, String time) {
        this.scramble = scramble;
        this.time = time;
    }

    public String getScramble() {
        return scramble;
    }

    public void setScramble(String scramble) {
        this.scramble = scramble;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}

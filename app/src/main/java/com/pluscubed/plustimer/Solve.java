package com.pluscubed.plustimer;

/**
 * solve times data object
 */
public class Solve {
    private String scramble;
    private String time;
    private String penalty;

    public Solve(String scramble, String time, String penalty) {
        this.scramble = scramble;
        this.time = time;
        this.penalty = penalty;
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

    public String getPenalty() {
        return penalty;
    }

    public void setPenalty(String penalty) {
        this.penalty = penalty;
    }
}

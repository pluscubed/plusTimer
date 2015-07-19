package com.pluscubed.plustimer.sql;

import android.provider.BaseColumns;

public class SolveDbEntry implements BaseColumns {
    /* Inner class that defines the table contents */
    public static final String TABLE_NAME = "solves";
    public static final String COLUMN_NAME_PUZZLETYPE_NAME = "puzzletype";
    public static final String COLUMN_NAME_SESSION_ID = "session_index";
    public static final String COLUMN_NAME_SCRAMBLE = "scramble";
    public static final String COLUMN_NAME_PENALTY = "penalty";
    public static final String COLUMN_NAME_TIME = "time";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
}

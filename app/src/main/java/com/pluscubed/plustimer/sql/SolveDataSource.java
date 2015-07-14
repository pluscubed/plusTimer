package com.pluscubed.plustimer.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;

/**
 * Solve Data Source
 */
public class SolveDataSource {

    public static final String SESSION_SOLVES_ORDER = SolveDbEntry.COLUMN_NAME_SESSION_INDEX + ", " + SolveDbEntry.COLUMN_NAME_TIMESTAMP;
    private SQLiteDatabase mDatabase;
    private SolveDbHelper mDbHelper;

    public SolveDataSource(Context context) {
        mDbHelper = new SolveDbHelper(context);
    }

    @NonNull
    public Session loadSessionForPuzzleType(PuzzleType puzzleType, int sessionIndex) {
        Cursor cursor = mDatabase.query(
                SolveDbEntry.TABLE_NAME,
                new String[]{
                        SolveDbEntry.COLUMN_NAME_SCRAMBLE,
                        SolveDbEntry.COLUMN_NAME_PENALTY,
                        SolveDbEntry.COLUMN_NAME_TIME,
                        SolveDbEntry.COLUMN_NAME_TIMESTAMP
                },
                SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " = ? AND " + SolveDbEntry.COLUMN_NAME_SESSION_INDEX + " = ?",
                new String[]{puzzleType.name(), String.valueOf(sessionIndex)},
                null, null,
                SESSION_SOLVES_ORDER);

        Session session = new Session();
        if (cursor.moveToFirst()) {
            do {
                Solve s = new Solve(cursor);
                session.addSolveNoWrite(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return session;
    }

    public void writeSolve(Solve solve, PuzzleType type, int sessionIndex) {
        ContentValues values = new ContentValues();

        values.put(SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME, type.name());
        values.put(SolveDbEntry.COLUMN_NAME_SESSION_INDEX, sessionIndex);

        values.put(SolveDbEntry.COLUMN_NAME_PENALTY, solve.getPenaltyInt());
        values.put(SolveDbEntry.COLUMN_NAME_SCRAMBLE, solve.getScrambleAndSvg().getScramble());
        values.put(SolveDbEntry.COLUMN_NAME_TIME, solve.getRawTime());
        values.put(SolveDbEntry.COLUMN_NAME_TIMESTAMP, solve.getTimestamp());

        mDatabase.insert(SolveDbEntry.TABLE_NAME, null, values);
    }

    public void deleteSolve(PuzzleType type, int sessionIndex, int solveIndex) {
        mDatabase.delete(SolveDbEntry.TABLE_NAME,

                SolveDbEntry._ID + " IN " +
                        "(SELECT " + SolveDbEntry._ID + " FROM " + SolveDbEntry.TABLE_NAME
                        + " WHERE " + SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " = ?"
                        + " AND " + SolveDbEntry.COLUMN_NAME_SESSION_INDEX + " = ?"
                        + " ORDER BY " + SESSION_SOLVES_ORDER + " ASC LIMIT 1 OFFSET ?)"

                , new String[]{type.name(), String.valueOf(sessionIndex), String.valueOf(solveIndex)});
    }

    public void open() throws SQLException {
        mDatabase = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDbHelper.close();
    }

    public static abstract class SolveDbEntry implements BaseColumns {
        /* Inner class that defines the table contents */
        public static final String TABLE_NAME = "solves";

        public static final String COLUMN_NAME_PUZZLETYPE_NAME = "puzzletype";
        public static final String COLUMN_NAME_SESSION_INDEX = "session_index";

        public static final String COLUMN_NAME_SCRAMBLE = "scramble";
        public static final String COLUMN_NAME_PENALTY = "penalty";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

}

package com.pluscubed.plustimer.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;

/**
 * Solve Data Source
 */
public class SolveDataSource {

    private SQLiteDatabase mDatabase;
    private SolveDbHelper mDbHelper;

    public SolveDataSource(Context context) {
        mDbHelper = new SolveDbHelper(context);
    }

    public void loadSessionForPuzzleType(PuzzleType puzzleType, int sessionIndex) {
        Cursor cursor = mDatabase.query(
                DbCon.Entry.TABLE_NAME,
                new String[]{
                        DbCon.Entry.COLUMN_NAME_SCRAMBLE,
                        DbCon.Entry.COLUMN_NAME_PENALTY,
                        DbCon.Entry.COLUMN_NAME_TIME,
                        DbCon.Entry.COLUMN_NAME_TIMESTAMP
                },
                DbCon.Entry.COLUMN_NAME_PUZZLETYPE_NAME + " = ? AND " + DbCon.Entry.COLUMN_NAME_SESSION_INDEX + " = ?",
                new String[]{puzzleType.name(), String.valueOf(sessionIndex)},
                null, null,
                DbCon.Entry.COLUMN_NAME_SESSION_INDEX + ", " + DbCon.Entry.COLUMN_NAME_TIMESTAMP);

        Session session = new Session();
        cursor.moveToFirst();
        do {
            Solve s = new Solve(cursor);
            session.addSolve(s);
        } while (cursor.moveToNext());
        cursor.close();
    }

    public void writeSolve(Solve solve, PuzzleType type, int sessionIndex) {
        ContentValues values = new ContentValues();

        values.put(DbCon.Entry.COLUMN_NAME_PUZZLETYPE_NAME, type.name());
        values.put(DbCon.Entry.COLUMN_NAME_SESSION_INDEX, sessionIndex);

        values.put(DbCon.Entry.COLUMN_NAME_PENALTY, solve.getPenaltyInt());
        values.put(DbCon.Entry.COLUMN_NAME_SCRAMBLE, solve.getScrambleAndSvg().getScramble());
        values.put(DbCon.Entry.COLUMN_NAME_TIME, solve.getRawTime());
        values.put(DbCon.Entry.COLUMN_NAME_TIMESTAMP, solve.getTimestamp());

        mDatabase.insert(DbCon.Entry.TABLE_NAME, null, values);
    }

    public void open() throws SQLException {
        mDatabase = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDbHelper.close();
    }

}

package com.pluscubed.plustimer.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;

import java.util.HashSet;
import java.util.Set;

/**
 * Solve Data Source
 */
public class SolveDataSource {

    public static final String SESSION_SOLVES_ORDER = SolveDbEntry.COLUMN_NAME_TIMESTAMP;
    private SQLiteDatabase mDatabase;
    private SolveDbHelper mDbHelper;

    public SolveDataSource(Context context) {
        mDbHelper = new SolveDbHelper(context);
    }

    @NonNull
    public Session loadSessionForPuzzleType(PuzzleType puzzleType, int sessionId) {
        open();
        Cursor cursor = mDatabase.query(
                SolveDbEntry.TABLE_NAME,
                new String[]{
                        SolveDbEntry.COLUMN_NAME_SCRAMBLE,
                        SolveDbEntry.COLUMN_NAME_PENALTY,
                        SolveDbEntry.COLUMN_NAME_TIME,
                        SolveDbEntry.COLUMN_NAME_TIMESTAMP
                },
                SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " = ? AND " + SolveDbEntry.COLUMN_NAME_SESSION_ID + " = ?",
                new String[]{puzzleType.name(), String.valueOf(sessionId)},
                null, null,
                SESSION_SOLVES_ORDER);

        Session session = new Session(sessionId);
        if (cursor.moveToFirst()) {
            do {
                Solve s = new Solve(cursor);
                session.addSolveNoWrite(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        close();
        return session;
    }

    @NonNull
    public Set<Session> loadHistorySessionsForPuzzleType(PuzzleType puzzleType) {
        Set<Session> sessions = new HashSet<>();
        open();
        Cursor cursor = mDatabase.query(
                SolveDbEntry.TABLE_NAME,
                new String[]{
                        SolveDbEntry.COLUMN_NAME_SCRAMBLE,
                        SolveDbEntry.COLUMN_NAME_PENALTY,
                        SolveDbEntry.COLUMN_NAME_TIME,
                        SolveDbEntry.COLUMN_NAME_TIMESTAMP,
                        SolveDbEntry.COLUMN_NAME_SESSION_ID
                },
                SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " = ?",
                new String[]{puzzleType.name()},
                null, null,
                SESSION_SOLVES_ORDER);

        if (cursor.moveToFirst()) {
            do {
                Session session = null;
                int sessionId = cursor.getInt(cursor.getColumnIndex(SolveDbEntry.COLUMN_NAME_SESSION_ID));
                for (Session aSession : sessions) {
                    if (aSession.getId() == sessionId) {
                        session = aSession;
                        break;
                    }
                }
                if (session == null) {
                    session = new Session(sessionId);
                    sessions.add(session);
                }

                Solve s = new Solve(cursor);
                session.addSolveNoWrite(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        close();

        return sessions;
    }

    public void deleteSession(PuzzleType type, int sessionId) {
        open();
        mDatabase.delete(SolveDbEntry.TABLE_NAME,

                SolveDbEntry._ID + " IN " +
                        "(SELECT " + SolveDbEntry._ID + " FROM " + SolveDbEntry.TABLE_NAME
                        + " WHERE " + SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " = ?"
                        + " AND " + SolveDbEntry.COLUMN_NAME_SESSION_ID + " = ?"

                , new String[]{type.name(), String.valueOf(sessionId)});
        close();
    }

    public void writeSolve(Solve solve, PuzzleType type, int sessionId) {
        updateSolve(solve, type, sessionId, -1);
    }

    public void updateSolve(Solve solve, PuzzleType type, int sessionId, int solveIndex) {
        open();
        ContentValues contentValuesForSolve = getContentValuesForSolve(solve, type, sessionId);

        if (solveIndex != -1) {
            Cursor cursor = mDatabase.rawQuery("SELECT " + SolveDbEntry._ID + " FROM " + SolveDbEntry.TABLE_NAME
                    + " WHERE " + SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " = ?"
                    + " AND " + SolveDbEntry.COLUMN_NAME_SESSION_ID + " = ?"
                    + " ORDER BY " + SESSION_SOLVES_ORDER + " ASC LIMIT 1 OFFSET ?"

                    , new String[]{type.name(), String.valueOf(sessionId), String.valueOf(solveIndex)});
            if (cursor.moveToFirst()) {
                contentValuesForSolve.put(SolveDbEntry._ID, cursor.getInt(cursor.getColumnIndex(SolveDbEntry._ID)));
            }
            cursor.close();
        }

        mDatabase.replace(SolveDbEntry.TABLE_NAME, null, contentValuesForSolve);
        close();
    }

    @NonNull
    private ContentValues getContentValuesForSolve(Solve solve, PuzzleType type, int sessionId) {
        ContentValues values = new ContentValues();

        values.put(SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME, type.name());
        values.put(SolveDbEntry.COLUMN_NAME_SESSION_ID, sessionId);

        values.put(SolveDbEntry.COLUMN_NAME_PENALTY, solve.getPenaltyInt());
        values.put(SolveDbEntry.COLUMN_NAME_SCRAMBLE, solve.getScrambleAndSvg().getScramble());
        values.put(SolveDbEntry.COLUMN_NAME_TIME, solve.getRawTime());
        values.put(SolveDbEntry.COLUMN_NAME_TIMESTAMP, solve.getTimestamp());
        return values;
    }

    public void deleteSolve(PuzzleType type, int sessionId, int solveIndex) {
        open();
        mDatabase.delete(SolveDbEntry.TABLE_NAME,

                SolveDbEntry._ID + " IN " +
                        "(SELECT " + SolveDbEntry._ID + " FROM " + SolveDbEntry.TABLE_NAME
                        + " WHERE " + SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " = ?"
                        + " AND " + SolveDbEntry.COLUMN_NAME_SESSION_ID + " = ?"
                        + " ORDER BY " + SESSION_SOLVES_ORDER + " ASC LIMIT 1 OFFSET ?)"

                , new String[]{type.name(), String.valueOf(sessionId), String.valueOf(solveIndex)});
        close();
    }

    private void open() throws SQLException {
        mDatabase = mDbHelper.getWritableDatabase();
    }

    private void close() {
        mDatabase = null;
        mDbHelper.close();
    }

}

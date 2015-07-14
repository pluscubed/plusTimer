package com.pluscubed.plustimer.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SolveDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "solves.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SolveDataSource.SolveDbEntry.TABLE_NAME + " (" +
                    SolveDataSource.SolveDbEntry._ID + " INTEGER PRIMARY KEY," +
                    SolveDataSource.SolveDbEntry.COLUMN_NAME_PUZZLETYPE_NAME + " TEXT NOT NULL," +
                    SolveDataSource.SolveDbEntry.COLUMN_NAME_SESSION_INDEX + " INTEGER NOT NULL," +
                    SolveDataSource.SolveDbEntry.COLUMN_NAME_SCRAMBLE + " TEXT NOT NULL," +
                    SolveDataSource.SolveDbEntry.COLUMN_NAME_PENALTY + " INTEGER NOT NULL," +
                    SolveDataSource.SolveDbEntry.COLUMN_NAME_TIME + " REAL NOT NULL," +
                    SolveDataSource.SolveDbEntry.COLUMN_NAME_TIMESTAMP + " INTEGER NOT NULL" +
                    " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SolveDataSource.SolveDbEntry.TABLE_NAME;

    public SolveDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        //TODO: Implement onUpgrade
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
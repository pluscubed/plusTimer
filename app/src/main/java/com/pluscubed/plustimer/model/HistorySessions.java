package com.pluscubed.plustimer.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for history sessions
 */
public class HistorySessions {

    private static final Type SESSION_LIST_TYPE;

    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(ScrambleAndSvg.class, new ScrambleAndSvg.Serializer())
                .registerTypeAdapter(ScrambleAndSvg.class, new ScrambleAndSvg.Deserializer())
                .create();
        SESSION_LIST_TYPE = new TypeToken<List<Session>>() {
        }.getType();
    }

    private final String mFilename;

    private List<Session> mHistorySessionsList;

    /**
     * Constructs a new HistorySessions that will save to a file.
     *
     * @param filename the filename to use for saving and loading
     */
    public HistorySessions(String filename) {
        mFilename = filename;
    }

    /**
     * Deletes a session according to its index and save.
     */
    public void deleteSession(int index, Context context) {
        mHistorySessionsList.remove(index);
        save(context);
    }

    /**
     * Deletes a session matching the argument and save.
     */
    public void deleteSession(Session session, Context context) {
        mHistorySessionsList.remove(session);
        save(context);
    }

    /**
     * Adds the Session to the history list and save.
     */
    public void addSession(Session session, Context context) {
        mHistorySessionsList.add(session);
        save(context);
    }

    /**
     * Save the list to a file.
     */
    public void save(Context context) {
        Writer writer = null;
        try {
            OutputStream out = context.openFileOutput(mFilename, Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(out);
            gson.toJson(mHistorySessionsList, SESSION_LIST_TYPE, writer);
        } catch (FileNotFoundException e) {
            //File not found: create new file
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets a copy of the list.
     *
     * @return a copy of the history sessions
     */
    public List<Session> getList() {
        return new ArrayList<Session>(mHistorySessionsList);
    }

    /**
     * Load up the history sessions stored in the list. If the file doesn't exist, create an empty
     * list.
     */
    public void init(Context context) {
        BufferedReader reader = null;
        try {
            InputStream in = context.openFileInput(mFilename);
            reader = new BufferedReader(new InputStreamReader(in));
            mHistorySessionsList = gson.fromJson(reader, SESSION_LIST_TYPE);
        } catch (FileNotFoundException e) {
            //File not found: create empty list
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mHistorySessionsList == null) {
                mHistorySessionsList = new ArrayList<Session>();
            }
        }
    }
}

package com.pluscubed.plustimer.model;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pluscubed.plustimer.App;

import java.io.IOException;
import java.util.Map;

import rx.Single;

@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public abstract class CbObject {
    protected String mId;

    protected CbObject() {
        mId = "";
    }

    /**
     * Create new CbObject
     *
     * @throws CouchbaseLiteException
     * @throws IOException
     */
    protected CbObject(Context context) throws CouchbaseLiteException, IOException {
        connectCb(context);
    }

    public static <T extends CbObject> T fromDocId(Context context, String docId, Class<T> type) throws CouchbaseLiteException, IOException {
        Document doc = App.getDatabase(context).getDocument(docId);
        return fromDoc(doc, type);
    }

    public static <T extends CbObject> T fromDoc(Document doc, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        T cbObject = mapper.convertValue(doc.getUserProperties(), type);

        cbObject.mId = doc.getId();

        return cbObject;
    }

    protected void connectCb(Context context) throws CouchbaseLiteException, IOException {
        Document doc = App.getDatabase(context).createDocument();
        mId = doc.getId();

        updateCb(context);
    }

    protected void updateCb(Context context) throws CouchbaseLiteException, IOException {
        getDocument(context).putProperties(toMap());
    }

    public Document getDocument(Context context) throws CouchbaseLiteException, IOException {
        return App.getDatabase(context).getDocument(mId);
    }

    public Single<Document> getDocumentDeferred(Context context) {
        return Single.defer(() -> Single.just(getDocument(context)));
    }

    public Map<String, Object> toMap() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> objectMap = mapper.convertValue(this, Map.class);

        objectMap.put("type", getType());

        return objectMap;
    }

    protected abstract String getType();

}

package org.nuxeo.ecm.platform.wi.backend;

import org.nuxeo.ecm.core.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class PathCache {

    private static final long FOLDER_LIFE_TIME = 30 * 60 * 1000;
    private static final long FILE_LIFE_TIME = 1 * 60 * 1000;

    private int maxSize;
    private Map<String, Value> pathToUuidCache = new ConcurrentHashMap<String, Value>();
    private CoreSession session;

    public PathCache(CoreSession session, int maxSize) {
        this.session = session;
        this.maxSize = maxSize;
    }

    public void put(String path, DocumentModel model) {
        if (model == null) {
            return;
        }
        if (pathToUuidCache.size() >= maxSize) {
            clean();
        }
        pathToUuidCache.put(path, new Value(System.currentTimeMillis()
                + (model.isFolder() ? FOLDER_LIFE_TIME : FILE_LIFE_TIME), model.getId()));
    }

    public DocumentModel get(String path) {
        Value value = pathToUuidCache.get(path);
        if (value == null) {
            return null;
        }

        if (value.getExpiredTime() < System.currentTimeMillis()) {
            pathToUuidCache.remove(path);
            return null;
        }
        String uuid = value.getValue();
        DocumentModel model = null;
        try {
            model = session.getDocument(new IdRef(uuid));
        } catch (ClientException e) {
            //do nothing
        }
        if(model == null){
            pathToUuidCache.remove(path);
        }
        
        return model;
    }

    public void remove(String path){
        pathToUuidCache.remove(path);
    }

    private int clean() {
        Map<String, Value> tmpMap = new HashMap<String, Value>(pathToUuidCache);
        for (String key : tmpMap.keySet()) {
            if ((pathToUuidCache.get(key).getExpiredTime()) < System.currentTimeMillis()) {
                pathToUuidCache.remove(key);
            }
        }
        return pathToUuidCache.size();
    }

    private class Value {
        private long expiredTime;
        private String value;

        private Value(long expiredTime, String value) {
            this.expiredTime = expiredTime;
            this.value = value;
        }

        public long getExpiredTime() {
            return expiredTime;
        }

        public void setExpiredTime(long expiredTime) {
            this.expiredTime = expiredTime;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}

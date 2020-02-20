/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.backend;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * @deprecated since 10.2, unused
 */
@Deprecated
public class PathCache {

    private static final long FOLDER_LIFE_TIME = Duration.ofMinutes(30).toMillis();

    private static final long FILE_LIFE_TIME = Duration.ofMinutes(1).toMillis();

    private int maxSize;

    private Map<String, Value> pathToUuidCache = new ConcurrentHashMap<>();

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
        } catch (DocumentNotFoundException e) {
            // do nothing
        }
        if (model == null) {
            pathToUuidCache.remove(path);
        }

        return model;
    }

    public void remove(String path) {
        Map<String, Value> cacheCopy = new HashMap<>(pathToUuidCache);
        for (String key : cacheCopy.keySet()) {
            if (key.startsWith(path)) {
                pathToUuidCache.remove(key);
            }
        }
    }

    private int clean() {
        Map<String, Value> tmpMap = new HashMap<>(pathToUuidCache);
        for (String key : tmpMap.keySet()) {
            if ((pathToUuidCache.get(key).getExpiredTime()) < System.currentTimeMillis()) {
                remove(key);
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

        public String getValue() {
            return value;
        }
    }

}

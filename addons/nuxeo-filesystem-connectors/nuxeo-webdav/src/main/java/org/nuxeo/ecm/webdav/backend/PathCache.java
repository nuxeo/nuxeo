/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.backend;

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
        } catch (DocumentNotFoundException e) {
            // do nothing
        }
        if (model == null) {
            pathToUuidCache.remove(path);
        }

        return model;
    }

    public void remove(String path) {
        Map<String, Value> cacheCopy = new HashMap<String, Value>(pathToUuidCache);
        for (String key : cacheCopy.keySet()) {
            if (key.startsWith(path)) {
                pathToUuidCache.remove(key);
            }
        }
    }

    private int clean() {
        Map<String, Value> tmpMap = new HashMap<String, Value>(pathToUuidCache);
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

        @SuppressWarnings("unused")
        public void setExpiredTime(long expiredTime) {
            this.expiredTime = expiredTime;
        }

        public String getValue() {
            return value;
        }

        @SuppressWarnings("unused")
        public void setValue(String value) {
            this.value = value;
        }
    }

}

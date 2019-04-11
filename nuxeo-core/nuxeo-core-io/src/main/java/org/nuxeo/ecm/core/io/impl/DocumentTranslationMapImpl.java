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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentTranslationMapImpl.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;

public class DocumentTranslationMapImpl implements DocumentTranslationMap {

    private final String oldServerName;

    private final String newServerName;

    private Map<DocumentRef, DocumentRef> map;

    public DocumentTranslationMapImpl(String oldServerName, String newServerName) {
        this.oldServerName = oldServerName;
        this.newServerName = newServerName;
    }

    public DocumentTranslationMapImpl(String oldServerName, String newServerName, Map<DocumentRef, DocumentRef> map) {
        this.oldServerName = oldServerName;
        this.newServerName = newServerName;
        this.map = map;
    }

    @Override
    public Map<DocumentRef, DocumentRef> getDocRefMap() {
        if (map == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String getNewServerName() {
        return newServerName;
    }

    @Override
    public String getOldServerName() {
        return oldServerName;
    }

    @Override
    public void put(DocumentRef oldRef, DocumentRef newRef) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(oldRef, newRef);
    }

    @Override
    public void putAll(Map<DocumentRef, DocumentRef> refs) {
        if (refs == null) {
            return;
        }
        if (map == null) {
            map = new HashMap<>();
        }
        map.putAll(refs);
    }

    public static DocumentTranslationMap merge(List<DocumentTranslationMap> maps) {
        if (maps == null || maps.isEmpty()) {
            return null;
        }
        // take first one as reference
        DocumentTranslationMap ref = maps.get(0);
        if (ref!=null) {
            String oldRepo = ref.getOldServerName();
            String newRepo = ref.getNewServerName();
            DocumentTranslationMap finalMap = new DocumentTranslationMapImpl(oldRepo, newRepo);
            for (DocumentTranslationMap item : maps) {
                finalMap.putAll(item.getDocRefMap());
            }
            return finalMap;
        }
        return null;
    }

}

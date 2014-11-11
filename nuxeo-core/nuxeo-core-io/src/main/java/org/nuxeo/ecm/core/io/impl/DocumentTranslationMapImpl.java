/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public DocumentTranslationMapImpl(String oldServerName,
            String newServerName, Map<DocumentRef, DocumentRef> map) {
        this.oldServerName = oldServerName;
        this.newServerName = newServerName;
        this.map = map;
    }

    public Map<DocumentRef, DocumentRef> getDocRefMap() {
        if (map == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }

    public String getNewServerName() {
        return newServerName;
    }

    public String getOldServerName() {
        return oldServerName;
    }

    public void put(DocumentRef oldRef, DocumentRef newRef) {
        if (map == null) {
            map = new HashMap<DocumentRef, DocumentRef>();
        }
        map.put(oldRef, newRef);
    }

    public void putAll(Map<DocumentRef, DocumentRef> refs) {
        if (refs == null) {
            return;
        }
        if (map == null) {
            map = new HashMap<DocumentRef, DocumentRef>();
        }
        map.putAll(refs);
    }

    public static DocumentTranslationMap merge(List<DocumentTranslationMap> maps) {
        if (maps == null || maps.isEmpty()) {
            return null;
        }
        // take first one as reference
        DocumentTranslationMap ref = maps.get(0);
        String oldRepo = ref.getOldServerName();
        String newRepo = ref.getNewServerName();
        DocumentTranslationMap finalMap = new DocumentTranslationMapImpl(
                oldRepo, newRepo);
        for (DocumentTranslationMap item : maps) {
            finalMap.putAll(item.getDocRefMap());
        }
        return finalMap;
    }

}

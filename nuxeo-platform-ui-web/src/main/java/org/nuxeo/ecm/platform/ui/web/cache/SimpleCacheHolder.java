/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.cache;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

// Not used as of 5.2.
public class SimpleCacheHolder<T extends Serializable> implements Serializable {

    public static final int DEFAULT_SIZE = 20;

    private static final long serialVersionUID = 1L;

    protected final Map<String, T> cacheMap;

    public SimpleCacheHolder() {
        this(DEFAULT_SIZE);
    }

    public SimpleCacheHolder(int maxSize) {
        cacheMap = new LRUCachingMap<String, T>(maxSize);
    }

    protected String getKey(DocumentRef docRef, String key) {
        if (docRef == null) {
            if (key == null) {
                return "default";
            } else {
                return key;
            }
        } else {
            if (key == null) {
                return docRef.toString();
            } else {
                return docRef.toString() + "-" + key;
            }
        }
    }

    protected String getKey(DocumentModel doc, String key) {
        DocumentRef docRef = doc.getRef();
        Calendar modified;
        try {
            modified = (Calendar) doc
                    .getProperty("dublincore", "modified");
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        if (key == null) {
            if (modified != null) {
                key = modified.toString();
            }
        } else {
            key = key + "-" + key;
        }

        return getKey(docRef, key);
    }

    // Adders
    protected void doAdd(String key, T value) {
        cacheMap.put(key, value);
    }

    public void addToCache(String key, T value) {
        doAdd(key, value);
    }

    public void addToCache(DocumentRef docRef, String key, T value) {
        doAdd(getKey(docRef, key), value);
    }

    public void addToCache(DocumentModel doc, String key, T value) {
        doAdd(getKey(doc, key), value);
    }

    // Getters
    protected T doGet(String key) {
        return cacheMap.get(key);
    }

    public T getFromCache(String key) {
        return doGet(key);
    }

    public T getFromCache(DocumentRef docRef, String key) {
        return doGet(getKey(docRef, key));
    }

    public T getFromCache(DocumentModel doc, String key) {
        return doGet(getKey(doc, key));
    }

    // Removers
    protected void doRemove(String key) {
        cacheMap.remove(key);
    }

    public void removeFromCache(DocumentRef docRef, String key) {
        doRemove(getKey(docRef, key));
    }

    public void removeFromCache(DocumentModel doc, String key) {
        doRemove(getKey(doc, key));
    }

    public void removeFromCache(String key) {
        doRemove(key);
    }

}

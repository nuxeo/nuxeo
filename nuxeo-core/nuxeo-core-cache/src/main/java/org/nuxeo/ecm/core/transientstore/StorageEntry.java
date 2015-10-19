/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.transientstore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;

/**
 * Class representing an entry stored in the {@link TransientStore}.
 *
 * @since 7.10
 */
public class StorageEntry implements Serializable {

    protected Log log = LogFactory.getLog(StorageEntry.class);

    private static final long serialVersionUID = 1L;

    protected Map<String, Serializable> params;

    protected List<Map<String, String>> blobInfos;

    protected long size;

    protected boolean completed;

    public StorageEntry() {
        this(0, false);
    }

    public StorageEntry(long size, boolean completed) {
        log.debug("Creating new StorageEntry");
        params = null;
        blobInfos = null;
        this.size = size;
        this.completed = completed;
    }

    public Map<String, Serializable> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

    public void putParams(Map<String, Serializable> params) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.putAll(params);
    }

    public Serializable getParam(String param) {
        if (params == null) {
            return null;
        }
        return params.get(param);
    }

    public void putParam(String param, Serializable value) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(param, value);
    }

    public List<Map<String, String>> getBlobInfos() {
        return blobInfos;
    }

    public void setBlobInfos(List<Map<String, String>> blobInfos) {
        this.blobInfos = blobInfos;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

}

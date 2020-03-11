/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.transientstore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    protected ConcurrentMap<String, Serializable> params;

    protected List<Map<String, String>> blobInfos;

    protected long size;

    protected boolean completed;

    public StorageEntry() {
        this(0, false);
    }

    public StorageEntry(long size, boolean completed) {
        params = null;
        blobInfos = null;
        this.size = size;
        this.completed = completed;
    }

    public Map<String, Serializable> getParams() {
        if (params == null) {
            params = new ConcurrentHashMap<>();
        }
        return params;
    }

    public void putParams(Map<String, Serializable> params) {
        if (this.params == null) {
            this.params = new ConcurrentHashMap<>();
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
            params = new ConcurrentHashMap<>();
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

/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 */
package org.nuxeo.ecm.core.transientstore.api;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.SimpleTransientStore;

/**
 * {@link XMap} descriptor for representing the Configuration of a {@link TransientStore}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@XObject("store")
public class TransientStoreConfig {

    @XNode("@name")
    protected String name;

    @XNode("@path")
    protected String path;

    // target size that ideally should never be exceeded
    @XNode("targetMaxSizeMB")
    protected int targetMaxSizeMB = -1;

    // size that must never be exceeded
    @XNode("absoluteMaxSizeMB")
    protected int absoluteMaxSizeMB = -1;

    @XNode("firstLevelTTL")
    protected int firstLevelTTL = 60 * 2;

    @XNode("secondLevelTTL")
    protected int secondLevelTTL = 10;

    @XNode("minimalRetention")
    protected int minimalRetention = 10;

    @XNode("@class")
    protected Class<? extends TransientStoreProvider> implClass = SimpleTransientStore.class;

    protected TransientStoreProvider store;

    public TransientStoreConfig() {
    }

    public TransientStoreConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getTargetMaxSizeMB() {
        return targetMaxSizeMB;
    }

    public void setTargetMaxSizeMB(int targetMaxSizeMB) {
        this.targetMaxSizeMB = targetMaxSizeMB;
    }

    public int getAbsoluteMaxSizeMB() {
        return absoluteMaxSizeMB;
    }

    public void setAbsoluteMaxSizeMB(int absoluteMaxSizeMB) {
        this.absoluteMaxSizeMB = absoluteMaxSizeMB;
    }

    public int getFirstLevelTTL() {
        return firstLevelTTL;
    }

    public void setFirstLevelTTL(int firstLevelTTL) {
        this.firstLevelTTL = firstLevelTTL;
    }

    public int getSecondLevelTTL() {
        return secondLevelTTL;
    }

    public void setSecondLevelTTL(int secondLevelTTL) {
        this.secondLevelTTL = secondLevelTTL;
    }

    public TransientStoreProvider getStore() {
        if (store == null) {
            try {
                store = implClass.newInstance();
                store.init(this);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new NuxeoException(e);
            }
        }
        return store;
    }

    /**
     * Flush the cached store if any
     *
     * @since 9.2
     */
    public void flush() {
        store = null;
    }

    /**
     * Returns the directory where blobs will be stored.
     *
     * @since 9.1
     */
    public String getDataDir() {
        return path;
    }

}

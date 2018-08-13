/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.uidgen;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.services.config.ConfigurationService;

import java.util.ArrayList;
import java.util.List;

/**
 * UID Sequencer based on a key/value store. The store is the same for all sequencers, but they are using different
 * keys, prefixed by the sequencer name.
 *
 * @since 10.2
 */
public class KeyValueStoreUIDSequencer extends AbstractUIDSequencer {

    /**
     * Configuration property to specify the key/value store name. If none is specified, {@code sequence} is used.
     */
    public static final String STORE_NAME_PROPERTY = "nuxeo.uidseq.keyvaluestore.name";

    public static final String DEFAULT_STORE_NAME = "sequence";

    public static final String SEP = ".";

    protected String storeName;

    @Override
    public void init() {
        storeName = Framework.getService(ConfigurationService.class).getProperty(STORE_NAME_PROPERTY);
        if (isBlank(storeName)) {
            storeName = DEFAULT_STORE_NAME;
        }
    }

    @Override
    public void dispose() {
        // nothing to do
    }

    protected KeyValueStore getStore() {
        KeyValueStore store = Framework.getService(KeyValueService.class).getKeyValueStore(storeName);
        if (store == null) {
            throw new NuxeoException("Unknown key/value store: " + storeName);
        }
        return store;
    }

    protected String getKey(String key) {
        return getName() + SEP + key;
    }

    @Override
    public void initSequence(String key, long id) {
        getStore().put(getKey(key), Long.valueOf(id));
    }

    @Override
    public long getNextLong(String key) {
        return getStore().addAndGet(getKey(key), 1);
    }

    @Override
    public List<Long> getNextBlock(String key, int blockSize) {
        List<Long> ret = new ArrayList<>(blockSize);
        long last = getStore().addAndGet(getKey(key), blockSize);
        for (int i = blockSize - 1; i >= 0; i--) {
            ret.add(last - i);
        }
        return ret;
    }
}

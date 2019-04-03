/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.work;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * Filter that use a KeyValueStore to pass big record value
 *
 * @since 11.1
 */
public class KeyValueStoreOverflowRecordFilter extends BaseOverflowRecordFilter {

    private static final String INIT_KEY = "_init_";

    @Override
    public void init(Map<String, String> options) {
        super.init(options);
        // Check at init time if the KV store works
        getKeyValueStore().put(getPrefixedKey(INIT_KEY), INIT_KEY.getBytes(StandardCharsets.UTF_8), 60);
    }

    protected KeyValueStore getKeyValueStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(getStoreName());
    }

    @Override
    protected void storeValue(String recordKey, byte[] data) {
        String key = getPrefixedKey(recordKey);
        getKeyValueStore().put(key, data, getStoreTTL().toSeconds());
    }

    @Override
    protected byte[] fetchValue(String recordKey) {
        String key = getPrefixedKey(recordKey);
        return getKeyValueStore().get(key);
    }

}

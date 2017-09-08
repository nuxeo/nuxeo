/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.redis;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.storage.kv.AbstractKeyValueStoreTest;
import org.nuxeo.ecm.core.storage.kv.KeyValueService;
import org.nuxeo.ecm.core.storage.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RedisFeature.class)
public class TestRedisKeyValueStore extends AbstractKeyValueStoreTest {

    @Inject
    protected KeyValueService keyValueService;

    @Override
    protected KeyValueStoreProvider newKeyValueStore() {
        KeyValueStoreProvider store = (KeyValueStoreProvider) keyValueService.getKeyValueStore("redis");
        store.clear();
        return store;
    }

}

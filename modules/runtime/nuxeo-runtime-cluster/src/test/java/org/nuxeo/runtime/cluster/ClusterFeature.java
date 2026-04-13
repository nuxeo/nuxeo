/*
 * (C) Copyright 2020-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.cluster;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterServiceImpl.ClusterLockHelper;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.kv.RuntimeKeyValueStoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.google.inject.Binder;
import com.google.inject.name.Names;

/**
 * Feature to test the {@link ClusterService}.
 *
 * @since 11.1
 */
@Deploy("org.nuxeo.runtime.cluster")
@Deploy("org.nuxeo.runtime.cluster.tests:OSGI-INF/test-cluster-feature.xml")
@Features(RuntimeKeyValueStoreFeature.class)
public class ClusterFeature implements RunnerFeature {

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(KeyValueStore.class)
              .annotatedWith(Names.named(ClusterLockHelper.KV_STORE_NAME))
              .toProvider(() -> Framework.getService(KeyValueService.class)
                                         .getKeyValueStore(ClusterLockHelper.KV_STORE_NAME));
    }
}

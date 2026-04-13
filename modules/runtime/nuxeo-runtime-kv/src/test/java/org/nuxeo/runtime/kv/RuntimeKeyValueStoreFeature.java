/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.kv;

import static org.nuxeo.runtime.kv.KeyValueServiceImpl.DEFAULT_STORE_ID;

import java.util.Optional;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Cleanup;
import org.nuxeo.runtime.test.runner.Cleanup.Granularity;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Binder;

/**
 * @since 2025.19
 */
@Deploy("org.nuxeo.runtime.kv:OSGI-INF/keyvalue-service.xml")
@Features(RuntimeFeature.class)
public class RuntimeKeyValueStoreFeature implements RunnerFeature {

    protected Granularity granularity;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        this.granularity = Optional.ofNullable(runner.getConfig(Cleanup.class))
                                   .map(Cleanup::value)
                                   .orElse(Granularity.METHOD);
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(KeyValueStore.class)
              .toProvider(() -> Framework.getService(KeyValueService.class).getKeyValueStore(DEFAULT_STORE_ID));
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        if (granularity == Granularity.METHOD) {
            cleanup();
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        if (granularity == Granularity.CLASS) {
            cleanup();
        }
    }

    protected void cleanup() {
        var keyValueService = (KeyValueServiceImpl) Framework.getService(KeyValueService.class);
        keyValueService.providers.values().forEach(KeyValueStoreProvider::clear);
    }
}

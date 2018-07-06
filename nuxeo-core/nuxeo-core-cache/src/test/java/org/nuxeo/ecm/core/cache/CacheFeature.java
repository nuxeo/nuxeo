/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.core.cache;

import org.junit.Assert;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Binder;
import com.google.inject.name.Names;

@Deploy("org.nuxeo.ecm.core.cache")
@Features(RuntimeFeature.class)
public class CacheFeature implements RunnerFeature {

    public static final String DEFAULT_TEST_CACHE_NAME = "default-test-cache";

    public static final String KEY = "key1";

    public static final String VAL = "val1";

    @Override
    public void configure(final FeaturesRunner runner, Binder binder) {
        bindCache(binder, DEFAULT_TEST_CACHE_NAME);
    }

    protected void bindCache(Binder binder, final String name) {
        binder.bind(Cache.class).annotatedWith(Names.named(name)).toProvider(
                () -> Framework.getService(CacheService.class).getCache(name));
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) {
        Cache cache = Framework.getService(CacheService.class).getCache(DEFAULT_TEST_CACHE_NAME);
        ((CacheManagement) cache).putLocal(KEY, VAL);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) {
        clearCache(DEFAULT_TEST_CACHE_NAME);
    }

    protected void clearCache(String name) {
        Cache cache = Framework.getService(CacheService.class).getCache(name);
        ((CacheManagement) cache).invalidateLocalAll();
    }

    public static <T extends Cache> T unwrapImpl(Class<T> type, Cache cache) {
        if (!(cache instanceof CacheWrapper)) {
            Assert.fail("Not a wrapper " + cache.getClass());
        }
        cache = ((CacheWrapper) cache).cache;
        if (cache instanceof CacheWrapper) {
            return unwrapImpl(type, cache);
        }
        if (!type.isAssignableFrom(cache.getClass())) {
            Assert.fail("Not of requested type  " + type);
        }
        return type.cast(cache);
    }
}

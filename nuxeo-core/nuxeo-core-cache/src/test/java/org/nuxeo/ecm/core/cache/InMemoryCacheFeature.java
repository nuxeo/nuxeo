/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.core.cache;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.name.Names;

@Features(CacheFeature.class)
@LocalDeploy("org.nuxeo.ecm.core.cache:inmemory-cache-config.xml")
public class InMemoryCacheFeature extends SimpleFeature {

    public static final String MAXSIZE_TEST_CACHE_NAME = "maxsize-test-cache";

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        runner.getFeature(CacheFeature.class).enable();
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(Cache.class).annotatedWith(
                Names.named(MAXSIZE_TEST_CACHE_NAME)).toProvider(
                new Provider<Cache>() {

                    @Override
                    public Cache get() {
                        return Framework.getService(CacheService.class).getCache(
                                MAXSIZE_TEST_CACHE_NAME);
                    }

                });
    }

}

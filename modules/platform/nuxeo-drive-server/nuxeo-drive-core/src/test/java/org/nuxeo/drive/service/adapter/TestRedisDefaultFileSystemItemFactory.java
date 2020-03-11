/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.service.adapter;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.drive.fixtures.DefaultFileSystemItemFactoryFixture;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.test.NuxeoDriveRedisCacheFeature;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Tests the {@link DefaultFileSystemItemFactory} against the Redis implementation of the {@link CacheService}.
 *
 * @since 8.4
 */
@RunWith(ContributableFeaturesRunner.class)
@Features(NuxeoDriveRedisCacheFeature.class)
@SuiteClasses(DefaultFileSystemItemFactoryFixture.class)
public class TestRedisDefaultFileSystemItemFactory {

}

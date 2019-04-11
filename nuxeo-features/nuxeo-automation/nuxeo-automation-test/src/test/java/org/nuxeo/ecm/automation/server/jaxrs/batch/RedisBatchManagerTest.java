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
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.ecm.core.redis.transientstore.TransientStoreRedisFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Tests the {@link BatchManager} against a Redis implementation of the {@link TransientStore}.
 *
 * @since 7.10
 */
@RunWith(ContributableFeaturesRunner.class)
@Features(TransientStoreRedisFeature.class)
@SuiteClasses(BatchManagerFixture.class)
public class RedisBatchManagerTest {

}

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
 *     dmetzler
 *     ataillefer
 */
package org.nuxeo.ecm.restapi.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.restapi.server.jaxrs.BatchUploadObject;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.transientstore.test.InMemoryTransientStoreFeature;

/**
 * Tests the {@link BatchUploadObject} endpoints against an in-memory implementation of the {@link TransientStore}.
 *
 * @since 5.8
 */
@RunWith(ContributableFeaturesRunner.class)
@Features(InMemoryTransientStoreFeature.class)
@SuiteClasses(BatchUploadFixture.class)
public class BatchUploadTest extends BaseTest {

}

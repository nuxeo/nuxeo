/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.mongodb.MongoDBConnectionConfig;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * This Test is for ensuring the correct injection of
 * {@link org.nuxeo.ecm.core.storage.mongodb.MongoDBRepositoryService} and the ability to handle a connection
 * contribution.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(MongoDBFeature.class)
public class TestMongoDBRepositoryService {

    @Inject
    protected MongoDBRepositoryService mongoDBRepositoryService;

    @Test
    public void testUP() {
        assertNotNull(mongoDBRepositoryService);
    }

    /**
     * @deprecated since 11.1.
     *             {@link MongoDBRepositoryService#handleConnectionContribution(org.nuxeo.ecm.core.storage.mongodb.MongoDBRepositoryDescriptor, java.util.function.BiConsumer)}
     */
    @Deprecated
    @Test
    public void shouldHandleConnectionContributionWithoutFailure() {

        MongoDBRepositoryDescriptor descriptor = new MongoDBRepositoryDescriptor();
        descriptor.server = "anyServer";

        BiConsumer<DefaultComponent, MongoDBConnectionConfig> consumer = (dc, mc) -> {
            assertNotNull(dc);
            assertNotNull(mc);
            assertTrue(dc instanceof MongoDBConnectionService);
        };

        mongoDBRepositoryService.handleConnectionContribution(descriptor, consumer);
    }
}

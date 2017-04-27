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
 *     Funsho David
 *
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy("org.nuxeo.directory.mongodb.tests:test-mongodb-directory-with-continents-contrib.xml")
public class TestMongoDBDirectoryWithInitialData extends MongoDBDirectoryTestCase {

    @Test
    public void testGetEntries() {
        try (Session session = openSession(CONTINENT_DIR)) {
            assertNotNull(session);
            DocumentModelList entries = session.query(Collections.emptyMap());
            assertEquals(7, entries.size());
            assertEquals("europe", entries.get(0).getId());
            assertEquals("africa", entries.get(1).getId());
            assertEquals("north-america", entries.get(2).getId());
            assertEquals("south-america", entries.get(3).getId());
            assertEquals("asia", entries.get(4).getId());
            assertEquals("oceania", entries.get(5).getId());
            assertEquals("antarctica", entries.get(6).getId());
        }
    }
}

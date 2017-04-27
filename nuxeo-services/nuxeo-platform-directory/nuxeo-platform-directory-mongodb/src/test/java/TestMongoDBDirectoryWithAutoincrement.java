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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.mongodb.MongoDBSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy({ "org.nuxeo.directory.mongodb.tests:test-mongodb-directory-autoincrement.xml" })
public class TestMongoDBDirectoryWithAutoincrement extends MongoDBDirectoryTestCase {

    private static final String TEST_SCHEMA = "testSchema";

    @Test
    public void testAutoIncrementId() throws Exception {
        try (MongoDBSession session = (MongoDBSession) openSession(TEST_DIR)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", 42L);
            map.put("label", "foo");
            DocumentModel entry = session.createEntry(map);
            assertNotNull(entry);
            assertEquals(1L, entry.getProperty(TEST_SCHEMA, "id"));
            assertEquals("foo", entry.getProperty(TEST_SCHEMA, "label"));

            map.clear();
            map.put("label", "bar");
            DocumentModel entry2 = session.createEntry(map);
            assertNotNull(entry2);
            assertEquals(2L, entry2.getProperty(TEST_SCHEMA, "id"));
            assertEquals("bar", entry2.getProperty(TEST_SCHEMA, "label"));

            // Clean up counters collection
            session.getCollection(TEST_DIR + ".counters").drop();
        }
    }
}

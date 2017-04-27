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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
@LocalDeploy({ "org.nuxeo.directory.mongodb.tests:test-mongodb-directory-cache-contrib.xml",
        "org.nuxeo.directory.mongodb.tests:mongodb-directory-cache-config.xml" })
public class TestCachedMongoDBSession extends MongoDBDirectoryTestCase {

    @Test
    public void testGetFromCache() throws DirectoryException {
        try (Session session = openSession(CONTINENT_DIR)) {
            // First call will update cache
            DocumentModel entry = session.getEntry("europe");
            assertNotNull(entry);

            // Second call will use the cache
            entry = session.getEntry("europe");
            assertNotNull(entry);
        }
    }
}

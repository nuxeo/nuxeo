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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy("org.nuxeo.directory.mongodb.tests:test-mongodb-directory-contrib.xml")
public class TestMongoDBDirectory extends MongoDBDirectoryTestCase {

    @Test
    public void testCreateEntry() {
        try (Session session = openSession(CONTINENT_DIR)) {
            assertNotNull(session);

            DocumentModel docModel = session.createEntry(testContinent);
            assertEquals("europe", docModel.getId());
            assertTrue(session.hasEntry("europe"));
            assertEquals(0, docModel.getProperty("obsolete").getValue());
            assertEquals(0, docModel.getProperty("ordering").getValue());
        }
    }

    @Test
    public void testCreateEntryIfSomeParamsMissing() {
        Map<String, Object> docParams = new HashMap<>(testContinent);
        docParams.remove("obsolete");
        docParams.remove("ordering");

        try (Session session = openSession(CONTINENT_DIR)) {
            assertNotNull(session);

            DocumentModel docModel = session.createEntry(docParams);
            assertEquals("europe", docModel.getId());
            assertTrue(session.hasEntry("europe"));
            // 'obsolete' and 'ordering' are not defined by user, their values remain as the default ones
            assertEquals(0L, docModel.getProperty("obsolete").getValue());
            assertEquals(10_000_000L, docModel.getProperty("ordering").getValue());
        }
    }

    @Test
    public void testGetEntry() {
        try (Session session = openSession(CONTINENT_DIR)) {
            assertNotNull(session);

            session.createEntry(testContinent);

            DocumentModel docModel = session.getEntry("europe");
            assertEquals("label.directories.continent.europe", docModel.getProperty("label").getValue());
            assertEquals(0, docModel.getProperty("obsolete").getValue());

        }
    }

    @Test
    public void testUpdateEntry() throws Exception {
        try (Session session = openSession(CONTINENT_DIR)) {
            assertNotNull(session);

            DocumentModel docModel = session.createEntry(testContinent);

            docModel.setPropertyValue("label", "label.directories.continent.america");
            docModel.setPropertyValue("obsolete", 1);

            session.updateEntry(docModel);

            DocumentModel updatedDocModel = session.getEntry("europe");

            assertEquals("label.directories.continent.america", updatedDocModel.getProperty("label").getValue());
            assertEquals(1L, updatedDocModel.getProperty("obsolete").getValue());

            // Check that updating an entry which doesn't exist will fail
            ((DocumentModelImpl) updatedDocModel).setId("wonderland");
            updatedDocModel.setPropertyValue("label", "label.directories.continent.wonderland");

            try {
                session.updateEntry(updatedDocModel);
                fail("The document does not exists, it can't be updated");
            } catch (DirectoryException e) {
                assertEquals("Error while updating the entry, no document was found with the id wonderland",
                        e.getMessage());
            }
        }
    }

    @Test
    public void testDeleteEntry() {
        try (Session session = openSession(CONTINENT_DIR)) {

            DocumentModel docModel = session.createEntry(testContinent);
            assertTrue(session.hasEntry("europe"));

            session.deleteEntry(docModel.getId());
            assertFalse(session.hasEntry("europe"));

        }
    }

    @Test
    public void testQuery() {
        try (Session session = openSession(CONTINENT_DIR)) {

            session.createEntry(testContinent);

            Map<String, Serializable> filter = new HashMap<>();
            filter.put("id", "europe");
            filter.put("obsolete", 0);

            DocumentModelList results = session.query(filter);
            assertEquals(1, results.size());
            assertEquals("europe", results.get(0).getId());

            filter.put("obsolete", 1);
            DocumentModelList emptyResults = session.query(filter);
            assertEquals(0, emptyResults.size());

        }
    }

    @Test
    public void testQueryFullText() {
        try (Session session = openSession(CONTINENT_DIR)) {

            session.createEntry(testContinent);

            Map<String, Serializable> filter = new HashMap<>();
            filter.put("id", "eur");

            Set<String> fulltext = Collections.singleton("id");

            DocumentModelList results = session.query(filter, fulltext);
            assertEquals(1, results.size());
            assertEquals("europe", results.get(0).getId());


            filter.put("id", "EURO");
            results = session.query(filter, fulltext);
            assertEquals(1, results.size());
            assertEquals("europe", results.get(0).getId());
        }
    }

    @Test
    public void testAuthenticateFailure() throws Exception {
        try (Session session = openSession(CONTINENT_DIR)) {
            // failed authentication: not existing user
            assertFalse(session.authenticate("NonExistingUser", "whatever"));
        }
    }

}

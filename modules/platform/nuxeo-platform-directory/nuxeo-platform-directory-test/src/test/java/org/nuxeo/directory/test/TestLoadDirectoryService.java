/*
 *  (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Thierry Casanova
 */

package org.nuxeo.directory.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DATA_LOADING_POLICY_NEVER_LOAD;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DATA_LOADING_POLICY_REJECT_DUPLICATE;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DATA_LOADING_POLICY_SKIP_DUPLICATE;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DATA_LOADING_POLICY_UPDATE_DUPLICATE;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory.tests:csv-on-missing-columns-loaded-directory-contrib.xml")
public class TestLoadDirectoryService {

    protected static final String CSV_LOAD_DIRECTORY = "csvLoadedDirectory";

    protected static final String CSV_FILE = "test-append-directory.csv";

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testUpdateDirectoryFromCsv() throws IOException {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            DocumentModel entry = session.getEntry("1");
            assertEquals("Europe", entry.getPropertyValue("label"));

            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext(CSV_FILE), "text/csv", UTF_8.name(),
                    CSV_FILE);
            directoryService.loadFromCSV(CSV_LOAD_DIRECTORY, blob, DATA_LOADING_POLICY_UPDATE_DUPLICATE);

            // check that document initially loaded is still present
            assertNotNull("entry with id '2' should not be null", session.getEntry("2"));
            // check a new directory entry has been created
            entry = session.getEntry("8");
            assertEquals("Italy", entry.getPropertyValue("label"));
            // check one directory entry has been updated
            entry = session.getEntry("1");
            assertEquals("European Union", entry.getPropertyValue("label"));
        }
    }

    @Test
    public void testIgnoreThenErrorOnDuplicate() throws IOException {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext(CSV_FILE), "text/csv", UTF_8.name(),
                    CSV_FILE);
            directoryService.loadFromCSV(CSV_LOAD_DIRECTORY, blob, DATA_LOADING_POLICY_SKIP_DUPLICATE);
            // verify that new entry was created
            assertEquals("Italy", session.getEntry("8").getPropertyValue("label"));
            // verify existing entry was not updated
            assertEquals("Europe", session.getEntry("1").getPropertyValue("label"));

            try {
                directoryService.loadFromCSV(CSV_LOAD_DIRECTORY, blob, DATA_LOADING_POLICY_REJECT_DUPLICATE);
                fail("loadFromCSV with DATA_LOADING_POLICY_REJECT_DUPLICATE should fail");
            } catch (DirectoryException e) {
                assertEquals(SC_CONFLICT, e.getStatusCode());
                assertTrue(e.getMessage().contains("already exists"));
            }
        }
    }

    @Test
    public void testNeverLoad() throws IOException {
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext(CSV_FILE), "text/csv", UTF_8.name(),
                CSV_FILE);
        try {
            directoryService.loadFromCSV(CSV_LOAD_DIRECTORY, blob, DATA_LOADING_POLICY_NEVER_LOAD);
            fail("loadFromCSV with DATA_LOADING_POLICY_NEVER_LOAD should fail");
        } catch (DirectoryException e) {
            assertEquals(SC_BAD_REQUEST, e.getStatusCode());
        }
    }
}

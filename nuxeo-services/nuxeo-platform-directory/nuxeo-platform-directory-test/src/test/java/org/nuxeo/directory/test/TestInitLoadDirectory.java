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

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
public class TestInitLoadDirectory {

    protected static final String CSV_LOAD_DIRECTORY = "csvLoadedDirectory";

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:csv-always-autoincrement-directory-contrib.xml")
    public void testInitDirectoryWithAlways() throws Exception {
        assertDirectoryEntries();
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-always-autoincrement-directory-reject-duplicate-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the second csv file has overwritten the first one
            DocumentModelList entries = session.query(Map.of());
            assertEquals(3, entries.size());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:csv-always-autoincrement-directory-contrib.xml")
    public void testInitDirectoryWithNever() throws Exception {
        assertDirectoryEntries();
        // Test case NEVER - NEVER_LOAD
        hotDeployer.deploy("org.nuxeo.ecm.directory.tests:csv-never-directory-never-load-contrib.xml");
        assertDirectoryEntries();
        // Test that autoincrementId is never updated
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-never-autoincrement-directory-update-duplicate-contrib.xml");
        assertDirectoryEntries();
        // Test case NEVER - UPDATE
        hotDeployer.deploy("org.nuxeo.ecm.directory.tests:csv-never-directory-update-duplicate-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged
            DocumentModelList entries = session.query(Map.of());
            assertEquals(4, entries.size());
            assertNotNull(session.getEntry("8"));
            assertEquals("European Union", getEuropeEntry(session).getPropertyValue("label"));
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:csv-on-missing-columns-loaded-directory-contrib.xml")
    public void testInitDirectoryWithOnMissingColumns() throws Exception {
        assertDirectoryEntries();
        // First we test with dataLoadingPolicy = never_load
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-autoincrement-directory-never-load-contrib.xml");
        // We verify that nothing has changed
        assertDirectoryEntries();

        // Then we test with dataLoadingPolicy = ignore_duplicate
        hotDeployer.deploy("org.nuxeo.ecm.directory.tests:csv-on-missing-columns-directory-skip-duplicate-contrib.xml");

        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged with duplicate line ignored
            DocumentModelList entries = session.query(Map.of());
            assertEquals(4, entries.size());
            // assert unchanged existing entry:
            assertEquals("Europe", getEuropeEntry(session).getPropertyValue("label"));
        }
        // Then we test with dataLoadingPolicy = update_duplicate
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-directory-update-duplicate-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged
            DocumentModelList entries = session.query(Map.of());
            assertEquals(4, entries.size());
            assertNotNull(session.getEntry("8"));
            assertEquals("European Union", getEuropeEntry(session).getPropertyValue("label"));
        }
        // Then we test with dataLoadingPolicy = error_on_duplicate
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-directory-reject-duplicate-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // we check that Directory has not been changed
            assertEquals("European Union", getEuropeEntry(session).getPropertyValue("label"));
        }
    }

    protected void assertDirectoryEntries() {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            List<String> labels = session.query(Map.of())
                                         .stream()
                                         .map(d -> (String) d.getPropertyValue("label"))
                                         .sorted()
                                         .collect(toList());
            assertEquals(List.of("America", "Europe"), labels);
        }
    }

    protected static DocumentModel getEuropeEntry(Session session) {
        Map<String, Serializable> europeFilterQuery = Map.of("continent", "europe");
        return session.query(europeFilterQuery).get(0);
    }
}

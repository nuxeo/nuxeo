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

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
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
public class TestInitLoadDirectoryWithAutoIncrementId {

    protected static final String CSV_LOAD_DIRECTORY = "csvLoadedDirectory";

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:csv-on-missing-columns-autoincrement-directory-never-load-contrib.xml")
    public void testInitDirectoryWithOnMissingColumnsAndAutoIncrementId() throws Exception {
        // autoincrement directory could be loaded only on table creation
        // as we're having a never_load dataLoadingPolicy, initialization won't fed the directory
        assertDirectoryEntries();
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-autoincrement-directory-update-duplicate-contrib.xml");
        // we now have an update_duplicate dataLoadingPolicy but table already exists, directory won't be touched
        assertDirectoryEntries();
    }

    protected void assertDirectoryEntries() {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            long entryCount = session.query(Map.of())
                                     .stream()
                                     .map(d -> (String) d.getPropertyValue("label"))
                                     .count();
            assertEquals(0, entryCount);
        }
    }
}

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
 *     George Lefter
 *     Funsho David
 *
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.directory.tests:test-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.tests:test-directories-tsv-bundle.xml" })
public class TestDirectoryWithTSVInit {

    private static final String SCHEMA = "user";

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testGetEntry() throws Exception {
        try (Session session = directoryService.open("userDirectory")) {
            DocumentModel dm = session.getEntry("AdministratorTSV");
            assertNotNull(dm);
            assertEquals("AdministratorTSV", dm.getProperty(SCHEMA, "username"));
            assertTrue(session.authenticate("AdministratorTSV", "AdministratorTSV"));
            assertEquals(10L, dm.getProperty(SCHEMA, "intField"));
            TestDirectory.assertCalendarEquals(TestDirectory.getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
        }
    }

}

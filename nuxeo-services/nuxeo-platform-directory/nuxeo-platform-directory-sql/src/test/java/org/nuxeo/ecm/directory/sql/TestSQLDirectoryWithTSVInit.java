/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     George Lefter
 */
package org.nuxeo.ecm.directory.sql;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-tsv-bundle.xml" })
public class TestSQLDirectoryWithTSVInit {

    private static final String SCHEMA = "user";

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testGetEntry() throws Exception {
        Session session = directoryService.open("userDirectory");
        try {
            DocumentModel dm = session.getEntry("AdministratorTSV");
            assertNotNull(dm);
            assertEquals("AdministratorTSV", dm.getProperty(SCHEMA, "username"));
            assertEquals("AdministratorTSV", dm.getProperty(SCHEMA, "password"));
            assertEquals(Long.valueOf(10), dm.getProperty(SCHEMA, "intField"));
            TestSQLDirectory.assertCalendarEquals(TestSQLDirectory.getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
        } finally {
            session.close();
        }
    }

}

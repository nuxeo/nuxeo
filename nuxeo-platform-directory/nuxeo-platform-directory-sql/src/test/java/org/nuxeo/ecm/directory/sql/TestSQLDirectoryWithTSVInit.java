/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class TestSQLDirectoryWithTSVInit extends SQLDirectoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
        "test-sql-directories-tsv-bundle.xml");
    }

    private static final String SCHEMA = "user";

    public static Session getSession() throws Exception {
        return getSession("userDirectory");
    }

    @Test
    public void testGetEntry() throws Exception {
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("AdministratorTSV");
            assertNotNull(dm);
            assertEquals("AdministratorTSV", dm.getProperty(SCHEMA, "username"));
            assertEquals("AdministratorTSV", dm.getProperty(SCHEMA, "password"));
            assertEquals(Long.valueOf(10), dm.getProperty(SCHEMA, "intField"));
            TestSQLDirectory.assertCalendarEquals(
                    TestSQLDirectory.getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    (Calendar) dm.getProperty(SCHEMA, "dateField"));
        } finally {
            session.close();
        }
    }

}

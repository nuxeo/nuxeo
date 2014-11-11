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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class TestSQLDirectoryWithTSVInit extends SQLDirectoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
        "test-sql-directories-tsv-bundle.xml");
    }

    private static final String SCHEMA = "user";

    public static Session getSession() throws Exception {
        return getSession("userDirectory");
    }

    public void testGetEntry() throws Exception {
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("AdministratorTSV");
            assertNotNull(dm);
            assertEquals("AdministratorTSV", dm.getProperty(SCHEMA, "username"));
            assertEquals("AdministratorTSV", dm.getProperty(SCHEMA, "password"));
            assertEquals(Long.valueOf(10), dm.getProperty(SCHEMA, "intField"));
            assertEquals(getCalendar(1982, 3, 25, 16, 30, 47, 123),
                    dm.getProperty(SCHEMA, "dateField"));
        } finally {
            session.close();
        }
    }

    private static Calendar getCalendar(int year, int month, int day,
            int hours, int minutes, int seconds, int milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        cal.set(Calendar.MILLISECOND, milliseconds);
        return cal;
    }


}

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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.jcr.search;

import java.util.Calendar;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Test Date-based Queries.
 *
 * @author <a href="mailto:fg@nuxeo.com">Florent Guillaume</a>
 */
public class TestSQLWithDate extends RepositoryTestCase {

    private Session session;

    private Document root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        createDocs();
    }

    @Override
    public void tearDown() throws Exception {
        removeDocs();

        if (session != null) {
            session.close();
            session = null;
        }
        root = null;
        super.tearDown();
    }

    private static Calendar getCalendar(int year, int month, int day,
            int hours, int minutes, int seconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        return cal;
    }

    /**
     * Creates two documents.
     *
     * @throws Exception
     */
    private void createDocs() throws Exception {
        // put some data in workspace
        Document folder = root.addChild("testfolder1", "Folder");

        Document file1 = folder.addChild("testfile1", "File");
        file1.setString("dc:title", "testfile1_Title");
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setDate("dc:created", cal1);

        Document file2 = folder.addChild("testfile2", "File");
        file2.setString("dc:title", "testfile2_Title");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setDate("dc:created", cal2);

        session.save();
    }

    private void removeDocs() throws DocumentException {
        root.getChild("testfolder1").remove();
        session.save();
    }

    public void testSQLWithDate() throws Exception {
        String sql;
        QueryResult qr;

        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-01-01'";
        qr = session.createQuery(sql, Query.Type.NXQL).execute();
        assertEquals(2L, qr.count());
        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-03-15'";
        qr = session.createQuery(sql, Query.Type.NXQL).execute();
        assertEquals(1L, qr.count());
        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-05-01'";
        qr = session.createQuery(sql, Query.Type.NXQL).execute();
        assertEquals(0L, qr.count());

        sql = "SELECT * FROM document WHERE dc:created >= TIMESTAMP '2007-03-15 00:00:00'";
        qr = session.createQuery(sql, Query.Type.NXQL).execute();
        assertEquals(1L, qr.count());

        sql = "SELECT * FROM document WHERE dc:created >= DATE '2007-02-15' AND dc:created <= DATE '2007-03-15'";
        qr = session.createQuery(sql, Query.Type.NXQL).execute();
        assertEquals(1L, qr.count());
    }

}

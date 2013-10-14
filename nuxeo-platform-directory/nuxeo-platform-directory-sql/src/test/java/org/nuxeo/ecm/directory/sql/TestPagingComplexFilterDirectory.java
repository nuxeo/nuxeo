/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern <akervern@nuxeo.com>
 */

package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.sql.filter.SQLBetweenFilter;

public class TestPagingComplexFilterDirectory extends SQLDirectoryTestCase {
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "pagingDirectory-contrib.xml");
    }

    public Session getSession() throws ClientException {
        return getSession("pagingDirectory");
    }

    @Test
    public void testPaging() throws ClientException {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("label", "Label");

            Map<String, String> order = new HashMap<String, String>();
            order.put("id", "ASC");

            List<DocumentModel> entries = session.query(filter, filter.keySet());
            assertEquals(12, entries.size());
            assertEquals("1", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), order, false, 5,
                    -1);
            assertEquals(5, entries.size());
            assertEquals("1", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), order, false, 5, 1);
            assertEquals(5, entries.size());
            assertEquals("2", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), order, false, 5,
                    11);
            assertEquals(1, entries.size());
            assertEquals("12", entries.get(0).getId());
        } catch (UnsupportedOperationException e) {
            // Swallow it
        } finally {
            session.close();
        }
    }

    @Test
    public void testComplexFilter() throws ClientException {
        Session session = getSession();
        try {
            Calendar d121110 = new DateTime(2012, 11, 10, 0, 0, 0, 0).toGregorianCalendar();
            Calendar d121211 = new DateTime(2012, 12, 11, 0, 0, 0, 0).toGregorianCalendar();
            Calendar d121224 = new DateTime(2012, 12, 24, 0, 0, 0, 0).toGregorianCalendar();

            SQLBetweenFilter betweenFilter = new SQLBetweenFilter(d121110,
                    d121224);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("date", betweenFilter);
            List<DocumentModel> entries = session.query(filter);
            assertEquals(12, entries.size());

            betweenFilter = new SQLBetweenFilter(d121211, d121224);
            filter.put("date", betweenFilter);
            entries = session.query(filter);
            assertEquals(2, entries.size());

            filter.put("type", "something");
            entries = session.query(filter);
            assertEquals(1, entries.size());
        } finally {
            session.close();
        }
    }
}

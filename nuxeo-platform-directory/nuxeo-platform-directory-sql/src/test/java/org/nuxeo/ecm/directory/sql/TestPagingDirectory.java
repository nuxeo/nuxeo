/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern <akervern@nuxeo.com>
 */

package org.nuxeo.ecm.directory.sql;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestPagingDirectory extends SQLDirectoryTestCase {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.directory.sql.tests", "pagingDirectory-contrib.xml");
    }

    public Session getSession() throws ClientException {
        return getSession("pagingDirectory");
    }

    @Test
    public void testPaging() throws ClientException {
        Session session = getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("label","Label");
            List<DocumentModel> entries = session.query(filter, filter.keySet());
            assertEquals(12, entries.size());
            assertEquals("1", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), null, false, 5, -1);
            assertEquals(5, entries.size());
            assertEquals("1", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), null, false, 5, 1);
            assertEquals(5, entries.size());
            assertEquals("2", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), null, false, 5, 11);
            assertEquals(1, entries.size());
            assertEquals("12", entries.get(0).getId());
        } finally {
            session.close();
        }
    }
}

/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Test of {@code <autoincrementIdField>true</autoincrementIdField>}.
 */
public class TestSQLDirectoryAutoIncrementId extends SQLDirectoryTestCase {

    private static final String SCHEMA = "intIdSchema";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "autoincrementid-contrib.xml");
    }

    @Test
    public void testAutoIncrementId() throws Exception {
        DirectoryService service = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);

        Session session = service.open("testAutoIncrement");
        assertNotNull(session);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", Long.valueOf(42)); // will be ignored
        map.put("label", "foo");
        DocumentModel entry = session.createEntry(map);
        assertNotNull(entry);
        assertEquals(Long.valueOf(1), entry.getProperty(SCHEMA, "id"));
        assertEquals("foo", entry.getProperty(SCHEMA, "label"));

        map.clear();
        map.put("label", "bar");
        DocumentModel entry2 = session.createEntry(map);
        assertNotNull(entry2);
        assertEquals(Long.valueOf(2), entry2.getProperty(SCHEMA, "id"));
        assertEquals("bar", entry2.getProperty(SCHEMA, "label"));
    }

}

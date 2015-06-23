/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;

/**
 * Tests where a field has several references bound to it.
 */
public class TestSQLDirectoryMultipleReferences extends SQLDirectoryTestCase {

    private static final String SCHEMA = "user";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.directory.sql.tests", "test-sql-directories-multi-refs.xml");
    }

    public static Session getSession() throws Exception {
        return getSession("userDirectory");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEntry() throws Exception {
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_1");
            List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            assertEquals(3, groups.size());
            assertTrue(groups.contains("group_1"));
            assertTrue(groups.contains("members"));
            assertTrue(groups.contains("powerusers")); // from second reference
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateEntry() throws Exception {
        Session session = getSession();
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("username", "user_0");
            // writing to groups, which has several references is ignored and a WARN logged
            map.put("groups", Arrays.asList("members", "administrators"));
            session.createEntry(map);
        } finally {
            session.close();
        }

        session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_0");
            assertEquals("user_0", dm.getProperty(SCHEMA, "username"));
            List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            // groups are unchanged
            assertEquals(0, groups.size());
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateEntry() throws Exception {
        Session session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_1");
            // update entry
            dm.setProperty(SCHEMA, "password", "pass_2");
            // writing to groups, which has several references is ignored and a WARN logged
            dm.setProperty(SCHEMA, "groups", Arrays.asList("administrators", "members"));
            session.updateEntry(dm);
            session.close();
        } finally {
            session.close();
        }

        session = getSession();
        try {
            DocumentModel dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
            List<String> groups = (List<String>) dm.getProperty(SCHEMA, "groups");
            // groups are unchanged
            assertEquals(3, groups.size());
            assertTrue(groups.contains("group_1"));
            assertTrue(groups.contains("members"));
            assertTrue(groups.contains("powerusers")); // from second reference
        } finally {
            session.close();
        }
    }

}

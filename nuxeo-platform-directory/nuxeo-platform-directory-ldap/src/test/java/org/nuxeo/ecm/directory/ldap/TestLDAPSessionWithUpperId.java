/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;

/**
 * Tests for NXP-7000: Manage LDAP directories changing id to upper case
 *
 * @author Anahide Tchertchian
 */
public class TestLDAPSessionWithUpperId extends LDAPDirectoryTestCase {

    protected static final String USER_SCHEMANAME = "user";

    protected static final String GROUP_SCHEMANAME = "group";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // override default defs
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            fail("This test is not configured for an external server");
        } else {
            deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                    INTERNAL_SERVER_SETUP_UPPER_ID);
            getLDAPDirectory("userDirectory").setTestServer(SERVER);
            getLDAPDirectory("groupDirectory").setTestServer(SERVER);
        }
    }

    // override tests to get specific use cases

    @SuppressWarnings("rawtypes")
    public void testGetEntry() throws Exception {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            DocumentModel entry = session.getEntry("Administrator");
            assertNotNull(entry);
            assertEquals("ADMINISTRATOR", entry.getId());
            assertEquals("ADMINISTRATOR", entry.getProperty(USER_SCHEMANAME,
                    "username"));
            assertEquals("Manager", entry.getProperty(USER_SCHEMANAME,
                    "lastName"));

            assertEquals("Administrator", entry.getProperty(USER_SCHEMANAME,
                    "firstName"));
            assertNull(entry.getProperty(USER_SCHEMANAME, "password"));

            List val = (List) entry.getProperty(USER_SCHEMANAME, "employeeType");
            assertTrue(val.isEmpty());

            DocumentModel entry2 = session.getEntry("user1");
            assertNotNull(entry2);
            assertEquals("USER1", entry2.getId());
            assertEquals("USER1",
                    entry2.getProperty(USER_SCHEMANAME, "username"));
            assertEquals("One", entry2.getProperty(USER_SCHEMANAME, "lastName"));
            assertEquals("User", entry2.getProperty(USER_SCHEMANAME,
                    "firstName"));
            assertNull(entry2.getProperty(USER_SCHEMANAME, "password"));

            try {
                entry2.getProperty(USER_SCHEMANAME, "userPassword");
                fail();
            } catch (ClientException ce) {
                // expected
            }
            assertEquals(Arrays.asList("Boss"), entry2.getProperty(
                    USER_SCHEMANAME, "employeeType"));

            DocumentModel entry3 = session.getEntry("UnexistingEntry");
            assertNull(entry3);

        } finally {
            session.close();
        }
    }

    public void testQuery1() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            DocumentModelList entries;

            // adding some filter that try to do unauthorized fullext
            filter.put("lastName", "Man");
            entries = session.query(filter);
            assertEquals(0, entries.size());

            // same request without cheating
            filter.put("lastName", "Manager");
            entries = session.query(filter);
            assertEquals(1, entries.size());
            assertEquals("ADMINISTRATOR", entries.get(0).getId());

            // impossible request (too restrictive)
            filter.put("firstName", "User");
            entries = session.query(filter);
            assertEquals(0, entries.size());
        } finally {
            session.close();
        }
    }

}

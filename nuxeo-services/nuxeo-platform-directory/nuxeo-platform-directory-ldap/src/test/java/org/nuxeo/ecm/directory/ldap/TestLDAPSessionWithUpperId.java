/*
 * (C) Copyright 2006-20017Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Tests for NXP-7000: Manage LDAP directories changing id to upper case Should not be used for external LDAP servers
 *
 * @author Anahide Tchertchian
 */
// override the default server setup
@Deploy("org.nuxeo.ecm.directory.ldap.tests:TestDirectoriesWithInternalApacheDS-override-upper-id.xml")
public class TestLDAPSessionWithUpperId extends LDAPDirectoryTestCase {

    protected static final String USER_SCHEMANAME = "user";

    protected static final String GROUP_SCHEMANAME = "group";

    // override tests to get specific use cases

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetEntry() throws Exception {
        // external LDAP not supported
        assumeTrue(isLocalServer());
        try (Session session = getLDAPDirectory("userDirectory").getSession()) {
            DocumentModel entry = session.getEntry("Administrator");
            assertNotNull(entry);
            assertEquals("ADMINISTRATOR", entry.getId());
            assertEquals("ADMINISTRATOR", entry.getProperty(USER_SCHEMANAME, "username"));
            assertEquals("Manager", entry.getProperty(USER_SCHEMANAME, "lastName"));

            assertEquals("Administrator", entry.getProperty(USER_SCHEMANAME, "firstName"));
            assertNull(entry.getProperty(USER_SCHEMANAME, "password"));

            List val = (List) entry.getProperty(USER_SCHEMANAME, "employeeType");
            assertTrue(val.isEmpty());

            DocumentModel entry2 = session.getEntry("user1");
            assertNotNull(entry2);
            assertEquals("USER1", entry2.getId());
            assertEquals("USER1", entry2.getProperty(USER_SCHEMANAME, "username"));
            assertEquals("One", entry2.getProperty(USER_SCHEMANAME, "lastName"));
            assertEquals("User", entry2.getProperty(USER_SCHEMANAME, "firstName"));
            assertNull(entry2.getProperty(USER_SCHEMANAME, "password"));

            try {
                entry2.getProperty(USER_SCHEMANAME, "userPassword");
                fail();
            } catch (PropertyNotFoundException ce) {
                // expected
            }
            assertEquals(Arrays.asList("Boss"), entry2.getProperty(USER_SCHEMANAME, "employeeType"));

            DocumentModel entry3 = session.getEntry("UnexistingEntry");
            assertNull(entry3);

        }
    }

    @Test
    public void testQuery1() {
        try (Session session = getLDAPDirectory("userDirectory").getSession()) {
            Map<String, Serializable> filter = new HashMap<>();
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
        }
    }

}

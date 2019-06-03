/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.directory.ldap.ExternalLDAPDirectoryFeature.GROUP_SCHEMANAME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test class on security based on LDAP external server. By default this tests are disabled because they required an
 * external ldap server to be started. Remove ignore annotation to enable it.
 */
@Ignore
@RunWith(FeaturesRunner.class)
@Features(ExternalLDAPDirectoryFeature.class)
@Deploy("org.nuxeo.ecm.directory.ldap.tests:ldap-directories-external-security.xml")
public class TestExternalLDAPSessionSecurity {

    public static final String READER_USER = "readerUser";

    public static final String SUPER_USER = "superUser";

    @Inject
    DirectoryService dirService;

    Session userDirSession;

    Session groupDirSession;

    @Inject
    ExternalLDAPDirectoryFeature ldapFeature;

    @Before
    public void setUp() {
        LDAPDirectory userDir = (LDAPDirectory) dirService.getDirectory("userDirectory");
        try (LDAPSession session = userDir.getSession()) {
            DirContext ctx = session.getContext();
            for (String ldifFile : ldapFeature.getLdifFiles()) {
                ldapFeature.loadDataFromLdif(ldifFile, ctx);
            }
        }

        userDirSession = userDir.getSession();

        Directory groupDir = dirService.getDirectory("groupDirectory");
        groupDirSession = groupDir.getSession();
    }

    @After
    public void tearDown() throws NamingException {
        try {
            DirContext ctx = ((LDAPSession) userDirSession).getContext();
            ldapFeature.destroyRecursively("ou=people,dc=example,dc=com", ctx, -1);
            ldapFeature.destroyRecursively("ou=groups,dc=example,dc=com", ctx, -1);
        } finally {
            userDirSession.close();
            groupDirSession.close();
        }
    }

    @Test
    @WithUser(READER_USER)
    public void readerUserCanGetEntry() {
        DocumentModel entry = userDirSession.getEntry("Administrator");
        assertNotNull(entry);
        assertEquals("Administrator", entry.getId());
    }

    @Test
    @WithUser(READER_USER)
    public void readerUserCantDeleteEntry() {
        DocumentModel entry = userDirSession.getEntry("user1");
        assertNotNull(entry);
        userDirSession.deleteEntry("user1");
        entry = userDirSession.getEntry("user1");
        assertNotNull(entry);
    }

    @Test
    @WithUser(SUPER_USER)
    public void superUserCanDeleteEntry() {
        DocumentModel entry = userDirSession.getEntry("user1");
        assertNotNull(entry);
        userDirSession.deleteEntry("user1");
        entry = userDirSession.getEntry("user1");
        Assert.assertNull(entry);
    }

    @Test
    @WithUser(SUPER_USER)
    public void superUserCanCreateEntry() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "user0");
        map.put("password", "pass0");
        map.put("firstName", "User");
        map.put("lastName", "");
        map.put("intField", Long.valueOf(0));
        map.put("email", "nobody@nowhere.com");
        map.put("employeeType", Arrays.asList("item1", "item2"));
        map.put("groups", Arrays.asList("members", "administrators"));
        DocumentModel dm = userDirSession.createEntry(map);

        dm = userDirSession.getEntry("user0");
        assertNotNull(dm);
    }

    @Test
    @WithUser("unauthorizedUser")
    public void unauthorizedUserCantGetEntry() {
        DocumentModel entry = userDirSession.getEntry("Administrator");
        Assert.assertNull(entry);
    }

    @Test
    @WithUser("anEveryoneUser")
    public void everyoneGroupCanGetEntry() {
        DocumentModel entry = groupDirSession.getEntry("members");
        assertNotNull(entry);
        assertEquals("members", entry.getId());
    }

    @Test
    @WithUser("anEveryoneUser")
    public void everyoneCanUpdateEntry() {
        DocumentModel entry = groupDirSession.getEntry("members");
        assertNotNull(entry);

        assertEquals("cn=members,ou=editable,ou=groups,dc=example,dc=com", entry.getProperty(GROUP_SCHEMANAME, "dn"));

        assertEquals(List.of("submembers"), entry.getProperty(GROUP_SCHEMANAME, "subGroups"));

        // edit description and members but not subGroups
        entry.setProperty(GROUP_SCHEMANAME, "description", "AWonderfulGroup");
        entry.setProperty(GROUP_SCHEMANAME, "members", Arrays.asList("user1", "user2"));
        groupDirSession.updateEntry(entry);

        entry = groupDirSession.getEntry("members");
        assertEquals("AWonderfulGroup", entry.getProperty(GROUP_SCHEMANAME, "description"));
    }

}

/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature.User;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Test class on security based on LDAP external server. By default this tests
 * are disabled because they required an external ldap server to be started.
 * Remove ignore annotation to enable it.
 */
@Ignore
@RunWith(FeaturesRunner.class)
@Features(ExternalLDAPDirectoryFeature.class)
@LocalDeploy("org.nuxeo.ecm.directory.ldap.tests:ldap-directories-external-security.xml")
@ClientLoginFeature.User(name=TestExternalLDAPSessionSecurity.READER_USER)
public class TestExternalLDAPSessionSecurity {

    private static final String AN_EVERYONE_USER = "anEveryoneUser";

    private static final String UNAUTHORIZED_USER = "unauthorizedUser";

    public static final String READER_USER = "readerUser";

    public static final String SUPER_USER = "superUser";


    @Inject
    DirectoryService dirService;

    Session userDirSession;

    Session groupDirSession;

    @Inject
    ExternalLDAPDirectoryFeature ldapFeature;

    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory userDir;

    @Inject
    @Named(SQLDirectoryFeature.GROUP_DIRECTORY_NAME)
    Directory groupDir;

    @Before
    public void setUp() {
        LDAPSession session = (LDAPSession) ((LDAPDirectory) userDir).getSession();
        try {
            DirContext ctx = session.getContext();
            for (String ldifFile : ldapFeature.getLdifFiles()) {
                ldapFeature.loadDataFromLdif(ldifFile, ctx);
            }
        } finally {
            session.close();
        }

        userDirSession = userDir.getSession();
        groupDirSession = groupDir.getSession();
    }

    @After
    public void tearDown() throws NamingException {
        try {
            DirContext ctx = ((LDAPSession) userDirSession).getContext();
            ldapFeature.destroyRecursively("ou=people,dc=example,dc=com", ctx,
                    -1);
            ldapFeature.destroyRecursively("ou=groups,dc=example,dc=com", ctx,
                    -1);
        } finally {
            userDirSession.close();
            groupDirSession.close();
        }
    }

    @Test
    public void readerUserCanGetEntry() throws Exception {
        DocumentModel entry = userDirSession.getEntry("Administrator");
        assertNotNull(entry);
        assertEquals("Administrator", entry.getId());
    }

    @Test
    public void readerUserCantDeleteEntry() throws Exception {
        DocumentModel entry = userDirSession.getEntry("user1");
        assertNotNull(entry);
        userDirSession.deleteEntry("user1");
        entry = userDirSession.getEntry("user1");
        assertNotNull(entry);
    }

    @Test
    @User(name=SUPER_USER)
    public void superUserCanDeleteEntry() throws Exception {
        DocumentModel entry = userDirSession.getEntry("user1");
        assertNotNull(entry);
        userDirSession.deleteEntry("user1");
        entry = userDirSession.getEntry("user1");
        Assert.assertNull(entry);
    }

    @Test
    @User(name=SUPER_USER)
    public void superUserCanCreateEntry() throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();
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
    @User(name=UNAUTHORIZED_USER)
    public void unauthorizedUserCantGetEntry() throws Exception {
        DocumentModel entry = userDirSession.getEntry("Administrator");
        Assert.assertNull(entry);
    }

    @Test
    @User(name=AN_EVERYONE_USER)
    public void everyoneGroupCanGetEntry() throws Exception {
        DocumentModel entry = groupDirSession.getEntry("members");
        assertNotNull(entry);
        assertEquals("members", entry.getId());
    }

    @Test
    @User(name=AN_EVERYONE_USER)
    public void everyoneCanUpdateEntry() throws Exception {
        DocumentModel entry = groupDirSession.getEntry("members");
        assertNotNull(entry);

        assertEquals("cn=members,ou=editable,ou=groups,dc=example,dc=com",
                entry.getProperty(
                        ExternalLDAPDirectoryFeature.GROUP_SCHEMANAME, "dn"));

        assertEquals(Arrays.asList("submembers"), entry.getProperty(
                ExternalLDAPDirectoryFeature.GROUP_SCHEMANAME, "subGroups"));

        // edit description and members but not subGroups
        entry.setProperty(ExternalLDAPDirectoryFeature.GROUP_SCHEMANAME,
                "description", "AWonderfulGroup");
        entry.setProperty(ExternalLDAPDirectoryFeature.GROUP_SCHEMANAME,
                "members", Arrays.asList("user1", "user2"));
        groupDirSession.updateEntry(entry);

        entry = groupDirSession.getEntry("members");
        Assert.assertEquals("AWonderfulGroup", entry.getProperty(
                ExternalLDAPDirectoryFeature.GROUP_SCHEMANAME, "description"));
    }

}

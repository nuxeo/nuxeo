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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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
 * Test class on security based on LDAP embedded server. Only read based test
 * can be perform because the embedded server does not allow to write
 */
@RunWith(FeaturesRunner.class)
@Features(InternalLDAPDirectoryFeature.class)
@LocalDeploy("org.nuxeo.ecm.directory.ldap.tests:ldap-directories-internal-security.xml")
@ClientLoginFeature.User(name=TestInternalLDAPSessionSecurity.READER_USER)
public class TestInternalLDAPSessionSecurity {

    private static final String AN_EVERYONE_USER = "anEveryoneUser";

    private static final String UNAUTHORIZED_USER = "unauthorizedUser";

    public static final String READER_USER = "readerUser";

    @Inject
    ClientLoginFeature dummyLogin;

    @Inject
    DirectoryService dirService;

    Session userDirSession;

    Session groupDirSession;

    @Inject
    InternalLDAPDirectoryFeature ldapFeature;

    @Inject
    MockLdapServer embeddedLDAPserver;

    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory userDir;

    @Inject
    @Named(SQLDirectoryFeature.GROUP_DIRECTORY_NAME)
    Directory groupDir;

    @Before
    public void setUp() {
        ((LDAPDirectory) userDir).setTestServer(embeddedLDAPserver);
        ((LDAPDirectory) groupDir).setTestServer(embeddedLDAPserver);
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
        userDirSession.close();
        groupDirSession.close();
        if (embeddedLDAPserver != null) {
            embeddedLDAPserver.shutdownLdapServer();
            embeddedLDAPserver = null;
        }
    }

    @Test
    public void readerUserCanGetEntry() throws Exception {
        DocumentModel entry = userDirSession.getEntry("Administrator");
        assertNotNull(entry);
        assertEquals("Administrator", entry.getId());
    }

    @Test
    public void readerUserCanQuery() throws LoginException {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("lastName", "Manager");
        DocumentModelList entries = userDirSession.query(filter);
        assertEquals(1, entries.size());
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

}

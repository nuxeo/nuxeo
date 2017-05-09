/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.directory.DirContext;
import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test class on security based on LDAP embedded server. Only read based test can be perform because the embedded server
 * does not allow to write
 */
/* Ignored due to NXP-15777, this feature causes failure in the next test */
@Ignore
@RunWith(FeaturesRunner.class)
@Features(InternalLDAPDirectoryFeature.class)
@LocalDeploy("org.nuxeo.ecm.directory.ldap.tests:ldap-directories-internal-security.xml")
public class TestInternalLDAPSessionSecurity {

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

    @Before
    public void setUp() {

        Directory userDir = dirService.getDirectory("userDirectory");
        Directory groupDir = dirService.getDirectory("groupDirectory");

        ((LDAPDirectory) userDir).setTestServer(embeddedLDAPserver);
        ((LDAPDirectory) groupDir).setTestServer(embeddedLDAPserver);
        try (LDAPSession session = (LDAPSession) userDir.getSession()) {
            DirContext ctx = session.getContext();
            for (String ldifFile : ldapFeature.getLdifFiles()) {
                ldapFeature.loadDataFromLdif(ldifFile, ctx);
            }
        }

        userDirSession = userDir.getSession();
        groupDirSession = groupDir.getSession();
    }

    @After
    public void tearDown() {
        userDirSession.close();
        groupDirSession.close();
        if (embeddedLDAPserver != null) {
            embeddedLDAPserver.shutdownLdapServer();
            embeddedLDAPserver = null;
        }
    }

    @Test
    public void readerUserCanGetEntry() throws Exception {
        dummyLogin.login(READER_USER);
        DocumentModel entry = userDirSession.getEntry("Administrator");
        assertNotNull(entry);
        assertEquals("Administrator", entry.getId());
        dummyLogin.logout();
    }

    @Test
    public void readerUserCanQuery() throws LoginException {
        dummyLogin.login(READER_USER);
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("lastName", "Manager");
        DocumentModelList entries = userDirSession.query(filter);
        assertEquals(1, entries.size());
        dummyLogin.logout();
    }

    @Test
    public void unauthorizedUserCantGetEntry() throws Exception {
        dummyLogin.login("unauthorizedUser");
        DocumentModel entry = userDirSession.getEntry("Administrator");
        Assert.assertNull(entry);
        dummyLogin.logout();
    }

    @Test
    public void everyoneGroupCanGetEntry() throws Exception {
        dummyLogin.login("anEveryoneUser");
        DocumentModel entry = groupDirSession.getEntry("members");
        assertNotNull(entry);
        assertEquals("members", entry.getId());
        dummyLogin.logout();
    }

}

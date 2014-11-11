/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import static org.nuxeo.ecm.core.api.Constants.CORE_TEST_TESTS_BUNDLE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ANONYMOUS;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestSecurityPolicyService extends SQLRepositoryTestCase {

    private void setTestPermissions(String user, String... perms)
            throws ClientException {
        CoreSession session = openSessionAs(SecurityConstants.SYSTEM_USERNAME);
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntryImpl userEntry = new UserEntryImpl(user);
        for (String perm : perms) {
            userEntry.addPrivilege(perm);
        }
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        session.save();
        closeSession(session);
    }

    @SuppressWarnings("deprecation")
    public void checkCorePolicy() throws Exception {
        // create document
        CoreSession session = openSessionAs(ADMINISTRATOR);
        setTestPermissions(ANONYMOUS, READ);
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        // set access security
        folder.setProperty("secupolicy", "securityLevel", Long.valueOf(4));
        folder = session.createDocument(folder);
        session.save();

        // test permission for 'foo' user using hasPermission
        Principal fooUser = new UserPrincipal("foo", new ArrayList<String>(),
                false, false);
        assertFalse(session.hasPermission(fooUser, folder.getRef(), READ));

        closeSession(session);

        // open session as anonymous and set access on user info
        session = openSessionAs(ANONYMOUS);
        DocumentModelImpl documentModelImpl = new DocumentModelImpl("User");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("accessLevel", Long.valueOf(3));
        documentModelImpl.addDataModel(new DataModelImpl("user", data));
        ((NuxeoPrincipal) session.getPrincipal()).setModel(documentModelImpl);
        // access level is too low for this doc
        assertFalse(session.hasPermission(folder.getRef(), READ));
        // change user access level => can read
        ((NuxeoPrincipal) session.getPrincipal()).getModel().setProperty(
                "user", "accessLevel", Long.valueOf(5));
        assertTrue(session.hasPermission(folder.getRef(), READ));
        session.save();
        closeSession(session);
    }

    @Test
    public void testNewSecurityPolicy() throws Exception {
        // "user" schema
        deployContrib(CORE_TEST_TESTS_BUNDLE, "test-CoreExtensions.xml");
        // deploy custom security policy
        deployContrib(CORE_TEST_TESTS_BUNDLE,
                "test-security-policy-contrib.xml");
        checkCorePolicy();
    }

    public static void checkLockPermissions(CoreSession session,
            DocumentRef docRef, boolean canWrite) throws ClientException {
        assertEquals(canWrite, session.hasPermission(docRef, WRITE));
        // test WRITE_PROPERTIES as it used to be granted when locked
        assertEquals(canWrite, session.hasPermission(docRef, WRITE_PROPERTIES));
        assertTrue(session.hasPermission(docRef, READ));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testLockSecurityPolicy() throws Exception {
        // create document
        CoreSession session = openSessionAs(ADMINISTRATOR);
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);
        DocumentRef docRef = folder.getRef();

        // write granted to admin
        checkLockPermissions(session, docRef, true);

        // add read/write to anonymous
        setTestPermissions(ANONYMOUS, READ_WRITE);

        session.save();
        closeSession(session);

        session = openSessionAs(ANONYMOUS);
        // write granted to anonymous
        checkLockPermissions(session, docRef, true);

        // set lock
        folder = session.getDocument(docRef);
        folder.setLock();
        folder = session.saveDocument(folder);

        // write still granted
        checkLockPermissions(session, docRef, true);

        session.save();
        closeSession(session);

        // write denied to admin
        session = openSessionAs(ADMINISTRATOR);
        checkLockPermissions(session, docRef, false);
        session.save();
        closeSession(session);
    }

}

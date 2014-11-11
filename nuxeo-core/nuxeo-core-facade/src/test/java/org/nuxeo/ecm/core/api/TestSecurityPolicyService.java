/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import static org.nuxeo.ecm.core.api.Constants.CORE_BUNDLE;
import static org.nuxeo.ecm.core.api.Constants.CORE_FACADE_TESTS_BUNDLE;
import static org.nuxeo.ecm.core.api.Constants.SCHEMA_BUNDLE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ANONYMOUS;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestSecurityPolicyService extends NXRuntimeTestCase {

    private static final String REPO_NAME = "default";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle(SCHEMA_BUNDLE);

        deployContrib(CORE_BUNDLE,
                "OSGI-INF/CoreService.xml");
        deployContrib(CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(CORE_BUNDLE,
                "OSGI-INF/RepositoryService.xml");

        deployContrib(CORE_FACADE_TESTS_BUNDLE,
                "test-CoreExtensions.xml");
        deployContrib(CORE_FACADE_TESTS_BUNDLE,
                "DemoRepository.xml");

        deployBundle("org.nuxeo.ecm.core.event");
    }

    private static CoreSession openSession(String user) throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", user);
        return CoreInstance.getInstance().open(REPO_NAME, ctx);
    }

    private static void saveAndCloseSession(CoreSession session)
            throws ClientException {
        session.save();
        CoreInstance.getInstance().close(session);
    }

    private static void setTestPermissions(String user, String... perms)
            throws ClientException {
        CoreSession session = openSession(SecurityConstants.SYSTEM_USERNAME);
        DocumentModel doc = session.getRootDocument();
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntryImpl userEntry = new UserEntryImpl(user);
        for (String perm : perms) {
            userEntry.addPrivilege(perm, true, false);
        }
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        saveAndCloseSession(session);
    }

    public static void checkCorePolicy() throws Exception {
        // create document
        CoreSession session = openSession(ADMINISTRATOR);
        setTestPermissions(ANONYMOUS, READ);
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        // set access security
        folder.setProperty("secupolicy", "securityLevel", 4L);
        folder = session.createDocument(folder);
        saveAndCloseSession(session);

        // open session as anonymous and set access on user info
        session = openSession(ANONYMOUS);
        DocumentModelImpl documentModelImpl = new DocumentModelImpl("User");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("accessLevel", 3L);
        documentModelImpl.addDataModel(new DataModelImpl("user", data));
        ((NuxeoPrincipal) session.getPrincipal()).setModel(documentModelImpl);
        // access level is too low for this doc
        assertFalse(session.hasPermission(folder.getRef(), READ));
        // change user access level => can read
        ((NuxeoPrincipal) session.getPrincipal()).getModel().setProperty(
                "user", "accessLevel", 5L);
        assertTrue(session.hasPermission(folder.getRef(), READ));
        saveAndCloseSession(session);
    }

    public void testNewSecurityPolicy() throws Exception {
        // standard permissions
        deployContrib(CORE_BUNDLE, "OSGI-INF/permissions-contrib.xml");
        // deploy custom security policy
        deployContrib(CORE_FACADE_TESTS_BUNDLE, "test-security-policy-contrib.xml");
        checkCorePolicy();
    }

    public static void checkLockPermissions(CoreSession session, DocumentRef docRef,
            boolean canWrite) throws ClientException {
        assertEquals(canWrite, session.hasPermission(docRef, WRITE));
        // test WRITE_PROPERTIES as it used to be granted when locked
        assertEquals(canWrite, session.hasPermission(docRef, WRITE_PROPERTIES));
        assertTrue(session.hasPermission(docRef, READ));
    }

    public void testLockSecurityPolicy() throws Exception {
        // deploy standard contribs
        deployContrib(CORE_BUNDLE, "OSGI-INF/permissions-contrib.xml");
        deployContrib(CORE_BUNDLE, "OSGI-INF/security-policy-contrib.xml");

        // create document
        CoreSession session = openSession(ADMINISTRATOR);
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);
        DocumentRef docRef = folder.getRef();

        // write granted to admin
        checkLockPermissions(session, docRef, true);

        // add read/write to anonymous
        setTestPermissions(ANONYMOUS, READ_WRITE);

        saveAndCloseSession(session);

        session = openSession(ANONYMOUS);
        // write granted to anonymous
        checkLockPermissions(session, docRef, true);

        // set lock
        folder = session.getDocument(docRef);
        folder.setLock("anonymous:");
        folder = session.saveDocument(folder);

        // write still granted
        checkLockPermissions(session, docRef, true);

        saveAndCloseSession(session);

        // write denied to admin
        session = openSession(ADMINISTRATOR);
        checkLockPermissions(session, docRef, false);
        saveAndCloseSession(session);
    }

}

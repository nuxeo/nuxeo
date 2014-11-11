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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ANONYMOUS;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.core.test.tests:test-CoreExtensions.xml",
        "org.nuxeo.ecm.core.test.tests:test-security-policy-contrib.xml" })
public class TestSecurityPolicyService {

    @Inject
    protected RepositorySettings repo;

    @Inject
    protected RuntimeHarness harness;

    private void setTestPermissions(String user, String... perms)
            throws ClientException {
        try (CoreSession session = repo.openSessionAs(SecurityConstants.SYSTEM_USERNAME)) {
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
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNewSecurityPolicy() throws Exception {
        // create document
        DocumentRef folderRef;
        try (CoreSession session = repo.openSessionAs(ADMINISTRATOR)) {
            setTestPermissions(ANONYMOUS, READ);
            DocumentModel root = session.getRootDocument();
            DocumentModel folder = new DocumentModelImpl(
                    root.getPathAsString(), "folder#1", "Folder");
            // set access security
            folder.setProperty("secupolicy", "securityLevel", Long.valueOf(4));
            folder = session.createDocument(folder);
            folderRef = folder.getRef();
            session.save();

            // test permission for 'foo' user using hasPermission
            Principal fooUser = new UserPrincipal("foo",
                    new ArrayList<String>(), false, false);
            assertFalse(session.hasPermission(fooUser, folder.getRef(), READ));
        }

        // open session as anonymous and set access on user info
        try (CoreSession session = repo.openSessionAs(ANONYMOUS)) {
            DocumentModelImpl documentModelImpl = new DocumentModelImpl("User");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("accessLevel", Long.valueOf(3));
            documentModelImpl.addDataModel(new DataModelImpl("user", data));
            ((NuxeoPrincipal) session.getPrincipal()).setModel(documentModelImpl);
            // access level is too low for this doc
            assertFalse(session.hasPermission(folderRef, READ));
            // change user access level => can read
            ((NuxeoPrincipal) session.getPrincipal()).getModel().setProperty(
                    "user", "accessLevel", Long.valueOf(5));
            assertTrue(session.hasPermission(folderRef, READ));
            session.save();
        }
    }

}

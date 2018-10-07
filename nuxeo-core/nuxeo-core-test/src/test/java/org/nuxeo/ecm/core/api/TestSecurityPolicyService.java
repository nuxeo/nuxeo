/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ANONYMOUS;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:test-CoreExtensions.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:test-security-policy-contrib.xml")
public class TestSecurityPolicyService {

    @Inject
    protected CoreFeature coreFeature;

    private void setTestPermissions(String user, String... perms) {
        try (CloseableCoreSession session = coreFeature.openCoreSession()) {
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
        try (CloseableCoreSession session = coreFeature.openCoreSession(ADMINISTRATOR)) {
            setTestPermissions(ANONYMOUS, READ);
            DocumentModel root = session.getRootDocument();
            DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "folder#1", "Folder");
            // set access security
            folder.setProperty("secupolicy", "securityLevel", Long.valueOf(4));
            folder = session.createDocument(folder);
            folderRef = folder.getRef();
            session.save();

            // test permission for 'foo' user using hasPermission
            NuxeoPrincipal fooUser = new UserPrincipal("foo", new ArrayList<String>(), false, false);
            assertFalse(session.hasPermission(fooUser, folderRef, READ));
            assertTrue(session.filterGrantedPermissions(fooUser, folderRef, Arrays.asList(READ)).isEmpty());
            setTestPermissions(fooUser.getName(), READ);
            assertTrue(session.hasPermission(fooUser, folderRef, READ));
            assertEquals(session.filterGrantedPermissions(fooUser, folderRef, Arrays.asList(READ)),
                    Arrays.asList(READ));
        }

        // open session as anonymous and set access on user info
        try (CloseableCoreSession session = coreFeature.openCoreSession(ANONYMOUS)) {
            DocumentModelImpl documentModelImpl = new DocumentModelImpl("User");
            Map<String, Object> data = new HashMap<>();
            data.put("accessLevel", Long.valueOf(3));
            documentModelImpl.addDataModel(new DataModelImpl("user", data));
            session.getPrincipal().setModel(documentModelImpl);
            // access level is too low for this doc
            assertFalse(session.hasPermission(folderRef, READ));
            // change user access level => can read
            session.getPrincipal().getModel().setProperty("user", "accessLevel", Long.valueOf(5));
            assertTrue(session.hasPermission(folderRef, READ));
            session.save();
        }
    }

}

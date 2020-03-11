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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ANONYMOUS;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestLockSecurityPolicy {

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

    public static void checkLockPermissions(CoreSession session, DocumentRef docRef, boolean canWrite) {
        assertEquals(canWrite, session.hasPermission(docRef, WRITE));
        // test WRITE_PROPERTIES as it used to be granted when locked
        assertEquals(canWrite, session.hasPermission(docRef, WRITE_PROPERTIES));
        assertTrue(session.hasPermission(docRef, READ));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testLockSecurityPolicy() throws Exception {
        // create document
        DocumentRef folderRef;
        try (CloseableCoreSession session = coreFeature.openCoreSession(ADMINISTRATOR)) {
            DocumentModel root = session.getRootDocument();
            DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "folder#1", "Folder");
            folder = session.createDocument(folder);
            folderRef = folder.getRef();

            // write granted to admin
            checkLockPermissions(session, folderRef, true);

            // add read/write to anonymous
            setTestPermissions(ANONYMOUS, READ_WRITE);

            session.save();
        }

        try (CloseableCoreSession session = coreFeature.openCoreSession(ANONYMOUS)) {
            // write granted to anonymous
            checkLockPermissions(session, folderRef, true);

            // set lock
            DocumentModel folder = session.getDocument(folderRef);
            folder.setLock();
            folder = session.saveDocument(folder);

            // write still granted
            checkLockPermissions(session, folderRef, true);

            session.save();
        }

        // write denied to admin
        try (CloseableCoreSession session = coreFeature.openCoreSession(ADMINISTRATOR)) {
            checkLockPermissions(session, folderRef, false);
            session.save();
        }
    }

}

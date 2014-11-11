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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ANONYMOUS;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
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
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestLockSecurityPolicy {

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

    @SuppressWarnings("boxing")
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
        DocumentRef folderRef;
        try (CoreSession session = repo.openSessionAs(ADMINISTRATOR)) {
            DocumentModel root = session.getRootDocument();
            DocumentModel folder = new DocumentModelImpl(
                    root.getPathAsString(), "folder#1", "Folder");
            folder = session.createDocument(folder);
            folderRef = folder.getRef();

            // write granted to admin
            checkLockPermissions(session, folderRef, true);

            // add read/write to anonymous
            setTestPermissions(ANONYMOUS, READ_WRITE);

            session.save();
        }

        try (CoreSession session = repo.openSessionAs(ANONYMOUS)) {
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
        try (CoreSession session = repo.openSessionAs(ADMINISTRATOR)) {
            checkLockPermissions(session, folderRef, false);
            session.save();
        }
    }

}

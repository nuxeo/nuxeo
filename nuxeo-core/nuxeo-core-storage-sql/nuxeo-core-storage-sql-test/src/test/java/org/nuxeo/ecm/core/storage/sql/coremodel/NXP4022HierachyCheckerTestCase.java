/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class NXP4022HierachyCheckerTestCase extends SQLRepositoryTestCase {

    private DocumentModel file;

    private DocumentModel folder;

    private DocumentModel root;

    private DocumentRef rootRef;

    private DocumentRef fileRef;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        createHierarchy();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected void createHierarchy() throws ClientException {
        root = session.getRootDocument();
        rootRef = root.getRef();
        String rootPath = root.getPathAsString();
        folder = session.createDocument(session.createDocumentModel(rootPath,
                "folder", "Folder"));
        String folderPath = folder.getPathAsString();
        file = session.createDocument(session.createDocumentModel(folderPath,
                "file", "File"));
        fileRef = file.getRef();
        session.save();
    }

    public void testExist() throws ClientException {
        assertTrue("file does not exist",
                BinaryTextListener.existsWithItsHierarchy(session, rootRef,
                        fileRef));
    }

    public void testOrphan() throws ClientException {
        CoreSession otherSession = openSessionAs(SecurityConstants.ADMINISTRATOR);
        otherSession.getDocument(fileRef);
        DocumentRef folderRef = folder.getRef();
        session.removeDocument(folderRef);
        session.save();
        assertTrue("file still exist", otherSession.exists(fileRef));
    }

}

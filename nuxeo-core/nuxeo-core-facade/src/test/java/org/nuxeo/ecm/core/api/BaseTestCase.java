/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public abstract class BaseTestCase extends SQLRepositoryTestCase {

    protected static NXRuntimeTestCase runtime;

    protected final Random random = new Random(new Date().getTime());

    protected DocumentModel root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        root = getRootDocument();
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp(getRootDocument().getRef());
        closeSession();
        super.tearDown();
    }

    // Convenience methods

    protected String generateUnique() {
        return String.valueOf(random.nextLong());
    }

    protected DocumentModel getRootDocument() throws ClientException {
        DocumentModel root = session.getRootDocument();

        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getRef());
        assertNotNull(root.getPathAsString());

        return root;
    }

    protected DocumentModel createChildDocument(DocumentModel childFolder)
            throws ClientException {

        DocumentModel ret = session.createDocument(childFolder);

        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());

        return ret;
    }

    protected List<DocumentModel> createChildDocuments(
            List<DocumentModel> childFolders) throws ClientException {
        List<DocumentModel> rets = new ArrayList<DocumentModel>();
        Collections.addAll(
                rets,
                session.createDocument(childFolders.toArray(new DocumentModel[childFolders.size()])));

        assertNotNull(rets);
        assertEquals(childFolders.size(), rets.size());

        for (DocumentModel createdChild : rets) {
            assertNotNull(createdChild);
            assertNotNull(createdChild.getName());
            assertNotNull(createdChild.getRef());
            assertNotNull(createdChild.getPathAsString());
            assertNotNull(createdChild.getId());
        }

        return rets;
    }

    protected void cleanUp(DocumentRef ref) throws ClientException {
        session.removeChildren(ref);
        session.save();
    }

}

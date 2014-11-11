/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */

package org.nuxeo.ecm.core.io.impl;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * Inits the repository for a typed exported document test case.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class TypedExportedDocumentRepositoryInit extends DefaultRepositoryInit {

    public static final String TEST_DOC_NAME = "testDoc";

    @Override
    public void populate(CoreSession session) throws ClientException {

        createTestDoc(session);
    }

    /**
     * Creates the test doc.
     * 
     * @param session the session
     * @return the document model
     * @throws ClientException the client exception
     */
    protected final DocumentModel createTestDoc(CoreSession session)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", TEST_DOC_NAME,
                "File");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setProperty("dublincore", "title", "My test doc");
        doc.setProperty("dublincore", "created", "2011-12-29T11:24:25Z");
        doc.setProperty("dublincore", "creator", "Administrator");
        doc.setProperty("dublincore", "modified", "2011-12-29T11:24:25Z");
        doc.setProperty("dublincore", "lastContributor", "Administrator");
        doc.setProperty("dublincore", "contributors", new String[] {
                "Administrator", "joe" });
        doc.setProperty("dublincore", "subjects", new String[] { "Art",
                "Architecture" });

        // -----------------------
        // file
        // -----------------------
        doc.setProperty("file", "filename", "test_file.doc");
        Blob blob = new StringBlob("My blob");
        blob.setFilename("test_file.doc");
        blob.setMimeType("text/plain");
        doc.setProperty("file", "content", blob);

        return session.createDocument(doc);
    }

}

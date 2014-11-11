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
 *     bstefanescu
 *
 * $Id: DocumentModelUpdater.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.IOException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;

/**
 * A writer that only updates existing docs. The doc ID is used to identity docs
 * The imported tree structure is ignored
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: improve it ->
// modify core session to add a batch create method and use it
public class DocumentModelUpdater extends AbstractDocumentModelWriter {

    /**
     *
     * @param session the session to the repository where to write
     * @param parentPath where to write the tree. this document will be used as
     *            the parent of all top level documents passed as input. Note
     *            that you may have
     */
    public DocumentModelUpdater(CoreSession session, String parentPath) {
        super(session, parentPath);

    }

    public DocumentModelUpdater(CoreSession session, String parentPath,
            int saveInterval) {
        super(session, parentPath, saveInterval);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Override
    public DocumentTranslationMap write(ExportedDocument xdoc) throws IOException {
        if (xdoc.getDocument() == null) {
            // not a valid doc -> this may be a regular folder for example the
            // root of the tree
            return null;
        }

        DocumentModel doc = null;
        String id = xdoc.getId();
        try {
            doc = session.getDocument(new IdRef(id));
        } catch (Exception e) {
            System.out.println("Cannot update document. No such document: "
                    + id);
            return null;
        }

        try {
            doc = updateDocument(xdoc, doc);
            DocumentLocation source = xdoc.getSourceLocation();
            DocumentTranslationMap map = new DocumentTranslationMapImpl(
                    source.getServerName(), doc.getRepositoryName());
            map.put(source.getDocRef(), doc.getRef());
            return map;
        } catch (ClientException e) {
            IOException ioe = new IOException(
                    "Failed to import document in repository: "
                            + e.getMessage());
            ioe.setStackTrace(e.getStackTrace());
            e.printStackTrace();
            return null;
        }
    }

}

/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DocumentTreeReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.IOException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentTreeIterator;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentTreeReader extends DocumentModelReader {

    private DocumentTreeIterator iterator;

    private int pathSegmentsToRemove = 0;

    public DocumentTreeReader(CoreSession session, DocumentModel root,
            boolean excludeRoot) throws ClientException {
        super(session);
        iterator = new DocumentTreeIterator(session, root, excludeRoot);
        pathSegmentsToRemove = root.getPath().segmentCount()
                - (excludeRoot ? 0 : 1);
    }

    public DocumentTreeReader(CoreSession session, DocumentRef root)
            throws ClientException {
        this(session, session.getDocument(root));
    }

    public DocumentTreeReader(CoreSession session, DocumentModel root)
            throws ClientException {
        this(session, root, false);
    }

    @Override
    public void close() {
        super.close();
        iterator.reset();
        iterator = null;
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (iterator.hasNext()) {
            DocumentModel docModel = iterator.next();
            if (pathSegmentsToRemove > 0) {
                // remove unwanted leading segments
                return new ExportedDocumentImpl(docModel,
                        docModel.getPath().removeFirstSegments(
                                pathSegmentsToRemove), inlineBlobs);
            } else {
                return new ExportedDocumentImpl(docModel, inlineBlobs);
            }
        }
        return null;
    }

}

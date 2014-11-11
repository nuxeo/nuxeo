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
 * $Id: DocumentsListReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * Reader for a simple list of DocumentModel objects.
 *
 * @author <a href="mailto:dm@nuxeo.com">DM</a>
 */
public class DocumentsListReader extends DocumentModelReader {

    private Iterator<DocumentModel> iterator;

    public DocumentsListReader(CoreSession session, List<DocumentModel> docsList) {
        super(session);

        iterator = docsList.iterator();
    }

    public static DocumentsListReader createDocumentsListReader(
            CoreSession session, Collection<DocumentRef> docRefsList)
            throws ClientException {

        List<DocumentModel> list = new ArrayList<DocumentModel>();

        for (DocumentRef docRef : docRefsList) {
            DocumentModel doc = session.getDocument(docRef);
            list.add(doc);
        }

        return new DocumentsListReader(session, list);
    }

    @Override
    public void close() {
        super.close();
        iterator = null;
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (iterator.hasNext()) {
            DocumentModel docModel = iterator.next();
            return new ExportedDocumentImpl(docModel, inlineBlobs);
        }
        return null;
    }

}

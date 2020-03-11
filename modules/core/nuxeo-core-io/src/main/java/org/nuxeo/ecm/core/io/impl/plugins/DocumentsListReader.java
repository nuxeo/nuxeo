/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    public static DocumentsListReader createDocumentsListReader(CoreSession session, Collection<DocumentRef> docRefsList) {

        List<DocumentModel> list = new ArrayList<>();

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

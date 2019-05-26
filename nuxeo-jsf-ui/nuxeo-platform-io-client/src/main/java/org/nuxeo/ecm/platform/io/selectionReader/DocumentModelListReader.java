/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.io.selectionReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

public class DocumentModelListReader extends AbstractDocumentReader {

    List<DocumentModel> docList;

    public DocumentModelListReader(List<DocumentModel> docs) {
        docList = new ArrayList<>();
        docList.addAll(docs);
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (docList == null || docList.isEmpty()) {
            return null;
        }
        return new ExportedDocumentImpl(docList.remove(0));
    }

    @Override
    public void close() {
        docList = null;
    }

}

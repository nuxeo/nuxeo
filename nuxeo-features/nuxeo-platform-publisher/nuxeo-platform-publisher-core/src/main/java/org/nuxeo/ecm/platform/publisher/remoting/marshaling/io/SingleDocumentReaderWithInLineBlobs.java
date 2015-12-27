/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling.io;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelReader;

import java.io.IOException;

/**
 * {@link DocumentModelReader} implementation that uses inline blobs.
 *
 * @author tiry
 */
public class SingleDocumentReaderWithInLineBlobs extends DocumentModelReader {

    protected DocumentModel doc;

    private boolean readDone = false;

    public SingleDocumentReaderWithInLineBlobs(CoreSession session, DocumentModel doc) {
        super(session);
        this.doc = doc;
    }

    @Override
    public void close() {
        super.close();
        session = null;
        doc = null;
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (doc != null) {
            if (readDone) {
                return null;
            } else {
                readDone = true;
                return new ExportedDocumentImpl(doc, true);
            }
        }
        doc = null;
        return null;
    }

}

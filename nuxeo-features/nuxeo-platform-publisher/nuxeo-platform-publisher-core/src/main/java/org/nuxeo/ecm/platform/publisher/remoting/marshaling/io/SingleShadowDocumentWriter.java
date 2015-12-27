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

import java.io.IOException;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;

/**
 * {@link DocumentModelWriter} that creates a shallow DocumentModel (ie: no path and no uuid).
 *
 * @author tiry
 */
public class SingleShadowDocumentWriter extends AbstractDocumentModelWriter {

    protected DocumentModel dm;

    public SingleShadowDocumentWriter(CoreSession session, String parentPath) {
        super(session, "/");
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc) throws IOException {
        dm = createDocument(doc, null);
        // keep location unchanged
        DocumentLocation oldLoc = doc.getSourceLocation();
        String oldServerName = oldLoc.getServerName();
        DocumentRef oldDocRef = oldLoc.getDocRef();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(oldServerName, oldServerName);
        map.put(oldDocRef, oldDocRef);
        return map;
    }

    @Override
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath) {
        String docType = xdoc.getType();
        dm = session.createDocumentModel(docType);
        // then load schemas data
        loadSchemas(xdoc, dm, xdoc.getDocument());
        return dm;
    }

    public DocumentModel getShadowDocument() {
        return dm;
    }
}

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
 *     bstefanescu
 *
 * $Id: DocumentModelUpdater.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;

/**
 * A writer that only updates existing documents. The doc ID is used to identity documents. The imported tree structure
 * is ignored.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: improve it ->
// modify core session to add a batch create method and use it
public class DocumentModelUpdater extends AbstractDocumentModelWriter {

    private static final Log log = LogFactory.getLog(DocumentModelUpdater.class);

    /**
     * @param session the session to the repository where to write
     * @param parentPath where to write the tree. this document will be used as the parent of all top level documents
     *            passed as input. Note that you may have
     */
    public DocumentModelUpdater(CoreSession session, String parentPath) {
        super(session, parentPath);
    }

    public DocumentModelUpdater(CoreSession session, String parentPath, int saveInterval) {
        super(session, parentPath, saveInterval);
    }

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
        } catch (DocumentNotFoundException e) {
            log.error("Cannot update document. No such document: " + id);
            return null;
        }

        doc = updateDocument(xdoc, doc);
        DocumentLocation source = xdoc.getSourceLocation();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(source.getServerName(), doc.getRepositoryName());
        map.put(source.getDocRef(), doc.getRef());
        return map;
    }

}

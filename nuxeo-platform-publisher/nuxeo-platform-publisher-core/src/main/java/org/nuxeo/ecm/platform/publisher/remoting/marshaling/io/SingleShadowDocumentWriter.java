/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling.io;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
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
 * {@link DocumentModelWriter} that creates a shallow DocumentModel (ie: no
 * path and no uuid).
 *
 * @author tiry
 */
public class SingleShadowDocumentWriter extends AbstractDocumentModelWriter {

    private static final Log log = LogFactory.getLog(SingleShadowDocumentWriter.class);

    protected DocumentModel dm;

    public SingleShadowDocumentWriter(CoreSession session, String parentPath) {
        super(session, "/");
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc)
            throws IOException {

        try {
            dm = createDocument(doc, null);
        } catch (ClientException e) {
            log.error(e, e);
        }
        // keep location unchanged
        DocumentLocation oldLoc = doc.getSourceLocation();
        String oldServerName = oldLoc.getServerName();
        DocumentRef oldDocRef = oldLoc.getDocRef();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(
                oldServerName, oldServerName);
        map.put(oldDocRef, oldDocRef);
        return map;
    }

    @Override
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath)
            throws ClientException {
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

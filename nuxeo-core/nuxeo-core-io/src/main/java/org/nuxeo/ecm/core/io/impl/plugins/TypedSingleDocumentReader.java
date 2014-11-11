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

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.IOException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.TypedExportedDocumentImpl;

/**
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class TypedSingleDocumentReader extends SingleDocumentReader {

    public TypedSingleDocumentReader(CoreSession session, DocumentModel root) {
        super(session, root);
    }

    public TypedSingleDocumentReader(CoreSession session, DocumentRef root)
            throws ClientException {
        this(session, session.getDocument(root));
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (doc != null) {
            if (readDone && !enableRepeatedReads) {
                return null;
            } else {
                readDone = true;
                return new TypedExportedDocumentImpl(doc);
            }
        }
        doc = null;
        return null;
    }

}

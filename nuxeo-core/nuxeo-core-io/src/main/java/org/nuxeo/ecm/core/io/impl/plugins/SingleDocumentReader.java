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
 * $Id: SingleDocumentReader.java 30256 2008-02-18 21:52:11Z tdelprat $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.IOException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SingleDocumentReader extends DocumentModelReader {

    private DocumentModel doc;

    private boolean enableRepeatedReads = false;

    private boolean readDone = false;

    public SingleDocumentReader(CoreSession session, DocumentModel root) {
        super(session);
        doc = root;
    }

    public SingleDocumentReader(CoreSession session, DocumentRef root)
            throws ClientException {
        this(session, session.getDocument(root));
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
            if (readDone && !enableRepeatedReads) {
                return null;
            } else {
                readDone = true;
                return new ExportedDocumentImpl(doc);
            }
        }
        doc = null;
        return null;
    }

    public void setEnableRepeatedReads(boolean enableRepeatedReads) {
        this.enableRepeatedReads = enableRepeatedReads;
    }

}

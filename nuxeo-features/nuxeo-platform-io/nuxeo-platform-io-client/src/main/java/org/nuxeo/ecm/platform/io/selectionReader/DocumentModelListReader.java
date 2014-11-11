/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
        docList = new ArrayList<DocumentModel>();
        docList.addAll(docs);
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (docList == null || docList.isEmpty()) {
            return null;
        }
        return new ExportedDocumentImpl(docList.remove(0));
    }

    public void close() {
        docList = null;
    }

}

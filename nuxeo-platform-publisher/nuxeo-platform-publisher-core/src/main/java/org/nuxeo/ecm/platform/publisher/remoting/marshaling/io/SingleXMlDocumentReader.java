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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelReader;

import java.io.IOException;

/**
 * {@link DocumentModelReader} that reads the {@link DocumentModel} from a String.
 *
 * @author tiry
 */
public class SingleXMlDocumentReader extends AbstractDocumentReader {

    protected Document xmldoc;

    public SingleXMlDocumentReader(String data) throws DocumentException {
        xmldoc = DocumentHelper.parseText(data);
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (xmldoc != null) {
            ExportedDocument xdoc = new ExportedDocumentImpl();
            xdoc.setDocument(xmldoc);
            close();
            return xdoc;
        } else {
            return null;
        }
    }

    public void close() {
        xmldoc = null;
    }

}

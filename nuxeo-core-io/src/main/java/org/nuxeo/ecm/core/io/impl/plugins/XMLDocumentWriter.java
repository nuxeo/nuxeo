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
 *     bstefanescu
 *
 * $Id: XMLDocumentWriter.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;

/**
 * Writes to a file or output stream the XML corresponding to the document
 * content.
 * <p>
 * Note that additional xml descriptors (like relations.xml, workflow.xml etc)
 * are ignored
 * <p>
 * Also blobs are not handled specially. The value existing in the blob data
 * element will be written down. By default blobs are referred as external
 * references, so if their content is not written in the   XML document. If you
 * want to encode blobs as base64 inside the document you must use the
 * {@link InlineBlobTransformer}
 * <p>
 * In order to write Blobs are encoded as Base64 and included in the XML
 * document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XMLDocumentWriter extends AbstractDocumentWriter {

    private static final Log log = LogFactory.getLog(XMLDocumentWriter.class);

    protected final OutputStream out;

    public XMLDocumentWriter(File file) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(file)));
    }

    public XMLDocumentWriter(OutputStream out) {
        this.out = out;
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc)
            throws IOException {

        OutputFormat format = AbstractDocumentWriter.createPrettyPrint();
        XMLWriter writer = null;
        try {
            writer = new XMLWriter(out, format);
            writer.write(doc.getDocument());
        } finally {
            if (writer != null) {
                writer.close();
            }
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

    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

}

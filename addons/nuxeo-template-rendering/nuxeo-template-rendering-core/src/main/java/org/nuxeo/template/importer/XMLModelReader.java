/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * CoreIO reader used to read a exploded XML archive.
 * <p>
 * This format is used here to make changes in the models easier
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XMLModelReader extends AbstractDocumentReader {

    protected File source;

    protected boolean done;

    protected String modelName;

    public XMLModelReader(File source, String modelName) {
        this.source = source;
        this.done = false;
        this.modelName = modelName;
    }

    @Override
    public void close() {
        source = null;
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (done) {
            return null;
        }
        ExportedDocument xdoc = new ExportedDocumentImpl();
        for (File file : source.listFiles()) {
            if (file.isFile()) {
                String name = file.getName();
                if (ExportConstants.DOCUMENT_FILE.equals(name)) {
                    Document doc = loadXML(file);
                    xdoc.setDocument(doc);
                    xdoc.setPath(new Path(modelName));
                } else { // presume a blob
                    xdoc.putBlob(file.getName(), Blobs.createBlob(file));
                }
            }
        }
        done = true;
        return xdoc;
    }

    private static Document loadXML(File file) throws IOException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            return new SAXReader().read(in);
        } catch (DocumentException e) {
            IOException ioe = new IOException("Failed to read file document " + file + ": " + e.getMessage());
            ioe.setStackTrace(e.getStackTrace());
            throw ioe;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

}

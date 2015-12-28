/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template.samples.importer;

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

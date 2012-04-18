/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     btatar
 *
 * $Id: XMLModelReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.template.samples.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;
import org.nuxeo.runtime.services.streaming.FileSource;

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
                    xdoc.putBlob(file.getName(), new StreamingBlob(
                            new FileSource(file)));
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
            IOException ioe = new IOException("Failed to read file document "
                    + file + ": " + e.getMessage());
            ioe.setStackTrace(e.getStackTrace());
            throw ioe;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

}

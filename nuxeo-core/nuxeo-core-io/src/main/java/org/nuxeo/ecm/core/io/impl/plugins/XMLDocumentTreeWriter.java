/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: XMLDocumentTreeWriter.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

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

public class XMLDocumentTreeWriter extends XMLDocumentWriter {

    private static final Log log = LogFactory.getLog(XMLDocumentTreeWriter.class);

    private XMLWriter writer;

    public XMLDocumentTreeWriter(File file) throws IOException {
        super(file);
    }

    public XMLDocumentTreeWriter(OutputStream out) {
        super(out);
    }

    protected XMLWriter initWriter() {
        if (writer == null) {
            try {
                OutputFormat format = AbstractDocumentWriter.createCompactFormat();
                format.setSuppressDeclaration(true);
                writer = new XMLWriter(out, format);
            } catch (UnsupportedEncodingException e) {
                // XXX
            }
        }

        return writer;
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc)
            throws IOException {

        initWriter();
        writer.write(doc.getDocument());
        // out.write(doc.getDocument().asXML().getBytes());

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
    public DocumentTranslationMap write(ExportedDocument[] docs)
            throws IOException {
        initWriter();
        out.write("<documents>".getBytes());
        out.flush();
        DocumentTranslationMap map = super.write(docs);
        writer.flush();
        out.write("</documents>".getBytes());
        return map;
    }

    @Override
    public DocumentTranslationMap write(Collection<ExportedDocument> docs)
            throws IOException {
        initWriter();
        out.write("<documents>".getBytes());
        out.flush();
        DocumentTranslationMap map = super.write(docs);
        writer.flush();
        out.write("</documents>".getBytes());
        return map;
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                // XXX
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

}

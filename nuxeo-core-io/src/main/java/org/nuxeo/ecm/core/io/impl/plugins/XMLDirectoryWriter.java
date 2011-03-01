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
 *     btatar
 *
 * $Id: XMLDirectoryWriter.java 30235 2008-02-18 15:35:09Z fguillaume $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentWriter;

/**
 * This class plays a role in the export pipe.It is used to generate xml files
 * that have a nuxeo specific format.Each file contains information about a
 * document model such as,general information like name, uid or document type,
 * and information about the schemas that the document includes.
 *
 * @author btatar
 */
// XXX AT: is this stil useful?
public class XMLDirectoryWriter extends AbstractDocumentWriter {

    private File destination;

    public XMLDirectoryWriter(String destinationPath) {
        this(new File(destinationPath));
    }

    public XMLDirectoryWriter(File destination) {
        this.destination = destination;
    }

    /**
     * Gives the destination where the XML file will be generated.
     */
    public Object getDestination() {
        return destination;
    }

    /**
     * Sets the destination where the XML file will be generated.
     */
    public void setDestination(File destination) {
        this.destination = destination;
    }

    public void close() {
        destination = null;
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc) throws IOException {

        File file = new File(getDestination() + File.separator
                + doc.getPath().toString());
        if (!file.mkdirs()) {
            throw new IOException("Cannot create target directory: "
                    + file.getAbsolutePath());
        }
        OutputFormat format = AbstractDocumentWriter.createPrettyPrint();
        XMLWriter writer = null;
        try {
            writer = new XMLWriter(new FileOutputStream(file.getAbsolutePath()
                    + File.separator + "document.xml"), format);
            writer.write(doc.getDocument());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        Map<String, Blob> blobs = doc.getBlobs();
        for (Map.Entry<String, Blob> entry : blobs.entrySet()) {
            String blobPath = file.getAbsolutePath() + File.separator
                    + entry.getKey();
            entry.getValue().transferTo(new File(blobPath));
        }

        // write external documents
        for (Map.Entry<String, Document> entry : doc.getDocuments().entrySet()) {
            writer = null;
            try {
                writer = new XMLWriter(new FileOutputStream(file.getAbsolutePath()
                        + File.separator + entry.getKey() + ".xml"), format);
                writer.write(entry.getValue());
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        return null;
    }

}

/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     annejubert
 */

package org.nuxeo.io.fsexporter;

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;

public class CustomExporterPlugin extends DefaultExporterPlugin {

    @Override
    public File serialize(CoreSession session, DocumentModel docfrom, String fsPath) throws IOException {
        // code to export XML
        BlobHolder myblobholder = docfrom.getAdapter(BlobHolder.class);
        String FileXMLNameToExport = "";
        File folder = new File(fsPath);

        if (myblobholder != null) {
            java.util.List<Blob> listblobs = myblobholder.getBlobs();
            int i = 1;
            for (Blob blob : listblobs) {
                // call the method to determine the name of the exported file
                FileXMLNameToExport = getFileName(blob, docfrom, folder, i);
                i++;
            }
            exportFileInXML(session, docfrom, fsPath + "/" + FileXMLNameToExport);
        }
        // export with default exporter all the documents
        return super.serialize(session, docfrom, fsPath);
    }

    protected void exportFileInXML(CoreSession session, DocumentModel docfrom, String pathtoexport) throws IOException {
        DocumentPipe pipe = new DocumentPipeImpl(10);
        SingleDocumentReader reader = new SingleDocumentReader(session, docfrom);
        pipe.setReader(reader);
        XMLDocumentWriter writer = new XMLDocumentWriter(new File(pathtoexport + ".xml"));
        pipe.setWriter(writer);
        pipe.run();
    }

}

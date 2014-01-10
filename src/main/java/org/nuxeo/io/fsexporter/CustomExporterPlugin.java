package org.nuxeo.io.fsexporter;

import java.io.File;

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
    public File serialize(CoreSession session, DocumentModel docfrom,
            String fsPath) throws Exception {

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
            exportFileInXML(session, docfrom, fsPath + "/"
                    + FileXMLNameToExport);
        }
        // export with default exporter all the documents
        super.serialize(session, docfrom, fsPath);
        return null;
    }

    protected void exportFileInXML(CoreSession session, DocumentModel docfrom,
            String pathtoexport) throws Exception {
        DocumentPipe pipe = new DocumentPipeImpl(10);
        SingleDocumentReader reader = new SingleDocumentReader(session, docfrom);
        pipe.setReader(reader);
        XMLDocumentWriter writer = new XMLDocumentWriter(new File(pathtoexport
                + ".xml"));
        pipe.setWriter(writer);
        pipe.run();
    }

}

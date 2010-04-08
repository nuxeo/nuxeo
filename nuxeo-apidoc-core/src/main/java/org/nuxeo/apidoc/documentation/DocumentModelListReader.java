package org.nuxeo.apidoc.documentation;

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

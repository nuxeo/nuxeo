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
 * 
 * {@link DocumentModelReader} that read the {@link DocumentModel} from a String
 * 
 * @author tiry
 * 
 */
public class SingleXMlDocumentReader extends AbstractDocumentReader {

    protected Document xmldoc = null;

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
        } else
            return null;
    }

    public void close() {
        xmldoc = null;
    }

}

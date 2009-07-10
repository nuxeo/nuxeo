package org.nuxeo.ecm.platform.publisher.remoting.marshaling;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.io.SingleDocumentReaderWithInLineBlobs;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.io.SingleShadowDocumentWriter;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.io.SingleXMlDocumentReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * 
 * DocumentModel marsheler using Core IO services
 * 
 * @author tiry
 * 
 */
public class CoreIODocumentModelMarshaler implements DocumentModelMarshaler {

    protected String originatingServer = null;

    public String marshalDocument(DocumentModel doc)
            throws PublishingMarshalingException {

        DocumentWriter writer = null;
        DocumentReader reader = null;
        File tmpFile = null;

        // load the datamodel
        if (originatingServer != null) {
            /*
             * String source = doc.getRepositoryName() + "@" + originatingServer
             * + ":" + doc.getRef().toString();
             */
            String source = new ExtendedDocumentLocation(originatingServer, doc).toString();
            try {
                doc.getPart("dublincore");
                doc.setProperty("dublincore", "source", source);
            } catch (ClientException e) {
                throw new PublishingMarshalingException(e);
            }

        }

        CoreSession coreSession = CoreInstance.getInstance().getSession(
                doc.getSessionId());
        reader = new SingleDocumentReaderWithInLineBlobs(coreSession, doc);

        try {

            tmpFile = File.createTempFile("io-marshaling-", "xml");
            tmpFile.deleteOnExit();
            writer = new XMLDocumentWriter(tmpFile);
            DocumentPipe pipe = new DocumentPipeImpl();
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
            FileReader freader = new FileReader(tmpFile);

            BufferedReader br = new BufferedReader(freader);
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            br.close();
            return sb.toString();

        } catch (Exception e) {
            throw new PublishingMarshalingException(
                    "Unable to marshal DocumentModel", e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    public DocumentModel unMarshalDocument(String data, CoreSession coreSession)
            throws PublishingMarshalingException {

        DocumentWriter writer = null;
        DocumentReader reader = null;

        try {
            reader = new SingleXMlDocumentReader(data);
            writer = new SingleShadowDocumentWriter(coreSession, null);
            DocumentPipe pipe = new DocumentPipeImpl();
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
            return ((SingleShadowDocumentWriter) writer).getShadowDocument();

        } catch (Exception e) {
            throw new PublishingMarshalingException(
                    "Unable to unmarshal DocumentModel", e);
        }
    }

    public void setOriginatingServer(String serverName) {
        this.originatingServer = serverName;
    }

}

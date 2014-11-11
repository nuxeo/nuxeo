/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

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
 * DocumentModel marshaler using Core IO services.
 *
 * @author tiry
 */
public class CoreIODocumentModelMarshaler implements DocumentModelMarshaler {

    protected String originatingServer;

    public String marshalDocument(DocumentModel doc)
            throws PublishingMarshalingException {

        // load the datamodel
        if (originatingServer != null) {
            /*
             * String source = doc.getRepositoryName() + "@" + originatingServer
             * + ":" + doc.getRef().toString();
             */
            String source = new ExtendedDocumentLocation(originatingServer, doc).toString();
            try {
                doc.setProperty("dublincore", "source", source);
            } catch (ClientException e) {
                throw new PublishingMarshalingException(e);
            }

        }

        CoreSession coreSession = CoreInstance.getInstance().getSession(
                doc.getSessionId());
        DocumentReader reader = new SingleDocumentReaderWithInLineBlobs(coreSession, doc);

        File tmpFile = null;
        try {

            tmpFile = File.createTempFile("io-marshaling-", "xml");
            tmpFile.deleteOnExit();
            DocumentWriter writer = new XMLDocumentWriter(tmpFile);
            DocumentPipe pipe = new DocumentPipeImpl();
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
            FileReader freader = new FileReader(tmpFile);

            BufferedReader br = new BufferedReader(freader);
            StringBuilder sb = new StringBuilder();
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

        try {
            DocumentReader reader = new SingleXMlDocumentReader(data);
            DocumentWriter writer = new SingleShadowDocumentWriter(coreSession, null);
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

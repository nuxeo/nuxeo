/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.io.SingleDocumentReaderWithInLineBlobs;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.io.SingleShadowDocumentWriter;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.io.SingleXMlDocumentReader;

/**
 * DocumentModel marshaler using Core IO services.
 *
 * @author tiry
 */
public class CoreIODocumentModelMarshaler implements DocumentModelMarshaler {

    protected String originatingServer;

    @Override
    public String marshalDocument(DocumentModel doc) {

        // load the datamodel
        if (originatingServer != null) {
            String source = new ExtendedDocumentLocation(originatingServer, doc).toString();
            doc.setProperty("dublincore", "source", source);
        }

        CoreSession coreSession = doc.getCoreSession();
        DocumentReader reader = new SingleDocumentReaderWithInLineBlobs(coreSession, doc);

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("io-marshaling-", "xml");
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
        } catch (IOException e) {
            throw new NuxeoException("Unable to marshal DocumentModel", e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    @Override
    public DocumentModel unMarshalDocument(String data, CoreSession coreSession) {
        try {
            DocumentReader reader = new SingleXMlDocumentReader(data);
            DocumentWriter writer = new SingleShadowDocumentWriter(coreSession, null);
            DocumentPipe pipe = new DocumentPipeImpl();
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
            return ((SingleShadowDocumentWriter) writer).getShadowDocument();
        } catch (IOException | DocumentException e) {
            throw new NuxeoException("Unable to unmarshal DocumentModel", e);
        }
    }

    @Override
    public void setOriginatingServer(String serverName) {
        this.originatingServer = serverName;
    }

}

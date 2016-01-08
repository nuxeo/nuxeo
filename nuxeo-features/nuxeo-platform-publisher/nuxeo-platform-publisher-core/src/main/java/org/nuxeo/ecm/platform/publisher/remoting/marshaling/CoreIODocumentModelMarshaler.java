/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.runtime.api.Framework;

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
            tmpFile = Framework.createTempFile("io-marshaling-", "xml");
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
        originatingServer = serverName;
    }

}

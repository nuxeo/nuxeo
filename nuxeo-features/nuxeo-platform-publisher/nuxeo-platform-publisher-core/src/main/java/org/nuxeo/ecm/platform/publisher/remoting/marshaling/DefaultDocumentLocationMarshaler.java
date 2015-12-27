/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.QName;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentLocationMarshaler;

/**
 * {@link DocumentLocation} marshaler using simple XML representation.
 *
 * @author tiry
 */
public class DefaultDocumentLocationMarshaler extends AbstractDefaultXMLMarshaler implements DocumentLocationMarshaler {

    protected String sourceServer;

    protected static QName rootTag = DocumentFactory.getInstance().createQName("documentLocation",
            publisherSerializerNSPrefix, publisherSerializerNS);

    public String marshalDocumentLocation(DocumentLocation docLoc) {
        org.dom4j.Element rootElem = DocumentFactory.getInstance().createElement(rootTag);
        rootElem.addNamespace(publisherSerializerNSPrefix, publisherSerializerNS);
        org.dom4j.Document rootDoc = DocumentFactory.getInstance().createDocument(rootElem);

        rootElem.addAttribute("repository", docLoc.getServerName());
        rootElem.addAttribute("ref", docLoc.getDocRef().toString());

        if (sourceServer != null) {
            rootElem.addAttribute("originalServer", sourceServer);
        }

        String data = rootDoc.asXML();

        return cleanUpXml(data);
    }

    public DocumentLocation unMarshalDocumentLocation(String data) {

        DocumentLocation docLoc;
        try {
            Document document = DocumentHelper.parseText(data);
            org.dom4j.Element rootElem = document.getRootElement();

            String repoName = rootElem.attribute("repository").getValue();
            String refStr = rootElem.attribute("ref").getValue();
            DocumentRef ref = null;
            if (refStr.startsWith("/")) {
                ref = new PathRef(refStr);
            } else {
                ref = new IdRef(refStr);
            }

            if (rootElem.attributeValue("originalServer") != null) {
                docLoc = new ExtendedDocumentLocation(rootElem.attributeValue("originalServer"), repoName, ref);
            } else {
                docLoc = new DocumentLocationImpl(repoName, ref);
            }

        } catch (DocumentException e) {
            throw new NuxeoException("Unable to unmarshal Publication Node", e);
        }
        return docLoc;
    }

    public void setOriginatingServer(String serverName) {
        this.sourceServer = serverName;
    }

}

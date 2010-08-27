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

import org.dom4j.*;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentLocationMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;

/**
 *
 * {@link DocumentLocation} marshaler using simple XML representation.
 *
 * @author tiry
 *
 */
public class DefaultDocumentLocationMarshaler extends
        AbstractDefaultXMLMarshaler implements DocumentLocationMarshaler {

    protected String sourceServer;

    protected static QName rootTag = DocumentFactory.getInstance().createQName(
            "documentLocation", publisherSerializerNSPrefix,
            publisherSerializerNS);

    public String marshalDocumentLocation(DocumentLocation docLoc)
            throws PublishingMarshalingException {
        org.dom4j.Element rootElem = DocumentFactory.getInstance().createElement(
                rootTag);
        rootElem.addNamespace(publisherSerializerNSPrefix,
                publisherSerializerNS);
        org.dom4j.Document rootDoc = DocumentFactory.getInstance().createDocument(
                rootElem);

        rootElem.addAttribute("repository", docLoc.getServerName());
        rootElem.addAttribute("ref", docLoc.getDocRef().toString());

        if (sourceServer != null) {
            rootElem.addAttribute("originalServer", sourceServer);
        }

        String data = rootDoc.asXML();

        return cleanUpXml(data);
    }

    public DocumentLocation unMarshalDocumentLocation(String data)
            throws PublishingMarshalingException {

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
                docLoc = new ExtendedDocumentLocation(
                        rootElem.attributeValue("originalServer"), repoName,
                        ref);
            } else {
                docLoc = new DocumentLocationImpl(repoName, ref);
            }

        } catch (DocumentException e) {
            throw new PublishingMarshalingException(
                    "Unable to unmarshal Piublication Node", e);
        }
        return docLoc;
    }

    public void setOriginatingServer(String serverName) {
        this.sourceServer = serverName;
    }

}

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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublicationNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublicationNodeMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;

/**
 * {@link PublicationNode} marshaler using simple XML representation.
 *
 * @author tiry
 *
 */
public class DefaultPublicationNodeMarshaler extends
        AbstractDefaultXMLMarshaler implements PublicationNodeMarshaler {

    protected static QName rootTag = DocumentFactory.getInstance().createQName(
            "publicationNode", publisherSerializerNSPrefix,
            publisherSerializerNS);

    protected static QName nodePathTag = DocumentFactory.getInstance().createQName(
            "nodePath", publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName nodeTitleTag = DocumentFactory.getInstance().createQName(
            "nodeTile", publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName nodeTypeTag = DocumentFactory.getInstance().createQName(
            "nodeType", publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName treeNameTag = DocumentFactory.getInstance().createQName(
            "treeName", publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName sidTag = DocumentFactory.getInstance().createQName(
            "sid", publisherSerializerNSPrefix, publisherSerializerNS);

    public String marshalPublicationNode(PublicationNode node)
            throws PublishingMarshalingException {

        org.dom4j.Element rootElem = DocumentFactory.getInstance().createElement(
                rootTag);
        rootElem.addNamespace(publisherSerializerNSPrefix,
                publisherSerializerNS);
        org.dom4j.Document rootDoc = DocumentFactory.getInstance().createDocument(
                rootElem);

        org.dom4j.Element pathElem = rootElem.addElement(nodePathTag);
        pathElem.setText(node.getPath());

        org.dom4j.Element titleElem = rootElem.addElement(nodeTitleTag);
        try {
            titleElem.setText(node.getTitle());
        } catch (ClientException e) {
            throw new PublishingMarshalingException(e);
        }

        org.dom4j.Element typeElem = rootElem.addElement(nodeTypeTag);
        typeElem.setText(node.getNodeType());

        org.dom4j.Element treeElem = rootElem.addElement(treeNameTag);
        treeElem.setText(node.getTreeConfigName());

        org.dom4j.Element sidElem = rootElem.addElement(sidTag);
        if (node.getSessionId() != null) {
            sidElem.setText(node.getSessionId());
        } else {
            sidElem.setText("");
        }

        String data = rootDoc.asXML();

        return cleanUpXml(data);
    }

    public PublicationNode unMarshalPublicationNode(String data)
            throws PublishingMarshalingException {
        PublicationNode node = null;

        try {
            Document document = DocumentHelper.parseText(data);
            org.dom4j.Element rootElem = document.getRootElement();

            String nodePath = rootElem.element(nodePathTag).getTextTrim();
            String nodeTitle = rootElem.element(nodeTitleTag).getTextTrim();
            String nodeType = rootElem.element(nodeTypeTag).getTextTrim();
            String treeName = rootElem.element(treeNameTag).getTextTrim();
            String sid = rootElem.element(sidTag).getTextTrim();
            node = new BasicPublicationNode(nodeType, nodePath, nodeTitle,
                    treeName, sid);

        } catch (DocumentException e) {
            throw new PublishingMarshalingException(
                    "Unable to unmarshal Piublication Node", e);
        }
        return node;
    }

}

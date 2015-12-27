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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublicationNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublicationNodeMarshaler;

/**
 * {@link PublicationNode} marshaler using simple XML representation.
 *
 * @author tiry
 */
public class DefaultPublicationNodeMarshaler extends AbstractDefaultXMLMarshaler implements PublicationNodeMarshaler {

    protected static QName rootTag = DocumentFactory.getInstance().createQName("publicationNode",
            publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName nodePathTag = DocumentFactory.getInstance().createQName("nodePath",
            publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName nodeTitleTag = DocumentFactory.getInstance().createQName("nodeTile",
            publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName nodeTypeTag = DocumentFactory.getInstance().createQName("nodeType",
            publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName treeNameTag = DocumentFactory.getInstance().createQName("treeName",
            publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName sidTag = DocumentFactory.getInstance().createQName("sid", publisherSerializerNSPrefix,
            publisherSerializerNS);

    public String marshalPublicationNode(PublicationNode node) {

        org.dom4j.Element rootElem = DocumentFactory.getInstance().createElement(rootTag);
        rootElem.addNamespace(publisherSerializerNSPrefix, publisherSerializerNS);
        org.dom4j.Document rootDoc = DocumentFactory.getInstance().createDocument(rootElem);

        org.dom4j.Element pathElem = rootElem.addElement(nodePathTag);
        pathElem.setText(node.getPath());

        org.dom4j.Element titleElem = rootElem.addElement(nodeTitleTag);
        titleElem.setText(node.getTitle());

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

    public PublicationNode unMarshalPublicationNode(String data) {
        PublicationNode node = null;

        try {
            Document document = DocumentHelper.parseText(data);
            org.dom4j.Element rootElem = document.getRootElement();

            String nodePath = rootElem.element(nodePathTag).getTextTrim();
            String nodeTitle = rootElem.element(nodeTitleTag).getTextTrim();
            String nodeType = rootElem.element(nodeTypeTag).getTextTrim();
            String treeName = rootElem.element(treeNameTag).getTextTrim();
            String sid = rootElem.element(sidTag).getTextTrim();
            node = new BasicPublicationNode(nodeType, nodePath, nodeTitle, treeName, sid);

        } catch (DocumentException e) {
            throw new NuxeoException("Unable to unmarshal Piublication Node", e);
        }
        return node;
    }

}

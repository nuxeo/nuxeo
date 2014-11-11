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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.QName;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentLocationMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublicationNodeMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishedDocumentMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * Default marshaler for RPC calls between 2 servers
 *
 * @author tiry
 *
 */
public class DefaultMarshaler extends AbstractDefaultXMLMarshaler implements
        RemotePublisherMarshaler {

    private static final String PARAM_PATTERN = "$PARAM";

    private static final String RESULT_PATTERN = "$RESULT$";

    protected PublicationNodeMarshaler nodeMarshaler;

    protected PublishedDocumentMarshaler publishedDocumentMarshaler;

    protected DocumentModelMarshaler documentModelMarshaler;

    protected DocumentLocationMarshaler docLocMarshaler;

    protected Map<String, String> params = new HashMap<String, String>();

    protected static QName rootParametersTag = DocumentFactory.getInstance().createQName(
            "parameters", publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName parameterTag = DocumentFactory.getInstance().createQName(
            "parameter", publisherSerializerNSPrefix, publisherSerializerNS);

    protected static QName rootResultTag = DocumentFactory.getInstance().createQName(
            "result", publisherSerializerNSPrefix, publisherSerializerNS);

    protected CoreSession session;

    public DefaultMarshaler() {
        this(null);
    }

    public DefaultMarshaler(CoreSession session) {
        this.nodeMarshaler = new DefaultPublicationNodeMarshaler();
        this.publishedDocumentMarshaler = new DefaultPublishedDocumentMarshaler();
        this.documentModelMarshaler = new CoreIODocumentModelMarshaler();
        this.docLocMarshaler = new DefaultDocumentLocationMarshaler();
        this.session = session;
    }

    public String marshallParameters(List<Object> params)
            throws PublishingMarshalingException {

        if (params == null) {
            return "null";
        }
        String env = buildParameterEnvelope(params.size());
        int idx = 1;
        for (Object param : params) {
            String strParam = marshalSingleObject(param);
            env = env.replace(PARAM_PATTERN + idx + "$", strParam);
            idx += 1;
        }
        return env;
    }

    public String marshallResult(Object result)
            throws PublishingMarshalingException {
        String res = buildResultEnvelope();
        String strResult = marshalSingleObject(result);
        res = res.replace(RESULT_PATTERN, strResult);
        return res;
    }

    public List<Object> unMarshallParameters(String data)
            throws PublishingMarshalingException {
        return unMarshallParameters(data, session);
    }

    protected List<Object> unMarshallParameters(String data, CoreSession session)
            throws PublishingMarshalingException {

        List<Object> params = new ArrayList<Object>();

        Document document;
        try {
            document = DocumentHelper.parseText(data);
            org.dom4j.Element rootElem = document.getRootElement();
            for (Iterator i = rootElem.elementIterator(parameterTag); i.hasNext();) {
                org.dom4j.Element param = (org.dom4j.Element) i.next();
                if (param.elements().size() > 0) {
                    String xmlParam = ((org.dom4j.Element) param.elements().get(
                            0)).asXML();
                    params.add(unMarshalSingleObject(xmlParam, session));
                } else {
                    String value = param.getText();
                    if ("null".equals(value)) {
                        value = null;
                    }
                    params.add(value);
                }
            }
        } catch (DocumentException e) {
            throw new PublishingMarshalingException(
                    "Error during unmarshaling of parameters", e);
        }
        return params;
    }

    public Object unMarshallResult(String data)
            throws PublishingMarshalingException {
        return unMarshallResult(data, session);
    }

    protected Object unMarshallResult(String data, CoreSession coreSession)
            throws PublishingMarshalingException {
        Document document;
        try {
            document = DocumentHelper.parseText(data);
            org.dom4j.Element rootElem = document.getRootElement();

            if (rootElem.elements().size() == 0) {
                return rootElem.getText();
            } else {
                return unMarshalSingleObject(
                        ((org.dom4j.Element) rootElem.elements().get(0)).asXML(),
                        coreSession);
            }
        } catch (DocumentException e) {
            throw new PublishingMarshalingException(
                    "Error during unmarshaling Result", e);
        }
    }

    protected String buildParameterEnvelope(int nbParams) {

        org.dom4j.Element rootElem = DocumentFactory.getInstance().createElement(
                rootParametersTag);
        rootElem.addNamespace(publisherSerializerNSPrefix,
                publisherSerializerNS);
        org.dom4j.Document rootDoc = DocumentFactory.getInstance().createDocument(
                rootElem);

        for (int i = 1; i <= nbParams; i++) {
            org.dom4j.Element pathElem = rootElem.addElement(parameterTag);
            pathElem.setText(PARAM_PATTERN + i + "$");
        }
        return rootDoc.asXML();
    }

    protected String buildResultEnvelope() {

        org.dom4j.Element rootElem = DocumentFactory.getInstance().createElement(
                rootResultTag);
        rootElem.addNamespace(publisherSerializerNSPrefix,
                publisherSerializerNS);
        org.dom4j.Document rootDoc = DocumentFactory.getInstance().createDocument(
                rootElem);
        rootElem.setText(RESULT_PATTERN);
        return rootDoc.asXML();
    }

    protected Object unMarshalSingleObject(String xml, CoreSession coreSession)
            throws PublishingMarshalingException {
        Document document;
        try {
            document = DocumentHelper.parseText(xml);
            org.dom4j.Element rootElem = document.getRootElement();

            QName qname = rootElem.getQName();
            String name = rootElem.getName();

            if (name.equals("publicationNode")) {
                return nodeMarshaler.unMarshalPublicationNode(xml);
            } else if (name.equals("publishedDocument")) {
                return publishedDocumentMarshaler.unMarshalPublishedDocument(xml);
            } else if (name.equals("document")) {
                return documentModelMarshaler.unMarshalDocument(xml,
                        coreSession);
            } else if (name.equals("documentLocation")) {
                return docLocMarshaler.unMarshalDocumentLocation(xml);
            } else if (name.equals("list")) {
                List<Object> lst = new ArrayList<Object>();
                for (Iterator i = rootElem.elementIterator("listitem"); i.hasNext();) {
                    org.dom4j.Element el = (org.dom4j.Element) i.next();
                    if (el.elements().size() == 0) {
                        lst.add(el.getText());
                    } else {
                        lst.add(unMarshalSingleObject(
                                ((org.dom4j.Element) el.elements().get(0)).asXML(),
                                coreSession));
                    }
                }
                return lst;
            } else if (name.equals("map")) {
                Map map = new HashMap();
                for (Iterator i = rootElem.elementIterator("mapitem"); i.hasNext();) {
                    org.dom4j.Element el = (org.dom4j.Element) i.next();

                    Object value = null;
                    if (el.elements().size() > 0) {
                        value = unMarshalSingleObject(
                                ((org.dom4j.Element) (el).elements().get(0)).asXML(),
                                coreSession);
                    } else {
                        value = el.getText();
                    }
                    String key = el.attributeValue("name");
                    map.put(key, value);
                }
                return map;
            }
        } catch (Throwable e) {
            throw new PublishingMarshalingException(
                    "Error during unmarshaling", e);
        }

        throw new PublishingMarshalingException("Unable to unmarshal data");

    }

    protected String marshalSingleObject(Object ob)
            throws PublishingMarshalingException {
        if (ob == null) {
            return "null";
        } else if (ob instanceof String) {
            return (String) ob;
        } else if (ob instanceof DocumentModel) {
            return cleanUpXml(documentModelMarshaler.marshalDocument((DocumentModel) ob));
        } else if (ob instanceof PublicationNode) {
            return nodeMarshaler.marshalPublicationNode((PublicationNode) ob);
        } else if (ob instanceof PublishedDocument) {
            return publishedDocumentMarshaler.marshalPublishedDocument((PublishedDocument) ob);
        } else if (ob instanceof DocumentLocation) {
            return docLocMarshaler.marshalDocumentLocation((DocumentLocation) ob);
        } else if (ob instanceof List) {
            StringBuffer sb = new StringBuffer();
            sb.append("<list>");

            List list = (List) ob;
            for (Object itemOb : list) {
                sb.append("<listitem>");
                sb.append(marshalSingleObject(itemOb));
                sb.append("</listitem>");
            }
            sb.append("</list>");
            return sb.toString();
        } else if (ob instanceof Map) {
            StringBuffer sb = new StringBuffer();
            sb.append("<map>");
            Map map = (Map) ob;
            for (Object key : map.keySet()) {
                sb.append("<mapitem ");
                sb.append("name=\"");
                sb.append((String) key);
                sb.append("\">");
                sb.append(marshalSingleObject(map.get(key)));
                sb.append("</mapitem>");
            }
            sb.append("</map>");
            return sb.toString();
        }

        throw new PublishingMarshalingException("unable to marshal object");

    }

    public void setAssociatedCoreSession(CoreSession session) {
        this.session = session;
    }

    public void setParameters(Map<String, String> params) {
        this.params.putAll(params);
        docLocMarshaler.setOriginatingServer(params.get("originalServer"));
        documentModelMarshaler.setOriginatingServer(params.get("originalServer"));
    }
}

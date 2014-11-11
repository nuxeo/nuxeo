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

package org.nuxeo.ecm.platform.publisher.remoting.restProxies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishingException;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.platform.publisher.remoting.invoker.DefaultRemotePublicationInvoker;
import org.nuxeo.ecm.platform.publisher.remoting.invoker.RemotePublicationInvoker;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;

/**
 * HTTP facade for the {@link RemotePublicationTreeManager} service. This facade
 * uses a invoker to do the actual calls to the remote back-end.
 *
 * @author tiry
 */
public class RemotePublicationTreeManagerRestProxy implements
        RemotePublicationTreeManager {

    private static final Log log = LogFactory.getLog(RemotePublicationTreeManagerRestProxy.class);

    protected String baseURL;

    protected String userName;

    protected String password;

    protected RemotePublisherMarshaler marshaler;

    protected RemotePublicationInvoker invoker;

    public RemotePublicationTreeManagerRestProxy(String baseURL,
            String userName, String password, RemotePublisherMarshaler marshaler) {
        this.baseURL = baseURL;
        this.userName = userName;
        this.password = password;
        this.marshaler = marshaler;

        invoker = new DefaultRemotePublicationInvoker();
        invoker.init(baseURL, userName, password, marshaler);
    }

    public List<PublishedDocument> getChildrenDocuments(PublicationNode node)
            throws ClientException {

        List<Object> params = new ArrayList<Object>();
        params.add(node);
        return (List<PublishedDocument>) invoker.invoke("getChildrenDocuments",
                params);
    }

    public List<PublicationNode> getChildrenNodes(PublicationNode node)
            throws ClientException {

        List<Object> params = new ArrayList<Object>();
        params.add(node);
        return (List<PublicationNode>) invoker.invoke("getChildrenNodes",
                params);
    }

    public List<PublishedDocument> getExistingPublishedDocument(String sid,
            DocumentLocation docLoc) throws ClientException {
        List<Object> params = new ArrayList<Object>();
        params.add(sid);
        params.add(docLoc);
        return (List<PublishedDocument>) invoker.invoke(
                "getExistingPublishedDocument", params);
    }

    public PublicationNode getNodeByPath(String sid, String path)
            throws ClientException {

        List<Object> params = new ArrayList<Object>();
        params.add(sid);
        params.add(path);
        return (PublicationNode) invoker.invoke("getNodeByPath", params);
    }

    public PublicationNode getParent(PublicationNode node) {

        List<Object> params = new ArrayList<Object>();
        params.add(node);
        try {
            return (PublicationNode) invoker.invoke("getParent", params);
        } catch (ClientException e) {
            log.error("Error during parent lookup", e);
            return null;
        }
    }

    public List<PublishedDocument> getPublishedDocumentInNode(
            PublicationNode node) throws ClientException {

        List<Object> params = new ArrayList<Object>();
        params.add(node);
        return (List<PublishedDocument>) invoker.invoke(
                "getPublishedDocumentInNode", params);
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {

        List<Object> params = new ArrayList<Object>();
        params.add(doc);
        params.add(targetNode);

        return (PublishedDocument) invoker.invoke("publish", params);

    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        List<Object> cparams = new ArrayList<Object>();
        cparams.add(doc);
        cparams.add(targetNode);
        cparams.add(params);

        return (PublishedDocument) invoker.invoke("publish", cparams);
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException {

        List<Object> params = new ArrayList<Object>();
        params.add(doc);
        params.add(targetNode);

        invoker.invoke("unpublish", params);
    }

    public void unpublish(String sid, PublishedDocument publishedDocument)
            throws ClientException {
        List<Object> params = new ArrayList<Object>();
        params.add(sid);
        params.add(publishedDocument);

        invoker.invoke("unpublish", params);
    }

    public Map<String, String> initRemoteSession(String treeConfigName,
            Map<String, String> params) throws Exception {

        List<Object> cparams = new ArrayList<Object>();
        cparams.add(treeConfigName);
        cparams.add(params);

        return (Map<String, String>) invoker.invoke("initRemoteSession",
                cparams);
    }

    public void setCurrentDocument(String sid, DocumentModel currentDocument) throws ClientException {
        // The current document is useless on a remote tree
    }

    public void validatorPublishDocument(String sid,
            PublishedDocument publishedDocument, String comment)
            throws PublishingException {
        throw new UnsupportedOperationException();
    }

    public void validatorRejectPublication(String sid, PublishedDocument publishedDocument, String comment) throws PublishingException {
        throw new UnsupportedOperationException();
    }

    public boolean canPublishTo(String sid, PublicationNode publicationNode) throws ClientException {
        if (publicationNode == null || publicationNode.getParent() == null) {
            return false;
        }
        return true;
    }

    public boolean canUnpublish(String sid, PublishedDocument publishedDocument) throws ClientException {
        return true;
    }

    public boolean canManagePublishing(String sid, PublishedDocument publishedDocument) throws ClientException {
        return true;
    }

    public boolean hasValidationTask(String sid, PublishedDocument publishedDocument) throws ClientException {
        return false;
    }

    public PublishedDocument wrapToPublishedDocument(String sid, DocumentModel documentModel) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public boolean isPublicationNode(String sid, DocumentModel documentModel) {
        throw new UnsupportedOperationException();
    }

    public PublicationNode wrapToPublicationNode(String sid, DocumentModel documentModel) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void release(String sid) {
        List<Object> params = new ArrayList<Object>();
        params.add(sid);
        try {
            invoker.invoke("release", params);
        } catch (ClientException e) {
            log.error("Error during release", e);
        }
    }

}

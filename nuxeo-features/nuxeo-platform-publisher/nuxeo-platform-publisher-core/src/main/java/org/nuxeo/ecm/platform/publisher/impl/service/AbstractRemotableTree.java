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

package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;

/**
 * Abstract class for {@link PublicationTree} that delegates method calls to a
 * remote service.
 *
 * @author tiry
 */
public abstract class AbstractRemotableTree implements PublicationTree {

    private static final Log log = LogFactory.getLog(AbstractRemotableTree.class);

    protected RemotePublicationTreeManager treeService;

    protected String sessionId;

    protected String configName;

    protected abstract RemotePublicationTreeManager getTreeService()
            throws ClientException;

    protected abstract String getTargetTreeName();

    protected abstract String getServerTreeSessionId();

    public List<PublishedDocument> getExistingPublishedDocument(
            DocumentLocation docLoc) throws ClientException {
        return getTreeService().getExistingPublishedDocument(
                getServerTreeSessionId(), docLoc);
    }

    public List<PublishedDocument> getPublishedDocumentInNode(
            PublicationNode node) throws ClientException {
        return getTreeService().getPublishedDocumentInNode(
                switchToServerNode(node));
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        return publish(doc, targetNode, null);
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        return getTreeService().publish(doc, switchToServerNode(targetNode),
                params);
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException {
        getTreeService().unpublish(doc, switchToServerNode(targetNode));
    }

    public void unpublish(PublishedDocument publishedDocument)
            throws ClientException {
        getTreeService().unpublish(getServerTreeSessionId(), publishedDocument);
    }

    protected abstract PublicationNode switchToClientNode(PublicationNode node)
            throws ClientException;

    protected abstract PublicationNode switchToServerNode(PublicationNode node);

    protected List<PublicationNode> switchToClientNodes(
            List<PublicationNode> nodes) throws ClientException {
        List<PublicationNode> wrappedNodes = new ArrayList<PublicationNode>();

        for (PublicationNode node : nodes) {
            wrappedNodes.add(switchToClientNode(node));
        }
        return wrappedNodes;
    }

    public PublicationNode getNodeByPath(String path) throws ClientException {
        return switchToClientNode(getTreeService().getNodeByPath(
                getServerTreeSessionId(), path));
    }

    public String getConfigName() {
        return configName;
    }

    public PublicationNode getParent() {
        return null;
    }

    public void setCurrentDocument(DocumentModel currentDocument) throws ClientException {
        getTreeService().setCurrentDocument(getServerTreeSessionId(), currentDocument);
    }

    protected boolean released = false;

    public void release() {
        try {
            if (!released) {
                getTreeService().release(getServerTreeSessionId());
            }
            released = true;
        } catch (ClientException e) {
            log.error("Error during release", e);
        }
    }

    public void validatorPublishDocument(PublishedDocument publishedDocument, String comment) throws ClientException {
        getTreeService().validatorPublishDocument(getServerTreeSessionId(), publishedDocument, comment);
    }

    public void validatorRejectPublication(PublishedDocument publishedDocument, String comment) throws ClientException {
        getTreeService().validatorRejectPublication(getServerTreeSessionId(), publishedDocument, comment);
    }

    public boolean canPublishTo(PublicationNode publicationNode) throws ClientException {
        return getTreeService().canPublishTo(getServerTreeSessionId(), publicationNode);
    }

    public boolean canUnpublish(PublishedDocument publishedDocument) throws ClientException {
        return getTreeService().canUnpublish(getServerTreeSessionId(), publishedDocument);
    }

    public boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException {
        return getTreeService().hasValidationTask(getServerTreeSessionId(), publishedDocument);
    }

    public boolean canManagePublishing(PublishedDocument publishedDocument) throws ClientException {
        return getTreeService().canManagePublishing(getServerTreeSessionId(), publishedDocument);
    }

    public PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) throws ClientException {
        return getTreeService().wrapToPublishedDocument(getServerTreeSessionId(), documentModel);
    }

    public boolean isPublicationNode(DocumentModel documentModel) throws ClientException {
        return getTreeService().isPublicationNode(getServerTreeSessionId(), documentModel);
    }

    public PublicationNode wrapToPublicationNode(DocumentModel documentModel) throws ClientException {
        return getTreeService().wrapToPublicationNode(getServerTreeSessionId(), documentModel);
    }

}

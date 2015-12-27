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

package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;

/**
 * Abstract class for {@link PublicationTree} that delegates method calls to a remote service.
 *
 * @author tiry
 */
public abstract class AbstractRemotableTree implements PublicationTree {

    private static final Log log = LogFactory.getLog(AbstractRemotableTree.class);

    protected RemotePublicationTreeManager treeService;

    protected String sessionId;

    protected String configName;

    protected abstract RemotePublicationTreeManager getTreeService();

    protected abstract String getTargetTreeName();

    protected abstract String getServerTreeSessionId();

    public List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc) {
        return getTreeService().getExistingPublishedDocument(getServerTreeSessionId(), docLoc);
    }

    public List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node) {
        return getTreeService().getPublishedDocumentInNode(switchToServerNode(node));
    }

    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode) {
        return publish(doc, targetNode, null);
    }

    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {
        return getTreeService().publish(doc, switchToServerNode(targetNode), params);
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode) {
        getTreeService().unpublish(doc, switchToServerNode(targetNode));
    }

    public void unpublish(PublishedDocument publishedDocument) {
        getTreeService().unpublish(getServerTreeSessionId(), publishedDocument);
    }

    protected abstract PublicationNode switchToClientNode(PublicationNode node);

    protected abstract PublicationNode switchToServerNode(PublicationNode node);

    protected List<PublicationNode> switchToClientNodes(List<PublicationNode> nodes) {
        List<PublicationNode> wrappedNodes = new ArrayList<PublicationNode>();

        for (PublicationNode node : nodes) {
            wrappedNodes.add(switchToClientNode(node));
        }
        return wrappedNodes;
    }

    public PublicationNode getNodeByPath(String path) {
        return switchToClientNode(getTreeService().getNodeByPath(getServerTreeSessionId(), path));
    }

    public String getConfigName() {
        return configName;
    }

    public PublicationNode getParent() {
        return null;
    }

    public void setCurrentDocument(DocumentModel currentDocument) {
        getTreeService().setCurrentDocument(getServerTreeSessionId(), currentDocument);
    }

    protected boolean released = false;

    public void release() {
        if (!released) {
            getTreeService().release(getServerTreeSessionId());
        }
        released = true;
    }

    public void validatorPublishDocument(PublishedDocument publishedDocument, String comment) {
        getTreeService().validatorPublishDocument(getServerTreeSessionId(), publishedDocument, comment);
    }

    public void validatorRejectPublication(PublishedDocument publishedDocument, String comment) {
        getTreeService().validatorRejectPublication(getServerTreeSessionId(), publishedDocument, comment);
    }

    public boolean canPublishTo(PublicationNode publicationNode) {
        return getTreeService().canPublishTo(getServerTreeSessionId(), publicationNode);
    }

    public boolean canUnpublish(PublishedDocument publishedDocument) {
        return getTreeService().canUnpublish(getServerTreeSessionId(), publishedDocument);
    }

    public boolean hasValidationTask(PublishedDocument publishedDocument) {
        return getTreeService().hasValidationTask(getServerTreeSessionId(), publishedDocument);
    }

    public boolean canManagePublishing(PublishedDocument publishedDocument) {
        return getTreeService().canManagePublishing(getServerTreeSessionId(), publishedDocument);
    }

    public PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) {
        return getTreeService().wrapToPublishedDocument(getServerTreeSessionId(), documentModel);
    }

    public boolean isPublicationNode(DocumentModel documentModel) {
        return getTreeService().isPublicationNode(getServerTreeSessionId(), documentModel);
    }

    public PublicationNode wrapToPublicationNode(DocumentModel documentModel) {
        return getTreeService().wrapToPublicationNode(getServerTreeSessionId(), documentModel);
    }

}

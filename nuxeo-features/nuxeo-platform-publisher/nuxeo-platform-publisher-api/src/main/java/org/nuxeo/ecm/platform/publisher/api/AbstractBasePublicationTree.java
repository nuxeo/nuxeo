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

package org.nuxeo.ecm.platform.publisher.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public abstract class AbstractBasePublicationTree implements PublicationTree {

    public static final String ROOT_PATH_KEY = "RootPath";

    public static final String ICON_EXPANDED_KEY = "iconExpanded";

    public static final String ICON_COLLAPSED_KEY = "iconCollapsed";

    public static final String TITLE_KEY = "title";

    protected PublicationNode rootNode;

    protected PublishedDocumentFactory factory;

    protected CoreSession coreSession;

    protected String configName;

    protected String sid;

    protected String rootPath;

    protected String treeTitle;

    protected String iconCollapsed = "/icons/folder.gif";

    protected String iconExpanded = "/icons/folder_open.gif";

    protected abstract String getDefaultRootPath();

    protected abstract PublishedDocumentFactory getDefaultFactory();

    public void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, String title)
            throws ClientException {
        this.sid = sid;
        this.coreSession = coreSession;
        if (factory != null) {
            this.factory = factory;
        } else {
            this.factory = getDefaultFactory();
            this.factory.init(coreSession, parameters);
        }

        if (parameters.containsKey(ROOT_PATH_KEY)) {
            rootPath = parameters.get(ROOT_PATH_KEY);
        } else {
            rootPath = getDefaultRootPath();
        }

        if (parameters.containsKey(ICON_COLLAPSED_KEY)) {
            iconCollapsed = parameters.get(ICON_COLLAPSED_KEY);
        }
        if (parameters.containsKey(ICON_EXPANDED_KEY)) {
            iconExpanded = parameters.get(ICON_EXPANDED_KEY);
        }
        treeTitle = title != null ? title : configName;

        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public String getSessionId() {
        return sid;
    }

    public String getNodeType() {
        return rootNode.getNodeType();
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public String getTreeType() {
        return getType();
    }

    public String getTreeTitle() {
        return treeTitle;
    }

    public List<PublishedDocument> getPublishedDocumentInNode(
            PublicationNode node) throws ClientException {
        return node.getChildrenDocuments();
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        return factory.publishDocument(doc, targetNode);
    }

    public PublishedDocument publish(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        return factory.publishDocument(doc, targetNode, params);
    }

    public String getTitle() throws ClientException {
        return rootNode.getTitle();
    }

    public String getName() throws ClientException {
        return rootNode.getName();
    }

    public String getTreeConfigName() {
        return getConfigName();
    }

    public PublicationNode getParent() {
        return null;
    }

    public List<PublicationNode> getChildrenNodes() throws ClientException {
        return rootNode.getChildrenNodes();
    }

    public List<PublishedDocument> getChildrenDocuments()
            throws ClientException {
        return rootNode.getChildrenDocuments();
    }

    public String getPath() {
        return rootNode.getPath();
    }

    public void setCurrentDocument(DocumentModel currentDocument) {
        // Not used by default
    }

    public String getIconExpanded() {
        return iconExpanded;
    }

    public String getIconCollapsed() {
        return iconCollapsed;
    }

    public void validatorPublishDocument(PublishedDocument publishedDocument,
            String comment) throws ClientException {
        if (!accept(publishedDocument)) {
            return;
        }
        factory.validatorPublishDocument(publishedDocument, comment);
    }

    public void validatorRejectPublication(PublishedDocument publishedDocument,
            String comment) throws ClientException {
        if (!accept(publishedDocument)) {
            return;
        }
        factory.validatorRejectPublication(publishedDocument, comment);
    }

    public boolean canPublishTo(PublicationNode publicationNode) throws ClientException {
        if (publicationNode == null || publicationNode.getParent() == null) {
            // we can't publish in the root node
            return false;
        }
        return true;
    }

    public boolean canUnpublish(PublishedDocument publishedDocument) throws ClientException {
        if (!accept(publishedDocument)) {
            return false;
        }
        return true;
    }

    public boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException {
        if (!accept(publishedDocument)) {
            return false;
        }
        return factory.hasValidationTask(publishedDocument);
    }

    public boolean canManagePublishing(PublishedDocument publishedDocument) throws ClientException {
        if (!accept(publishedDocument)) {
            return false;
        }
        return factory.canManagePublishing(publishedDocument);
    }

    public PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) throws ClientException {
        return factory.wrapDocumentModel(documentModel);
    }

    public boolean isPublicationNode(DocumentModel documentModel) throws ClientException {
        return false;
    }

    public PublicationNode wrapToPublicationNode(DocumentModel documentModel) throws ClientException {
        throw new UnsupportedOperationException("");
    }

    protected abstract boolean accept(PublishedDocument publishedDocument);

}

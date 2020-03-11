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

package org.nuxeo.ecm.platform.publisher.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public abstract class AbstractBasePublicationTree implements PublicationTree {

    private static final long serialVersionUID = 1L;

    public static final String ROOT_PATH_KEY = "RootPath";

    public static final String ICON_EXPANDED_KEY = "iconExpanded";

    public static final String ICON_COLLAPSED_KEY = "iconCollapsed";

    public static final String TITLE_KEY = "title";

    protected PublicationNode rootNode;

    protected PublishedDocumentFactory factory;

    protected CoreSession coreSession;

    protected String configName;

    protected String rootPath;

    protected String treeTitle;

    protected String iconCollapsed = "/icons/folder.gif";

    protected String iconExpanded = "/icons/folder_open.gif";

    protected abstract String getDefaultRootPath();

    protected abstract PublishedDocumentFactory getDefaultFactory();

    @Override
    public void initTree(CoreSession coreSession, Map<String, String> parameters,
            PublishedDocumentFactory factory, String configName, String title) {
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

    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public String getNodeType() {
        return rootNode.getNodeType();
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getTreeType() {
        return getType();
    }

    @Override
    public String getTreeTitle() {
        return treeTitle;
    }

    @Override
    public List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node) {
        return node.getChildrenDocuments();
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode) {
        return factory.publishDocument(doc, targetNode);
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {
        return factory.publishDocument(doc, targetNode, params);
    }

    @Override
    public String getTitle() {
        return rootNode.getTitle();
    }

    @Override
    public String getName() {
        return rootNode.getName();
    }

    @Override
    public PublicationTree getTree() {
        return this;
    }

    @Override
    public PublicationNode getParent() {
        return null;
    }

    @Override
    public List<PublicationNode> getChildrenNodes() {
        return rootNode.getChildrenNodes();
    }

    @Override
    public List<PublishedDocument> getChildrenDocuments() {
        return rootNode.getChildrenDocuments();
    }

    @Override
    public String getPath() {
        return rootNode.getPath();
    }

    @Override
    public void setCurrentDocument(DocumentModel currentDocument) {
        // Not used by default
    }

    @Override
    public String getIconExpanded() {
        return iconExpanded;
    }

    @Override
    public String getIconCollapsed() {
        return iconCollapsed;
    }

    @Override
    public void validatorPublishDocument(PublishedDocument publishedDocument, String comment) {
        if (!accept(publishedDocument)) {
            return;
        }
        factory.validatorPublishDocument(publishedDocument, comment);
    }

    @Override
    public void validatorRejectPublication(PublishedDocument publishedDocument, String comment) {
        if (!accept(publishedDocument)) {
            return;
        }
        factory.validatorRejectPublication(publishedDocument, comment);
    }

    @Override
    public boolean canPublishTo(PublicationNode publicationNode) {
        if (publicationNode == null || publicationNode.getParent() == null) {
            // we can't publish in the root node
            return false;
        }
        return true;
    }

    @Override
    public boolean canUnpublish(PublishedDocument publishedDocument) {
        if (!accept(publishedDocument)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasValidationTask(PublishedDocument publishedDocument) {
        if (!accept(publishedDocument)) {
            return false;
        }
        return factory.hasValidationTask(publishedDocument);
    }

    @Override
    public boolean canManagePublishing(PublishedDocument publishedDocument) {
        if (!accept(publishedDocument)) {
            return false;
        }
        return factory.canManagePublishing(publishedDocument);
    }

    @Override
    public PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) {
        return factory.wrapDocumentModel(documentModel);
    }

    @Override
    public boolean isPublicationNode(DocumentModel documentModel) {
        return false;
    }

    @Override
    public PublicationNode wrapToPublicationNode(DocumentModel documentModel) {
        throw new UnsupportedOperationException("");
    }

    protected abstract boolean accept(PublishedDocument publishedDocument);

}

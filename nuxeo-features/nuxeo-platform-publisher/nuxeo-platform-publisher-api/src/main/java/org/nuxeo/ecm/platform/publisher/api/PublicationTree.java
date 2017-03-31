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
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for the publication tree. A Publication Tree is a generic view on a set of PublicationNode.
 *
 * @author tiry
 */
public interface PublicationTree extends PublicationNode {

    PublicationNode getNodeByPath(String path);

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode);

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params);

    void unpublish(DocumentModel doc, PublicationNode targetNode);

    void unpublish(PublishedDocument publishedDocument);

    List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc);

    List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node);

    String getConfigName();

    String getTreeType();

    String getTreeTitle();

    void initTree(CoreSession coreSession, Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, String title);

    /**
     * Sets the current document on which the tree will be based, if needed.
     * <p>
     * Can be useful for some implementations that need to know on which document the user is.
     *
     * @param currentDocument the current document
     */
    void setCurrentDocument(DocumentModel currentDocument);

    void release();

    String getIconExpanded();

    String getIconCollapsed();

    /**
     * A validator (the current user) approves the publication.
     *
     * @param publishedDocument the current published document that will be approved
     * @param comment
     */
    void validatorPublishDocument(PublishedDocument publishedDocument, String comment);

    /**
     * A validator (the current user) rejects the publication.
     *
     * @param publishedDocument the currently published document that will be rejected
     * @param comment
     */
    void validatorRejectPublication(PublishedDocument publishedDocument, String comment);

    /**
     * Returns {@code true} if the current user can publish to the specified publicationNode, {@code false} otherwise.
     *
     * @return {@code true} if the current user can publish to the specified publicationNode, {@code false} otherwise.
     */
    boolean canPublishTo(PublicationNode publicationNode);

    /**
     * Returns {@code true} if the current user can unpublish the given publishedDocument, {@code false} otherwise.
     *
     * @return {@code true} if the current user can unpublish the given publishedDocument, {@code false} otherwise.
     */
    boolean canUnpublish(PublishedDocument publishedDocument);

    boolean hasValidationTask(PublishedDocument publishedDocument);

    /**
     * Returns {@code true} if the current user can manage the publishing of the given publishedDocument, ie approve or
     * reject the document.
     */
    boolean canManagePublishing(PublishedDocument publishedDocument);

    PublishedDocument wrapToPublishedDocument(DocumentModel documentModel);

    /**
     * Returns {@code true} if the given {@code documentModel} is a PublicationNode of the current tree, {@code false}
     * otherwise.
     */
    boolean isPublicationNode(DocumentModel documentModel);

    /**
     * Returns a PublicationNode for the current tree built on the given {@code documentModel}.
     */
    PublicationNode wrapToPublicationNode(DocumentModel documentModel);

}

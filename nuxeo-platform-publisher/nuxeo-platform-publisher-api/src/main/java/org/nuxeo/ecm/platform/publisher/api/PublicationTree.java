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
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for the publication tree. A Publication Tree is a generic view on a
 * set of PublicationNode.
 *
 * @author tiry
 */
public interface PublicationTree extends PublicationNode {

    PublicationNode getNodeByPath(String path) throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode,
            Map<String, String> params) throws ClientException;

    void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    void unpublish(PublishedDocument publishedDocument) throws ClientException;

    List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc)
            throws ClientException;

    List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node)
            throws ClientException;

    String getConfigName();

    String getTreeType();

    String getTreeTitle();

    void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, String title) throws ClientException;

    /**
     * Sets the current document on which the tree will be based, if needed.
     * <p>
     * Can be useful for some implementations that need to know on which document the user is.
     *
     * @param currentDocument the current document
     */
    void setCurrentDocument(DocumentModel currentDocument) throws ClientException;

    void release();

    String getIconExpanded();

    String getIconCollapsed();

    /**
     * A validator (the current user) approves the publication.
     *
     * @param publishedDocument the current published document that will be
     *            approved
     * @param comment
     */
    void validatorPublishDocument(PublishedDocument publishedDocument, String comment)
            throws ClientException;

    /**
     * A validator (the current user) rejects the publication.
     *
     * @param publishedDocument the currently published document that will be
     *            rejected
     * @param comment
     */
    void validatorRejectPublication(PublishedDocument publishedDocument,
            String comment) throws ClientException;

    /**
     * Returns {@code true} if the current user can publish to the specified publicationNode,
     * {@code false} otherwise.
     *
     * @return {@code true} if the current user can publish to the specified publicationNode, {@code false} otherwise.
     */
    boolean canPublishTo(PublicationNode publicationNode) throws ClientException;

    /**
     * Returns {@code true} if the current user can unpublish the given publishedDocument,
     * {@code false} otherwise.
     *
     * @return {@code true} if the current user can unpublish the given publishedDocument, {@code false} otherwise.
     */
    boolean canUnpublish(PublishedDocument publishedDocument) throws ClientException;

    boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException;

    /**
     * Returns {@code true} if the current user can manage the publishing of the given publishedDocument,
     * ie approve or reject the document.
     */
    boolean canManagePublishing(PublishedDocument publishedDocument) throws ClientException;

    PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) throws ClientException;

    /**
     * Returns {@code true} if the given {@code documentModel} is a PublicationNode of the current tree,
     * {@code false} otherwise.
     */
    boolean isPublicationNode(DocumentModel documentModel) throws ClientException;

    /**
     * Returns a PublicationNode for the current tree built on the given {@code documentModel}.
     *
     * @throws ClientException if the given documentModel cannot be a PublicationNode
     */
    PublicationNode wrapToPublicationNode(DocumentModel documentModel) throws ClientException;

}

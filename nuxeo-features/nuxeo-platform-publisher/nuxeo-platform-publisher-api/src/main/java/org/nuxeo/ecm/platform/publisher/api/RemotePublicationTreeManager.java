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

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Remote interface used by PublicationService to communicate with each others.
 *
 * @author tiry
 */
public interface RemotePublicationTreeManager {

    List<PublishedDocument> getChildrenDocuments(PublicationNode node);

    List<PublicationNode> getChildrenNodes(PublicationNode node);

    PublicationNode getParent(PublicationNode node);

    PublicationNode getNodeByPath(String sid, String path);

    List<PublishedDocument> getExistingPublishedDocument(String sid, DocumentLocation docLoc);

    List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node);

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode);

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params);

    void unpublish(DocumentModel doc, PublicationNode targetNode);

    void unpublish(String sid, PublishedDocument publishedDocument);

    Map<String, String> initRemoteSession(String treeConfigName, Map<String, String> params);

    /**
     * Sets the current document on which the tree will be based, if needed. Can be useful for some implementations that
     * need to know on which document the user is.
     *
     * @param currentDocument the current document
     */
    void setCurrentDocument(String sid, DocumentModel currentDocument);

    void release(String sid);

    /**
     * A validator (the current user) approves the publication.
     *
     * @param publishedDocument the current published document that will be approved
     * @param comment
     */
    void validatorPublishDocument(String sid, PublishedDocument publishedDocument, String comment);

    /**
     * A validator (the current user) rejects the publication.
     *
     * @param publishedDocument the currently published document that will be rejected
     * @param comment
     */
    void validatorRejectPublication(String sid, PublishedDocument publishedDocument, String comment);

    /**
     * Returns {@code true} if the current user can publish to the specified publicationNode, {@code false} otherwise.
     *
     * @return {@code true} if the current user can publish to the specified publicationNode, {@code false} otherwise.
     */
    boolean canPublishTo(String sid, PublicationNode publicationNode);

    /**
     * Returns {@code true} if the current user can unpublish the given publishedDocument, {@code false} otherwise.
     *
     * @return {@code true} if the current user can unpublish the given publishedDocument, {@code false} otherwise.
     */
    boolean canUnpublish(String sid, PublishedDocument publishedDocument);

    boolean hasValidationTask(String sid, PublishedDocument publishedDocument);

    /**
     * Returns {@code true} if the current user can manage the publishing of the given published document, ie. approve
     * or reject the document.
     */
    boolean canManagePublishing(String sid, PublishedDocument publishedDocument);

    PublishedDocument wrapToPublishedDocument(String sid, DocumentModel documentModel);

    /**
     * Returns {@code true} if the given {@code documentModel} is a PublicationNode of the current tree, {@code false}
     * otherwise.
     */
    boolean isPublicationNode(String sid, DocumentModel documentModel);

    /**
     * Returns a PublicationNode for the current tree built on the given {@code documentModel}.
     */
    PublicationNode wrapToPublicationNode(String sid, DocumentModel documentModel);

}

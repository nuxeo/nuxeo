/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.publishing.api;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author arussel
 */
public interface Publisher {

    public enum PublishingEvent {
        documentPublished, documentSubmittedForPublication, documentPublicationRejected,
        documentPublicationApproved, documentWaitingPublication, documentUnpublished
    }

    void unpublish(DocumentModel document, NuxeoPrincipal principal)
            throws PublishingException;

    void unpublish(List<DocumentModel> documents, NuxeoPrincipal principal)
            throws PublishingException;

    void submitToPublication(DocumentModel document,
            DocumentModel placeToPublishTo, NuxeoPrincipal principal)
            throws PublishingException, DocumentWaitingValidationException;

    boolean hasValidationTask(DocumentModel proxy, NuxeoPrincipal currentUser)
            throws PublishingException;

    void validatorPublishDocument(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException;

    void validatorRejectPublication(DocumentModel doc,
            NuxeoPrincipal principal, String comment)
            throws PublishingException;

    boolean canManagePublishing(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException;

    /**
     * Checks if this proxy is published.
     */
    boolean isPublished(DocumentModel proxy) throws PublishingException;

}

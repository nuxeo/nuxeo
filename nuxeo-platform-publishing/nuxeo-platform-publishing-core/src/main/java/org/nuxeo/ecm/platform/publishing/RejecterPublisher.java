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
package org.nuxeo.ecm.platform.publishing;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.publishing.api.DocumentWaitingValidationException;
import org.nuxeo.ecm.platform.publishing.api.Publisher;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;

/**
 * @author arussel
 *
 */
public class RejecterPublisher extends AbstractPublisher implements Publisher {

    public boolean canManagePublishing(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) {
        return false;
    }

    public boolean isPublished(DocumentModel document) {
        //if it is there, then it is published
        return document != null;
    }

    public boolean hasValidationTask(DocumentModel proxy,
            NuxeoPrincipal currentUser) {
        return false;
    }

    public void submitToPublication(DocumentModel document,
            DocumentModel placeToPublishTo, NuxeoPrincipal principal)
            throws PublishingException {
        CoreSession coreSession = null;
        try {
            coreSession = getCoreSession(document, principal);
            boolean docPerm = coreSession.hasPermission(document.getRef(),
                    SecurityConstants.READ);
            boolean secPerm = coreSession.hasPermission(
                    placeToPublishTo.getRef(), SecurityConstants.ADD_CHILDREN);
            if (docPerm && secPerm) {
                publish(document, placeToPublishTo, principal);
            } else {
                throw new DocumentWaitingValidationException();
            }
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    public void validatorPublishDocument(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) {
        // always rejeted
    }

    public void validatorRejectPublication(DocumentModel doc,
            NuxeoPrincipal principal, String comment) {
        // rejected for no reason
    }

}

/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingServiceBean.java 28957 2008-01-11 13:36:52Z tdelprat $
 */

package org.nuxeo.ecm.platform.publishing;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.publishing.api.PublishingValidatorException;
import org.nuxeo.ecm.platform.publishing.api.ValidatorsRule;
import org.nuxeo.runtime.api.Framework;

/**
 * Publishing service session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(PublishingService.class)
@Remote(PublishingService.class)
public class PublishingServiceBean implements PublishingService {

    private PublishingService service;

    @PostConstruct
    void postConstruct() {
        try {
            service = Framework.getService(PublishingService.class);
        } catch (Exception e) {
            throw new IllegalStateException("Publishing service not deployed.", e);
        }
    }

    public String getValidDateFieldName() {
        return service.getValidDateFieldName();
    }

    public String getValidDateFieldSchemaPrefixName() {
        return service.getValidDateFieldSchemaPrefixName();
    }

    public String[] getValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException {
        return service.getValidatorsFor(dm);
    }

    public ValidatorsRule getValidatorsRule() throws PublishingValidatorException {
        return service.getValidatorsRule();
    }

    public boolean canManagePublishing(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException {
        return service.canManagePublishing(currentDocument, currentUser);
    }

    public boolean hasValidationTask(DocumentModel proxy,
            NuxeoPrincipal currentUser) throws PublishingException {
        return service.hasValidationTask(proxy, currentUser);
    }

    public boolean isPublished(DocumentModel proxy) throws PublishingException {
        return service.isPublished(proxy);
    }

    public void submitToPublication(DocumentModel document,
            DocumentModel placeToPublishTo, NuxeoPrincipal principal)
            throws PublishingException {
        service.submitToPublication(document, placeToPublishTo, principal);
    }

    public void validatorPublishDocument(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException {
        service.validatorPublishDocument(currentDocument, currentUser);
    }

    public void validatorRejectPublication(DocumentModel doc,
            NuxeoPrincipal principal, String comment) throws PublishingException {
        service.validatorRejectPublication(doc, principal, comment);
    }

    public void unpublish(DocumentModel document, NuxeoPrincipal principal)
            throws PublishingException {
        service.unpublish(document, principal);
    }

    public void unpublish(List<DocumentModel> documents,
            NuxeoPrincipal principal) throws PublishingException {
        service.unpublish(documents, principal);
    }

}

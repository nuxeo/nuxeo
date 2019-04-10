/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.jsf;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.rendition.TemplateBasedRenditionProvider;

public class BaseTemplateAction implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(BaseTemplateAction.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected List<TemplateInput> templateEditableInputs;

    protected TemplateInput newInput;

    public boolean canAddTemplateInputs() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!documentManager.hasPermission(currentDocument.getRef(), SecurityConstants.WRITE)) {
            return false;
        }
        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        return template != null ? true : false;
    }

    public boolean canUpdateTemplateInputs(String templateName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!documentManager.hasPermission(currentDocument.getRef(), SecurityConstants.WRITE)) {
            return false;
        }
        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {
            return true;
        }
        TemplateBasedDocument templateBased = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBased != null) {
            return templateBased.hasEditableParams(templateName);
        }
        return false;
    }

    public boolean canResetParameters() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!documentManager.hasPermission(currentDocument.getRef(), SecurityConstants.WRITE)) {
            return false;
        }
        TemplateBasedDocument templateBased = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBased != null) {
            return true;
        }
        return false;
    }

    public TemplateSourceDocument getCurrentDocumentAsTemplateSourceDocument() {
        return navigationContext.getCurrentDocument().getAdapter(TemplateSourceDocument.class);
    }

    public DocumentModel resolveTemplateById(String uuid) {
        try {
            return documentManager.getDocument(new IdRef(uuid));
        } catch (DocumentNotFoundException e) {
            return null;
        }
    }

    public List<RenditionDefinition> getRenditions() {
        RenditionService rs = Framework.getService(RenditionService.class);
        return rs.getDeclaredRenditionDefinitionsForProviderType(TemplateBasedRenditionProvider.class.getSimpleName());
    }

    public List<TemplateSourceDocument> getAvailableOfficeTemplates(String targetType) {
        TemplateProcessorService tps = Framework.getService(TemplateProcessorService.class);
        return tps.getAvailableOfficeTemplates(documentManager, targetType);
    }

    public String addTemplateInput() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {
            if (template.hasInput(newInput.getName())) {
                facesMessages.add(Severity.WARN, messages.get("label.template.parameter.already.exist"),
                        newInput.getName());
                return null;
            }
            template.addInput(newInput);
            newInput = null;
            templateEditableInputs = null;
        } else {
            return null;
        }
        return navigationContext.navigateToDocument(currentDocument);
    }

}

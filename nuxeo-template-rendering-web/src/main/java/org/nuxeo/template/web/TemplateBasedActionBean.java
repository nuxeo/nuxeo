/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.template.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@Name("templateBasedActions")
@Scope(ScopeType.CONVERSATION)
public class TemplateBasedActionBean extends BaseTemplateAction {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient DocumentActions documentActions;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    protected List<TemplateInput> templateInputs;

    protected String templateIdToAssociate;

    protected String editableTemplateName;

    public String createTemplate() {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        TemplateSourceDocument sourceTemplate = changeableDocument.getAdapter(TemplateSourceDocument.class);
        if (sourceTemplate != null && sourceTemplate.getTemplateBlob() != null) {
            try {
                sourceTemplate.initTemplate(false);
                if (sourceTemplate.hasEditableParams()) {
                    templateInputs = sourceTemplate.getParams();
                    return "editTemplateRelatedData";
                }
            } catch (PropertyException e) {
                log.error("Error during parameter automatic initialization", e);
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get("label.template.err.parameterInit"));
            }
        }
        return documentActions.saveDocument(changeableDocument);
    }

    public List<TemplateInput> getTemplateInputs() {
        return templateInputs;
    }

    public void setTemplateInputs(List<TemplateInput> templateInputs) {
        this.templateInputs = templateInputs;
    }

    public String saveDocument() {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();

        for (TemplateInput ti : templateInputs) {
            log.info(ti.toString());
        }
        TemplateSourceDocument source = changeableDocument.getAdapter(TemplateSourceDocument.class);
        if (source != null) {
            source.saveParams(templateInputs, false);
        }

        return documentActions.saveDocument(changeableDocument);
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED, EventNames.NEW_DOCUMENT_CREATED,
            EventNames.DOCUMENT_CHANGED }, create = false)
    @BypassInterceptors
    public void reset() {
        templateInputs = null;
        templateEditableInputs = null;
        editableTemplateName = null;
        templateIdToAssociate = null;
    }

    public List<TemplateInput> getTemplateEditableInputs() {
        if (editableTemplateName == null) {
            return new ArrayList<>();
        }
        if (templateEditableInputs == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            TemplateBasedDocument templateBasedDoc = currentDocument.getAdapter(TemplateBasedDocument.class);
            templateEditableInputs = templateBasedDoc.getParams(editableTemplateName);
        }
        return templateEditableInputs;
    }

    public void setTemplateEditableInputs(List<TemplateInput> templateEditableInputs) {
        this.templateEditableInputs = templateEditableInputs;
    }

    public String saveTemplateInputs() {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateBasedDocument template = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (template != null) {
            currentDocument = template.saveParams(editableTemplateName, templateEditableInputs, true);
        }
        reset();
        navigationContext.invalidateCurrentDocument();
        return navigationContext.navigateToDocument(currentDocument);
    }

    public void cancelTemplateInputsEdit() {
        reset();
    }

    public TemplateInput getNewInput() {
        if (newInput == null) {
            newInput = new TemplateInput("newField");
        }
        return newInput;
    }

    public void setNewInput(TemplateInput newInput) {
        this.newInput = newInput;
    }

    public String render(String templateName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (doc == null) {
            return null;
        }
        try {
            Blob rendition = doc.renderWithTemplate(templateName);
            String filename = rendition.getFilename();
            ComponentUtils.download(currentDocument, null, rendition, filename, "templateRendition");
            return null;
        } catch (NuxeoException e) {
            log.error("Unable to render template ", e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.template.err.renderingFailed"));
            return null;
        }
    }

    public String renderAndStore(String templateName) {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (doc == null) {
            return null;
        }
        doc.renderAndStoreAsAttachment(templateName, true);
        documentManager.save();
        return navigationContext.navigateToDocument(doc.getAdaptedDoc());
    }

    @Override
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

    public void resetParameters(String templateName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument templateBased = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBased != null) {
            templateBased.initializeFromTemplate(templateName, true);
            templateEditableInputs = null;
        }
    }

    public boolean canDetachTemplate(String templateName) {
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

    public String detachTemplate(String templateName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        DocumentModel detachedDocument = tps.detachTemplateBasedDocument(currentDocument, templateName, true);
        webActions.resetTabList();
        // because of cacheKey issue
        navigationContext.setCurrentDocument(null);
        return navigationContext.navigateToDocument(detachedDocument);
    }

    public String getTemplateIdToAssociate() {
        return templateIdToAssociate;
    }

    public void setTemplateIdToAssociate(String templateIdToAssociate) {
        this.templateIdToAssociate = templateIdToAssociate;
    }

    public void associateDocumentToTemplate() {
        if (templateIdToAssociate == null) {
            // return null;
            return;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentModel sourceTemplate = documentManager.getDocument(new IdRef(templateIdToAssociate));
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        try {
            currentDocument = tps.makeTemplateBasedDocument(currentDocument, sourceTemplate, true);
        } catch (NuxeoException e) {
            log.error("Unable to do template association", e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.template.err.associationFailed"),
                    sourceTemplate.getName());
        }
        navigationContext.invalidateCurrentDocument();
        EventManager.raiseEventsOnDocumentChange(currentDocument);
        templateIdToAssociate = null;
    }

    public boolean canRenderAndStore() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        // check that templating is supported
        TemplateBasedDocument template = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (template == null) {
            return false;
        }
        // check that we can store the result
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh == null) {
            return false;
        }
        return true;
    }

    public String getEditableTemplateName() {
        return editableTemplateName;
    }

    public void setEditableTemplateName(String editableTemplateName) {
        if (editableTemplateName == null || !editableTemplateName.equals(this.editableTemplateName)) {
            this.editableTemplateName = editableTemplateName;
            templateEditableInputs = null;
        }
    }

    public List<TemplateSourceDocument> getBindableTemplatesForDocument() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String targetType = currentDocument.getType();
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        List<DocumentModel> templates = tps.getAvailableTemplateDocs(documentManager, targetType);

        List<TemplateSourceDocument> result = new ArrayList<>();
        TemplateBasedDocument currentTBD = currentDocument.getAdapter(TemplateBasedDocument.class);
        List<String> alreadyBoundTemplateNames = new ArrayList<>();
        if (currentTBD != null) {
            alreadyBoundTemplateNames = currentTBD.getTemplateNames();
        }
        for (DocumentModel doc : templates) {
            TemplateSourceDocument source = doc.getAdapter(TemplateSourceDocument.class);
            if (!alreadyBoundTemplateNames.contains(source.getName())) {
                result.add(source);
            }
        }
        return result;

    }

    public List<SelectItem> getBindableTemplatesForDocumentAsSelectItems() {

        List<SelectItem> items = new ArrayList<>();
        List<TemplateSourceDocument> sources = getBindableTemplatesForDocument();
        for (TemplateSourceDocument sd : sources) {
            DocumentModel doc = sd.getAdaptedDoc();
            String label = doc.getTitle();
            if (doc.isVersion()) {
                label = label + " (V " + doc.getVersionLabel() + ")";
            }
            items.add(new SelectItem(doc.getId(), label));
        }

        return items;
    }

    public boolean canBindNewTemplate() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!currentDocument.getCoreSession().hasPermission(currentDocument.getRef(), SecurityConstants.WRITE)) {
            return false;
        }
        if (getBindableTemplatesForDocument().size() == 0) {
            return false;
        }
        return true;
    }

    @Factory(value = "currentTemplateBasedDocument", scope = ScopeType.EVENT)
    public TemplateBasedDocument getCurrentDocumentAsTemplateBasedDocument() {
        return navigationContext.getCurrentDocument().getAdapter(TemplateBasedDocument.class);
    }

    @Factory(value = "associatedRenderableTemplates", scope = ScopeType.EVENT)
    public List<TemplateSourceDocument> getRenderableTemplates() {
        List<TemplateSourceDocument> result = new ArrayList<>();
        TemplateBasedDocument template = getCurrentDocumentAsTemplateBasedDocument();
        if (template != null) {
            List<TemplateSourceDocument> sources = template.getSourceTemplates();
            for (TemplateSourceDocument source : sources) {
                if (source.getTargetRenditionName() == null || source.getTargetRenditionName().isEmpty()) {
                    result.add(source);
                }
            }
        }
        return result;
    }
}

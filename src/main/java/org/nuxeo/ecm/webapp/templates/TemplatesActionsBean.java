/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.webapp.templates;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.template.rendition.TemplateBasedRenditionProvider;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorDescriptor;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorService;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam bean used as UI Controler
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * 
 */
@Name("templateActions")
@Scope(CONVERSATION)
public class TemplatesActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TemplatesActionsBean.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient DocumentActions documentActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient TypeManager typeManager;

    @In(create = true)
    protected transient WebActions webActions;

    protected List<TemplateInput> templateInputs;

    protected List<TemplateInput> templateEditableInputs;

    protected TemplateInput newInput;

    protected String templateIdToAssociate;

    public String createTemplate() throws Exception {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        TemplateSourceDocument sourceTemplate = changeableDocument.getAdapter(TemplateSourceDocument.class);
        if (sourceTemplate != null && sourceTemplate.getTemplateBlob() != null) {
            try {
                sourceTemplate.initTemplate(false);
                if (sourceTemplate.hasEditableParams()) {
                    templateInputs = sourceTemplate.getParams();
                    return "editTemplateRelatedData";
                }
            } catch (Exception e) {
                log.error("Error during parameter automatic initialization", e);
            }
        }
        return documentActions.saveDocument(changeableDocument);
    }

    public String createFromTemplate() throws Exception {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        TemplateBasedDocument templateBasedDocument = changeableDocument.getAdapter(TemplateBasedDocument.class);

        if (templateBasedDocument != null) {
            DocumentModel templateDoc = templateBasedDocument.getSourceTemplateDoc();
            if (templateDoc != null) {
                changeableDocument = templateBasedDocument.initializeFromTemplate(false);
                navigationContext.setChangeableDocument(changeableDocument);
            }
            if (templateBasedDocument.getSourceTemplate().allowInstanceOverride()
                    && templateBasedDocument.hasEditableParams()) {
                templateInputs = templateBasedDocument.getParams();
                return "editTemplateRelatedData";
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

    public String saveDocument() throws Exception {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();

        for (TemplateInput ti : templateInputs) {
            log.info(ti.toString());
        }

        TemplateBasedDocument templateBasedDocument = changeableDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBasedDocument != null) {
            templateBasedDocument.saveParams(templateInputs, false);
            templateBasedDocument.renderAndStoreAsAttachment(false);
        }
        TemplateSourceDocument source = changeableDocument.getAdapter(TemplateSourceDocument.class);
        if (source != null) {
            source.saveParams(templateInputs, false);
        }

        return documentActions.saveDocument(changeableDocument);
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.NEW_DOCUMENT_CREATED, EventNames.DOCUMENT_CHANGED }, create = false)
    @BypassInterceptors
    public void reset() {
        templateInputs = null;
        templateEditableInputs = null;
    }

    public List<DocumentModel> getAvailableTemplates(String targetType)
            throws ClientException {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        return tps.getAvailableTemplateDocs(documentManager, targetType);
    }

    public List<TemplateSourceDocument> getAvailableOfficeTemplates(
            String targetType) throws ClientException {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        return tps.getAvailableOfficeTemplates(documentManager, targetType);
    }

    public List<TemplateInput> getTemplateEditableInputs() throws Exception {
        if (templateEditableInputs == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
            if (template != null) {
                templateEditableInputs = template.getParams();
            } else {
                TemplateBasedDocument templateBasedDoc = currentDocument.getAdapter(TemplateBasedDocument.class);
                templateEditableInputs = templateBasedDoc.getParams();
            }
        }
        return templateEditableInputs;
    }

    public void setTemplateEditableInputs(
            List<TemplateInput> templateEditableInputs) {
        this.templateEditableInputs = templateEditableInputs;
    }

    public String saveTemplateInputs() throws Exception {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {
            currentDocument = template.saveParams(templateEditableInputs, true);
        } else {
            TemplateBasedDocument templateBasedDoc = currentDocument.getAdapter(TemplateBasedDocument.class);
            currentDocument = templateBasedDoc.saveParams(
                    templateEditableInputs, true);
        }

        return navigationContext.navigateToDocument(currentDocument);
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

    public boolean canAddTemplateInputs() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        return template != null ? true : false;
    }

    public boolean canUpdateTemplateInputs() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {
            return true;
        }
        TemplateBasedDocument templateBased = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBased != null) {
            return templateBased.hasEditableParams()
                    && documentManager.hasPermission(currentDocument.getRef(),
                            "Write");
        }
        return false;
    }

    public boolean canResetParameters() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument templateBased = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBased != null) {
            return true;
        }
        return false;
    }

    public void resetParameters() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument templateBased = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBased != null) {
            templateBased.initializeFromTemplate(true);
            templateEditableInputs = null;
        }
    }

    public String detachTemplate() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        currentDocument = tps.detachTemplateBasedDocument(currentDocument, true);
        webActions.resetTabList();
        return navigationContext.navigateToDocument(currentDocument);
    }

    public boolean canRenderTemplate() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        // check that templating is supported
        TemplateBasedDocument template = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (template == null) {
            return false;
        }
        // check that we can store the result : XXX do better
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh == null) {
            return false;
        }
        return true;
    }

    public boolean canReverseTemplate() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        // check that templating is supported
        TemplateBasedDocument template = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (template == null) {
            return false;
        }
        return template.isBidirectional();
    }

    public String addTemplateInput() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {
            template.addInput(newInput);
            newInput = null;
            templateEditableInputs = null;
        } else {
            return null;
        }

        return navigationContext.navigateToDocument(currentDocument);
    }

    public String updateBlob() throws Exception {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (doc == null) {
            return null;
        }
        doc.renderAndStoreAsAttachment(true);
        documentManager.save();
        return navigationContext.navigateToDocument(doc.getAdaptedDoc());
    }

    public String updateDocumentFromBlob() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (doc == null) {
            return null;
        }

        if (doc.isBidirectional()) {
            currentDocument = doc.updateDocumentModelFromBlob(true);
            documentManager.save();
        }
        return navigationContext.navigateToDocument(currentDocument);
    }

    public Collection<Type> getAllTypes() {
        return typeManager.getTypes();
    }

    public Collection<Type> getForcableTypes() {

        Collection<Type> types = typeManager.getTypes();

        Iterator<Type> it = types.iterator();
        while (it.hasNext()) {
            Type type = it.next();
            if (type.getId().equals("TemplateBasedFile")) {
                it.remove();
                break;
            }
        }
        return types;
    }

    public DocumentModel resolveTemplateById(String uuid) {
        try {
            return documentManager.getDocument(new IdRef(uuid));
        } catch (Exception e) {
            return null;
        }
    }

    public TemplateSourceDocument getCurrentDocumentAsTemplateSourceDocument() {
        return navigationContext.getCurrentDocument().getAdapter(
                TemplateSourceDocument.class);
    }

    public TemplateBasedDocument getCurrentDocumentATemplateBasedDocument() {
        return navigationContext.getCurrentDocument().getAdapter(
                TemplateBasedDocument.class);
    }

    public Collection<TemplateProcessorDescriptor> getRegistredTemplateProcessors() {
        return Framework.getLocalService(TemplateProcessorService.class).getRegistredTemplateProcessors();
    }

    public String render() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (doc == null) {
            return null;
        }

        // XXX handle rendering error
        Blob rendition = doc.renderWithTemplate();
        String filename = rendition.getFilename();
        FacesContext context = FacesContext.getCurrentInstance();
        return ComponentUtils.download(context, rendition, filename);
    }

    public String associateDocumentToTemplate() throws ClientException {
        if (templateIdToAssociate == null) {
            return null;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentModel sourceTemplate = documentManager.getDocument(new IdRef(
                templateIdToAssociate));
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        currentDocument = tps.makeTemplateBasedDocument(currentDocument,
                sourceTemplate, true);
        navigationContext.invalidateCurrentDocument();
        EventManager.raiseEventsOnDocumentChange(currentDocument);
        templateIdToAssociate = null;
        return navigationContext.navigateToDocument(currentDocument,
                "after-edit");
    }

    public String getTemplateIdToAssociate() {
        return templateIdToAssociate;
    }

    public void setTemplateIdToAssociate(String templateIdToAssociate) {
        this.templateIdToAssociate = templateIdToAssociate;
    }

    public List<RenditionDefinition> getRenditions() {
        RenditionService rs = Framework.getLocalService(RenditionService.class);
        return rs.getDeclaredRenditionDefinitionsForProviderType(TemplateBasedRenditionProvider.class.getSimpleName());
    }
}

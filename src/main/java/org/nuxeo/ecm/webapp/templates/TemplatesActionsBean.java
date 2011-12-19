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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;

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

    protected List<TemplateInput> templateInputs;

    protected List<TemplateInput> templateEditableInputs;

    protected TemplateInput newInput;

    public String createTemplate() throws Exception {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        TemplateSourceDocument sourceTemplate =changeableDocument.getAdapter(TemplateSourceDocument.class);
        if (sourceTemplate!=null && sourceTemplate.getTemplateBlob()!=null){
            try {
                sourceTemplate.initParamsFromFile(false);
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

        if (templateBasedDocument!=null){
            DocumentModel templateDoc = templateBasedDocument.getSourceTemplateDoc();
            if (templateDoc!=null) {
                changeableDocument = templateBasedDocument.initializeFromTemplate(false);
                navigationContext.setChangeableDocument(changeableDocument);
            }
            if (templateBasedDocument.getSourceTemplate().allowInstanceOverride() && templateBasedDocument.hasEditableParams()) {
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
        DocumentModel changeableDocument = navigationContext
                .getChangeableDocument();

        for (TemplateInput ti : templateInputs) {
            log.info(ti.toString());
        }

        TemplateBasedDocument templateBasedDocument = changeableDocument.getAdapter(TemplateBasedDocument.class);
        if (templateBasedDocument!=null) {
            templateBasedDocument.saveParams(templateInputs, false);
            templateBasedDocument.updateBlobFromParams(false);
        }
        TemplateSourceDocument source = changeableDocument.getAdapter(TemplateSourceDocument.class);
        if (source!=null) {
            source.saveParams(templateInputs, false);
        }

        return documentActions.saveDocument(changeableDocument);
    }


    @Observer( value= { EventNames.LOCATION_SELECTION_CHANGED,
            EventNames.NEW_DOCUMENT_CREATED, EventNames.DOCUMENT_CHANGED }, create=false)
    @BypassInterceptors
    public void reset() {
        templateInputs=null;
        templateEditableInputs=null;
    }

    public List<DocumentModel> getAvailableTemplates() throws ClientException {
        StringBuffer sb = new StringBuffer("select * from TemplateSource");
        return documentManager.query(sb.toString());
    }

    public List<TemplateInput> getTemplateEditableInputs() throws Exception {
        if (templateEditableInputs==null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
            if (template!=null) {
                templateEditableInputs = template.getParams();
            } else {
                TemplateBasedDocument templateBasedDoc = currentDocument.getAdapter(TemplateBasedDocument.class);
                templateEditableInputs = templateBasedDoc.getParams();
            }
        }
        return templateEditableInputs;
    }

    public void setTemplateEditableInputs(List<TemplateInput> templateEditableInputs) {
        this.templateEditableInputs = templateEditableInputs;
    }

    public String saveTemplateInputs() throws Exception {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template!=null) {
            currentDocument = template.saveParams(templateEditableInputs, true);
        } else {
            TemplateBasedDocument templateBasedDoc = currentDocument.getAdapter(TemplateBasedDocument.class);
            currentDocument = templateBasedDoc.saveParams(templateEditableInputs, true);
        }

        return navigationContext.navigateToDocument(currentDocument);
    }

    public TemplateInput getNewInput() {
        if (newInput==null) {
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
        return template!=null?true:false;
    }

    public boolean canRenderTemplate() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument template = currentDocument.getAdapter(TemplateBasedDocument.class);
        return template!=null?true:false;
    }

    public String addTemplateInput() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template!=null) {
            template.addInput(newInput);
            newInput=null;
            templateEditableInputs=null;
        } else {
            return null;
        }

        return navigationContext.navigateToDocument(currentDocument);
    }

    public String updateBlob() throws Exception {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (doc==null) {
            return null;
        }
        doc.updateBlobFromParams(true);
        documentManager.save();
        return navigationContext.navigateToDocument(doc.getAdaptedDoc());
    }


    public String updateDocumentFromBlob() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        TemplateBasedDocument doc = currentDocument.getAdapter(TemplateBasedDocument.class);
        if (doc==null) {
            return null;
        }

        if (doc.isBidirectional()) {
            currentDocument = doc.updateDocumentModelFromBlob(true);
            documentManager.save();
        }

        return navigationContext.navigateToDocument(currentDocument);
    }



}

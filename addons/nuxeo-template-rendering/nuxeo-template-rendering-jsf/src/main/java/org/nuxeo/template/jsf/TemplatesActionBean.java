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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;

@Name("templateActions")
@Scope(CONVERSATION)
public class TemplatesActionBean extends BaseTemplateAction {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient DocumentActions documentActions;

    @In(create = true)
    protected transient TypeManager typeManager;

    protected List<TemplateInput> templateInputs;

    protected boolean showParamEditor = false;

    protected boolean showUsageListing = false;

    protected boolean showVersions = false;

    protected boolean checkedInVersion = false;

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
        showParamEditor = false;
    }

    public List<TemplateInput> getTemplateEditableInputs() {
        if (templateEditableInputs == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
            if (template != null) {
                templateEditableInputs = template.getParams();
            }
        }
        return templateEditableInputs;
    }

    public void setTemplateEditableInputs(List<TemplateInput> templateEditableInputs) {
        this.templateEditableInputs = templateEditableInputs;
    }

    public String saveTemplateInputs() {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {
            currentDocument = template.saveParams(templateEditableInputs, true);
        }
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

    @Override
    public String addTemplateInput() {
        showParamEditor = true;
        return super.addTemplateInput();
    }

    public String removeTemplateInput(String name) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        showParamEditor = true;

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {

            Iterator<TemplateInput> it = templateEditableInputs.listIterator();
            while (it.hasNext()) {
                TemplateInput input = it.next();
                if (input.getName().equals(name)) {
                    it.remove();
                    break;
                }
            }

            currentDocument = template.saveParams(templateEditableInputs, true);
            newInput = null;
            templateEditableInputs = null;
            navigationContext.invalidateCurrentDocument();
            return navigationContext.navigateToDocument(currentDocument);
        } else {
            return null;
        }
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

    public Collection<TemplateProcessorDescriptor> getRegistredTemplateProcessors() {
        return Framework.getService(TemplateProcessorService.class).getRegisteredTemplateProcessors();
    }

    public Collection<OutputFormatDescriptor> getOutputFormatDescriptors() {
        return Framework.getService(TemplateProcessorService.class).getOutputFormats();
    }

    public List<String> getTemplateAndVersionsUUIDs() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        TemplateSourceDocument template = currentDocument.getAdapter(TemplateSourceDocument.class);
        if (template != null) {
            List<String> uuids = new ArrayList<>();
            uuids.add("\"" + currentDocument.getId() + "\"");

            if (showVersions) {
                for (DocumentModel version : documentManager.getVersions(currentDocument.getRef())) {
                    uuids.add("\"" + version.getId() + "\"");
                }
            }
            return uuids;
        }

        return new ArrayList<>();
    }

    public boolean isShowParamEditor() {
        return showParamEditor;
    }

    public boolean isShowUsageListing() {
        return showUsageListing;
    }

    public void setShowUsageListing(boolean showUsageListing) {
        this.showUsageListing = showUsageListing;
    }

    public boolean isShowVersions() {
        return showVersions;
    }

    public void setShowVersions(boolean showVersions) {
        this.showVersions = showVersions;
    }

    public boolean isCheckedInVersion() {
        return checkedInVersion;
    }

    public void setCheckedInVersion(boolean checkedInVersion) {
        this.checkedInVersion = checkedInVersion;
    }

}

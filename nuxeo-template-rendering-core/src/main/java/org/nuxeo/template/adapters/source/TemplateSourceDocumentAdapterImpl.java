/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.template.adapters.source;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.XMLSerializer;
import org.nuxeo.template.adapters.AbstractTemplateDocument;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * Default implementation of {@link TemplateSourceDocument}. It mainly expect from the underlying DocumentModel to have
 * the "Template" facet.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class TemplateSourceDocumentAdapterImpl extends AbstractTemplateDocument implements Serializable,
        TemplateSourceDocument {

    public static final String TEMPLATE_DATA_PROP = "tmpl:templateData";

    public static final String TEMPLATE_NAME_PROP = "tmpl:templateName";

    public static final String TEMPLATE_TYPE_PROP = "tmpl:templateType";

    public static final String TEMPLATE_TYPE_AUTO = "auto";

    public static final String TEMPLATE_APPLICABLE_TYPES_PROP = "tmpl:applicableTypes";

    public static final String TEMPLATE_APPLICABLE_TYPES_ALL = "all";

    public static final String TEMPLATE_FORCED_TYPES_PROP = "tmpl:forcedTypes";

    public static final String TEMPLATE_FORCED_TYPES_ITEM_PROP = "tmpl:forcedTypes/*";

    public static final String TEMPLATE_FORCED_TYPES_NONE = "none";

    public static final String TEMPLATE_RENDITION_NONE = "none";

    public static final String TEMPLATE_OUTPUT_PROP = "tmpl:outputFormat";

    public static final String TEMPLATE_OVERRIDE_PROP = "tmpl:allowOverride";

    public static final String TEMPLATE_USEASMAIN_PROP = "tmpl:useAsMainContent";

    public static final String TEMPLATE_RENDITION_PROP = "tmpl:targetRenditionName";

    public static final String TEMPLATE_FACET = "Template";

    private static final long serialVersionUID = 1L;

    public TemplateSourceDocumentAdapterImpl(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

    protected String getTemplateParamsXPath() {
        return TEMPLATE_DATA_PROP;
    }

    @Override
    public List<TemplateInput> getParams() {
        String dataPath = getTemplateParamsXPath();

        if (adaptedDoc.getPropertyValue(dataPath) == null) {
            return new ArrayList<>();
        }
        String xml = adaptedDoc.getPropertyValue(dataPath).toString();

        try {
            return XMLSerializer.readFromXml(xml);
        } catch (DocumentException e) {
            log.error("Unable to parse parameters", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasEditableParams() {
        for (TemplateInput param : getParams()) {
            if (!param.isReadOnly()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DocumentModel saveParams(List<TemplateInput> params, boolean save) {
        String dataPath = getTemplateParamsXPath();
        String xml = XMLSerializer.serialize(params);
        adaptedDoc.setPropertyValue(dataPath, xml);
        adaptedDoc.putContextData(TemplateSourceDocument.INIT_DONE_FLAG, true);
        if (save) {
            doSave();
        }
        return adaptedDoc;
    }

    protected TemplateProcessor getTemplateProcessor() {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        return tps.getProcessor(getTemplateType());
    }

    @Override
    public String getParamsAsString() throws PropertyException {
        String dataPath = getTemplateParamsXPath();

        if (adaptedDoc.getPropertyValue(dataPath) == null) {
            return null;
        }
        return adaptedDoc.getPropertyValue(dataPath).toString();
    }

    @Override
    public List<TemplateInput> addInput(TemplateInput input) {

        List<TemplateInput> params = getParams();
        if (input == null) {
            return params;
        }

        boolean newParam = true;
        if (params == null) {
            params = new ArrayList<>();
        }
        for (TemplateInput param : params) {
            if (param.getName().equals(input.getName())) {
                newParam = false;
                param.update(input);
                break;
            }
        }
        if (newParam) {
            params.add(input);
        }
        saveParams(params, false);

        return params;
    }

    @Override
    public boolean hasInput(String inputName) {
        List<TemplateInput> params = getParams();
        return params != null && params.stream().map(TemplateInput::getName).anyMatch(inputName::equals);
    }

    @Override
    public String getTemplateType() {
        String ttype = (String) getAdaptedDoc().getPropertyValue(TEMPLATE_TYPE_PROP);
        if (TEMPLATE_TYPE_AUTO.equals(ttype)) {
            return null;
        }
        return ttype;
    }

    @Override
    public void initTemplate(boolean save) {
        // avoid duplicate init
        if (getAdaptedDoc().getContextData(TemplateSourceDocument.INIT_DONE_FLAG) == null) {
            Blob blob = getTemplateBlob();
            if (blob != null) {
                if (getTemplateType() == null) {
                    TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
                    String templateType = tps.findProcessorName(blob);
                    if (templateType != null) {
                        getAdaptedDoc().setPropertyValue(TEMPLATE_TYPE_PROP, templateType);
                    }
                }

                String tmplName = (String) getAdaptedDoc().getPropertyValue(TEMPLATE_NAME_PROP);
                if (tmplName == null || tmplName.isEmpty()) {
                    tmplName = computeTemplateName();
                    getAdaptedDoc().setPropertyValue(TEMPLATE_NAME_PROP, tmplName);
                }

                TemplateProcessor processor = getTemplateProcessor();
                if (processor != null) {
                    List<TemplateInput> params;
                    try {
                        params = processor.getInitialParametersDefinition(blob);
                    } catch (IOException e) {
                        throw new NuxeoException(e);
                    }
                    saveParams(params, save);
                }
                getAdaptedDoc().putContextData(TemplateSourceDocument.INIT_DONE_FLAG, true);
            }
        }
    }

    protected String computeTemplateName() {
        return getAdaptedDoc().getTitle();
    }

    @Override
    public boolean allowInstanceOverride() {
        Boolean allowOverride = (Boolean) getAdaptedDoc().getPropertyValue(TEMPLATE_OVERRIDE_PROP);
        if (allowOverride == null) {
            allowOverride = true;
        }
        return allowOverride;
    }

    @Override
    public void initTypesBindings() {

        // manage applicable types
        String[] applicableTypesArray = (String[]) getAdaptedDoc().getPropertyValue(TEMPLATE_APPLICABLE_TYPES_PROP);

        String[] newApplicableTypesArray = null;

        if (applicableTypesArray == null || applicableTypesArray.length == 0) {
            newApplicableTypesArray = new String[] { TEMPLATE_APPLICABLE_TYPES_ALL };
        } else if (applicableTypesArray.length > 1) {
            if (TEMPLATE_APPLICABLE_TYPES_ALL.equals(applicableTypesArray[0])) {
                List<String> at = Arrays.asList(applicableTypesArray);
                at.remove(0);
                newApplicableTypesArray = at.toArray(new String[at.size()]);
            }
        }
        if (newApplicableTypesArray != null) {
            getAdaptedDoc().setPropertyValue(TEMPLATE_APPLICABLE_TYPES_PROP, newApplicableTypesArray);
        }

        // manage forcedTypes
        String[] forcedTypesArray = (String[]) getAdaptedDoc().getPropertyValue(TEMPLATE_FORCED_TYPES_PROP);
        String[] newForcedTypesArray = null;
        if (forcedTypesArray == null || forcedTypesArray.length == 0) {
            newForcedTypesArray = new String[] { TEMPLATE_FORCED_TYPES_NONE };
        } else if (forcedTypesArray.length > 1) {
            if (TEMPLATE_FORCED_TYPES_NONE.equals(forcedTypesArray[0])) {
                List<String> ft = Arrays.asList(forcedTypesArray);
                ft.remove(0);
                newForcedTypesArray = ft.toArray(new String[ft.size()]);
            }
        }
        if (newForcedTypesArray != null) {
            getAdaptedDoc().setPropertyValue(TEMPLATE_FORCED_TYPES_PROP, newForcedTypesArray);
        }

    }

    @Override
    public List<String> getApplicableTypes() {
        String[] applicableTypesArray = (String[]) getAdaptedDoc().getPropertyValue(TEMPLATE_APPLICABLE_TYPES_PROP);
        List<String> applicableTypes = new ArrayList<>();
        if (applicableTypesArray != null) {
            applicableTypes.addAll((Arrays.asList(applicableTypesArray)));
        }
        if (applicableTypes.size() > 0 && applicableTypes.get(0).equals(TEMPLATE_APPLICABLE_TYPES_ALL)) {
            applicableTypes.remove(0);
        }
        return applicableTypes;
    }

    @Override
    public List<String> getForcedTypes() {
        String[] forcedTypesArray = (String[]) getAdaptedDoc().getPropertyValue(TEMPLATE_FORCED_TYPES_PROP);
        List<String> applicableTypes = new ArrayList<>();
        if (forcedTypesArray != null) {
            applicableTypes.addAll((Arrays.asList(forcedTypesArray)));
        }
        if (applicableTypes.size() > 0 && applicableTypes.get(0).equals(TEMPLATE_FORCED_TYPES_NONE)) {
            applicableTypes.remove(0);
        }
        return applicableTypes;
    }

    @Override
    public void removeForcedType(String type, boolean save) {
        List<String> types = getForcedTypes();
        if (types.contains(type)) {
            types.remove(type);
            String[] typesArray = types.toArray(new String[types.size()]);
            getAdaptedDoc().setPropertyValue(TemplateSourceDocumentAdapterImpl.TEMPLATE_FORCED_TYPES_PROP, typesArray);
            if (save) {
                adaptedDoc = getAdaptedDoc().getCoreSession().saveDocument(getAdaptedDoc());
            }
        }
    }

    @Override
    public void setForcedTypes(String[] forcedTypes, boolean save) {
        getAdaptedDoc().setPropertyValue(TemplateSourceDocumentAdapterImpl.TEMPLATE_FORCED_TYPES_PROP, forcedTypes);
        if (save) {
            adaptedDoc = getAdaptedDoc().getCoreSession().saveDocument(getAdaptedDoc());
        }
    }

    @Override
    public List<TemplateBasedDocument> getTemplateBasedDocuments() {
        return Framework.getLocalService(TemplateProcessorService.class).getLinkedTemplateBasedDocuments(adaptedDoc);
    }

    @Override
    public String getOutputFormat() {
        return (String) getAdaptedDoc().getPropertyValue(TEMPLATE_OUTPUT_PROP);
    }

    @Override
    public void setOutputFormat(String mimetype, boolean save) {
        getAdaptedDoc().setPropertyValue(TEMPLATE_OUTPUT_PROP, mimetype);
        if (save) {
            doSave();
        }
    }

    @Override
    public boolean useAsMainContent() {
        Boolean useAsMain = (Boolean) getAdaptedDoc().getPropertyValue(TEMPLATE_USEASMAIN_PROP);
        if (useAsMain == null) {
            useAsMain = false;
        }
        return useAsMain;
    }

    @Override
    public Blob getTemplateBlob() {
        BlobHolder bh = getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh != null) {
            return bh.getBlob();
        }
        return null;
    }

    @Override
    public void setTemplateBlob(Blob blob, boolean save) {
        BlobHolder bh = getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh != null) {
            bh.setBlob(blob);
            initTemplate(false);
            if (save) {
                doSave();
            }
        }
    }

    @Override
    public String getName() {
        String name = (String) getAdaptedDoc().getPropertyValue(TEMPLATE_NAME_PROP);
        if (name == null) {
            name = getAdaptedDoc().getTitle();
        }
        return name;
    }

    @Override
    public String getFileName() {
        Blob blob = getTemplateBlob();
        if (blob != null) {
            return blob.getFilename();
        }
        return null;
    }

    @Override
    public String getTitle() {
        return getAdaptedDoc().getTitle();
    }

    @Override
    public String getVersionLabel() {
        return getAdaptedDoc().getVersionLabel();
    }

    @Override
    public String getId() {
        return getAdaptedDoc().getId();
    }

    @Override
    public String getLabel() {
        StringBuilder sb = new StringBuilder(getTitle());
        if (!getTitle().equals(getFileName())) {
            sb.append(" (").append(getFileName()).append(")");
        }
        if (getVersionLabel() != null) {
            sb.append(" [").append(getVersionLabel()).append("]");
        }
        return sb.toString();
    }

    @Override
    public String getTargetRenditionName() {
        String targetRendition = (String) getAdaptedDoc().getPropertyValue(TEMPLATE_RENDITION_PROP);
        if (TEMPLATE_RENDITION_NONE.equals(targetRendition)) {
            return null;
        }
        return targetRendition;
    }

    @Override
    public void setTargetRenditioName(String renditionName, boolean save) {
        getAdaptedDoc().setPropertyValue(TEMPLATE_RENDITION_PROP, renditionName);
        if (save) {
            doSave();
        }
    }

}

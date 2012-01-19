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
package org.nuxeo.ecm.platform.template.adapters.source;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.AbstractTemplateDocument;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorService;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of {@link TemplateSourceDocument}. It mainly expect
 * from the underlying DocumentModel to have the "Template" facet.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class TemplateSourceDocumentAdapterImpl extends AbstractTemplateDocument
        implements Serializable, TemplateSourceDocument {

    public static final String TEMPLATE_DATA_PROP = "tmpl:templateData";

    public static final String TEMPLATE_TYPE_PROP = "tmpl:templateType";

    public static final String TEMPLATE_TYPE_AUTO = "auto";

    public static final String TEMPLATE_APPLICABLE_TYPES_PROP = "tmpl:applicableTypes";

    public static final String TEMPLATE_FORCED_TYPES_PROP = "tmpl:forcedTypes";

    public static final String TEMPLATE_OVERRIDE_PROP = "tmpl:allowOverride";

    public static final String TEMPLATE_FACET = "Template";

    private static final long serialVersionUID = 1L;

    public TemplateSourceDocumentAdapterImpl(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

    protected String getTemplateParamsXPath() {
        return TEMPLATE_DATA_PROP;
    }

    public String getParamsAsString() throws PropertyException, ClientException {
        String dataPath = getTemplateParamsXPath();

        if (adaptedDoc.getPropertyValue(dataPath) == null) {
            return null;
        }
        return adaptedDoc.getPropertyValue(dataPath).toString();
    }

    public List<TemplateInput> addInput(TemplateInput input) throws Exception {

        List<TemplateInput> params = getParams();

        if (params == null) {
            params = new ArrayList<TemplateInput>();
        }

        params.add(input);
        saveParams(params, false);

        return params;
    }

    public String getTemplateType() {
        try {
            String ttype = (String) getAdaptedDoc().getPropertyValue(TEMPLATE_TYPE_PROP);
            if (TEMPLATE_TYPE_AUTO.equals(ttype)) {
                return null;
            }
            return ttype;
        } catch (Exception e) {
            log.error("Unable to read template type ", e);
            return null;
        }
    }

    public void initTemplate(boolean save) throws Exception {
        // avoid duplicate init
        if (getAdaptedDoc().getContextData(TemplateSourceDocument.INIT_DONE_FLAG)==null && getTemplateType()==null) {
            Blob blob = getTemplateBlob();
            if (blob!=null) {
                TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
                String templateType = tps.findProcessorName(blob);
                if (templateType != null) {
                    getAdaptedDoc().setPropertyValue(TEMPLATE_TYPE_PROP, templateType);
                }
                TemplateProcessor processor = getTemplateProcessor();
                if (processor != null) {
                    List<TemplateInput> params = processor.getInitialParametersDefinition(blob);
                    saveParams(params, save);
                }
                getAdaptedDoc().getContextData().put(TemplateSourceDocument.INIT_DONE_FLAG, true);
            }
        }
    }

    public boolean allowInstanceOverride() {
        try {
            Boolean allowOverride = (Boolean) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_OVERRIDE_PROP);
            if (allowOverride == null) {
                allowOverride = true;
            }
            return allowOverride;
        } catch (Exception e) {
            log.error("Unable to read template allow override ", e);
            return false;
        }
    }

    public List<String> getApplicableTypes() {
        try {
            String[] applicableTypesArray = (String[]) getAdaptedDoc().getPropertyValue(
                    TemplateSourceDocumentAdapterImpl.TEMPLATE_APPLICABLE_TYPES_PROP);
            List<String> applicableTypes = new ArrayList<String>();
            if (applicableTypesArray != null) {
                applicableTypes.addAll((Arrays.asList(applicableTypesArray)));
            }
            if (applicableTypes.size()>0 && applicableTypes.get(0).equals("all")) {
                applicableTypes.remove(0);
            }
            return applicableTypes;
        } catch (Exception e) {
            log.error("Error while reading applicable types");
            return new ArrayList<String>();
        }
    }

    public List<String> getForcedTypes() {
        try {
            String[] applicableTypesArray = (String[]) getAdaptedDoc().getPropertyValue(
                    TemplateSourceDocumentAdapterImpl.TEMPLATE_FORCED_TYPES_PROP);
            List<String> applicableTypes = new ArrayList<String>();
            if (applicableTypesArray != null) {
                applicableTypes.addAll((Arrays.asList(applicableTypesArray)));
            }
            if (applicableTypes.size()>0 && applicableTypes.get(0).equals("none")) {
                applicableTypes.remove(0);
            }
            return applicableTypes;
        } catch (Exception e) {
            log.error("Error while reading applicable types");
            return new ArrayList<String>();
        }
    }

    public List<TemplateBasedDocument> getTemplateBasedDocuments() throws ClientException {
        return Framework.getLocalService(TemplateProcessorService.class).getLinkedTemplateBasedDocuments(adaptedDoc);
    }
}

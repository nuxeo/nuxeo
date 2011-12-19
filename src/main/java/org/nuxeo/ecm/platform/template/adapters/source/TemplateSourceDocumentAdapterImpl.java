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
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.AbstractTemplateDocument;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;

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
            return (String) getAdaptedDoc().getPropertyValue(TEMPLATE_TYPE_PROP);
        } catch (Exception e) {
            log.error("Unable to read template type ", e);
            return null;
        }
    }

    public void initParamsFromFile(boolean save) throws Exception {
        Blob blob = getTemplateBlob();
        String templateType = getTemplateType(blob);
        if (templateType != null) {
            getAdaptedDoc().setPropertyValue(TEMPLATE_TYPE_PROP, templateType);
        }
        TemplateProcessor processor = getTemplateProcessor();
        if (processor != null) {
            List<TemplateInput> params = processor.getInitialParametersDefinition(blob);
            saveParams(params, save);

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
}

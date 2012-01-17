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
package org.nuxeo.ecm.platform.template.adapters.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.AbstractTemplateDocument;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.template.processors.BidirectionalTemplateProcessor;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;

/**
 * Default implementation of {@link TemplateBasedDocument} adapter. This adapter
 * mainly expect from the underlying {@link DocumentModel} to have the
 * "TemplateBased" facet
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class TemplateBasedDocumentAdapterImpl extends AbstractTemplateDocument
        implements Serializable, TemplateBasedDocument {

    public static final String TEMPLATE_DATA_PROP = "nxts:templateData";

    public static final String TEMPLATE_ID_PROP = "nxts:templateId";

    private static final long serialVersionUID = 1L;

    public static final String TEMPLATEBASED_FACET = "TemplateBased";

    public TemplateBasedDocumentAdapterImpl(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

    protected String getTemplateParamsXPath() {
        return TEMPLATE_DATA_PROP;
    }

    public DocumentModel setTemplate(DocumentModel template, boolean save)
            throws PropertyException, ClientException {

        String tid = template.getId();
        adaptedDoc.setPropertyValue(TEMPLATE_ID_PROP, tid);

        if (save) {
            adaptedDoc = getSession().saveDocument(adaptedDoc);
        }

        return adaptedDoc;
    }

    public TemplateSourceDocument getSourceTemplate() throws Exception {
        DocumentModel template = getSourceTemplateDoc();
        if (template != null) {
            return template.getAdapter(TemplateSourceDocument.class);
        }
        return null;
    }

    public DocumentModel getSourceTemplateDoc() throws Exception {
        Property tidProp = adaptedDoc.getProperty(TEMPLATE_ID_PROP);
        if (tidProp == null || tidProp.getValue() == null
                || "none".equals(tidProp.getValue().toString())) {
            return null;
        }
        IdRef tRef = new IdRef(tidProp.getValue().toString());
        return getSession().getDocument(tRef);
    }

    public String getTemplateType() {
        TemplateSourceDocument source = null;
        try {
            source = getSourceTemplate();
        } catch (Exception e) {
            log.error("Unable to find source template");
            return null;
        }
        if (source != null) {
            return source.getTemplateType();
        }
        return null;
    }

    public DocumentModel initializeFromTemplate(boolean save) throws Exception {

        TemplateSourceDocument tmpl = getSourceTemplate();
        if (tmpl == null) {
            throw new ClientException("No associated template");
        }

        // copy Params but set as readonly all params set in template
        List<TemplateInput> params = tmpl.getParams();
        List<TemplateInput> myParams = new ArrayList<TemplateInput>();
        for (TemplateInput param : params) {
            TemplateInput myParam = param.getCopy(param.isSet());
            myParams.add(myParam);
        }

        saveParams(myParams, false);

        if (save) {
            adaptedDoc = getSession().saveDocument(adaptedDoc);
        }
        return adaptedDoc;
    }

    protected void setBlob(Blob blob) throws ClientException {
        adaptedDoc.getAdapter(BlobHolder.class).setBlob(blob);
    }

    public Blob renderWithTemplate() throws Exception {
        TemplateProcessor processor = getTemplateProcessor();
        if (processor != null) {
            Blob blob = processor.renderTemplate(this);
            return blob;
        } else {
            throw new ClientException(
                    "No template processor found for template type="
                            + getTemplateType());
        }
    }

    public Blob renderAndStoreAsAttachment(boolean save) throws Exception {
        Blob blob = renderWithTemplate();
        setBlob(blob);
        if (save) {
            adaptedDoc = getSession().saveDocument(adaptedDoc);
        }
        return blob;
    }

    public boolean isBidirectional() {
        TemplateProcessor processor = getTemplateProcessor();
        if (processor != null) {
            return processor instanceof BidirectionalTemplateProcessor;
        }
        return false;
    }

    public DocumentModel updateDocumentModelFromBlob(boolean save)
            throws Exception {
        TemplateProcessor processor = getTemplateProcessor();
        if (isBidirectional()) {
            adaptedDoc = ((BidirectionalTemplateProcessor) processor).updateDocumentFromBlob(this);
            if (save) {
                adaptedDoc = getSession().saveDocument(adaptedDoc);
            }
        }
        return adaptedDoc;
    }

}

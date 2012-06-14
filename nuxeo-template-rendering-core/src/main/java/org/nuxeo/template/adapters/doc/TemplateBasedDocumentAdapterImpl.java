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
package org.nuxeo.template.adapters.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.XMLSerializer;
import org.nuxeo.template.adapters.AbstractTemplateDocument;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.processors.convert.ConvertHelper;

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

    private static final long serialVersionUID = 1L;

    public static final String TEMPLATEBASED_FACET = "TemplateBased";

    protected ConvertHelper convertHelper = new ConvertHelper();

    protected final TemplateBindings bindings;

    public TemplateBasedDocumentAdapterImpl(DocumentModel doc)
            throws ClientException {
        this.adaptedDoc = doc;
        bindings = new TemplateBindings(doc);
    }

    public DocumentModel setTemplate(DocumentModel template, boolean save)
            throws ClientException {

        TemplateSourceDocument source = template.getAdapter(TemplateSourceDocument.class);
        if (source == null) {
            throw new ClientException(
                    "Can not bind to an non template document");
        }
        String tid = source.getId();
        String templateName = source.getName();
        if (!bindings.containsTemplateId(tid)) {
            if (templateName == null) {
                templateName = TemplateBindings.DEFAULT_BINDING;
            }
            TemplateBinding tb = new TemplateBinding();
            tb.setTemplateId(tid);
            tb.setName(templateName);
            bindings.add(tb);
            try {
                initializeFromTemplate(templateName, false);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            bindings.save(adaptedDoc);
            if (save) {
                doSave();
            }

        }
        return adaptedDoc;
    }

    public DocumentModel removeTemplateBinding(String templateName, boolean save)
            throws ClientException {
        if (bindings.containsTemplateName(templateName)) {
            bindings.removeByName(templateName);
            bindings.save(adaptedDoc);
            if (save) {
                doSave();
            }
        }
        return adaptedDoc;
    }

    /*
     * public TemplateSourceDocument getSourceTemplate() throws Exception {
     * return getSourceTemplate(TemplateBindings.DEFAULT_BINDING); }
     */
    public TemplateSourceDocument getSourceTemplate(String templateName)
            throws Exception {
        DocumentModel template = getSourceTemplateDoc(templateName);
        if (template != null) {
            return template.getAdapter(TemplateSourceDocument.class);
        }
        return null;
    }

    /*
     * public DocumentModel getSourceTemplateDoc() throws Exception { return
     * getSourceTemplateDoc(TemplateBindings.DEFAULT_BINDING); }
     */

    @Override
    public DocumentRef getSourceTemplateDocRef(String templateName)
            throws Exception {
        TemplateBinding binding = null;
        if (templateName == null) {
            binding = bindings.get();
        } else {
            binding = bindings.get(templateName);
        }
        if (binding == null) {
            return null;
        }
        return new IdRef(binding.getTemplateId());
    }

    public DocumentModel getSourceTemplateDoc(String templateName)
            throws Exception {
        TemplateBinding binding = null;
        if (templateName == null) {
            binding = bindings.get();
        } else {
            binding = bindings.get(templateName);
        }
        if (binding == null) {
            return null;
        }
        DocumentRef tRef = getSourceTemplateDocRef(templateName);
        if (tRef == null) {
            return null;
        }
        return getSession().getDocument(tRef);
    }

    public List<TemplateSourceDocument> getSourceTemplates() {

        List<TemplateSourceDocument> result = new ArrayList<TemplateSourceDocument>();

        for (TemplateBinding binding : bindings) {
            try {
                result.add(getSourceTemplate(binding.getName()));
            } catch (Exception e) {
                log.error("Unable to fetch source template for binding "
                        + binding.getName());
            }
        }
        return result;
    }

    public String getTemplateType(String templateName) {
        TemplateSourceDocument source = null;
        try {
            source = getSourceTemplate(templateName);
        } catch (Exception e) {
            log.error("Unable to find source template for name " + templateName);
            return null;
        }
        if (source != null) {
            return source.getTemplateType();
        }
        return null;
    }

    public DocumentModel initializeFromTemplate(boolean save) throws Exception {
        return initializeFromTemplate(TemplateBindings.DEFAULT_BINDING, save);
    }

    public DocumentModel initializeFromTemplate(String templateName,
            boolean save) throws Exception {

        TemplateSourceDocument tmpl = getSourceTemplate(templateName);
        if (tmpl == null) {
            throw new ClientException("No associated template for name "
                    + templateName);
        }

        // copy Params but set as readonly all params set in template
        List<TemplateInput> params = tmpl.getParams();
        List<TemplateInput> myParams = new ArrayList<TemplateInput>();
        for (TemplateInput param : params) {
            TemplateInput myParam = param.getCopy(param.isSet());
            myParams.add(myParam);
        }

        bindings.get(templateName).setData(myParams);

        if (tmpl.useAsMainContent()) {
            // copy the template as main blob
            BlobHolder bh = adaptedDoc.getAdapter(BlobHolder.class);
            if (bh != null) {
                bh.setBlob(tmpl.getTemplateBlob());
            }
            bindings.get(templateName).setUseMainContentAsTemplate(true);
        }

        if (save) {
            doSave();
        }
        return adaptedDoc;
    }

    @Override
    protected void doSave() throws ClientException {
        bindings.save(adaptedDoc);
        super.doSave();
    }

    protected void setBlob(Blob blob) throws ClientException {
        adaptedDoc.getAdapter(BlobHolder.class).setBlob(blob);
    }

    protected Blob convertBlob(Blob blob, String format) throws Exception {
        return convertHelper.convertBlob(blob, format);
    }

    public Blob renderWithTemplate(String templateName) throws Exception {
        TemplateProcessor processor = getTemplateProcessor(templateName);
        if (processor != null) {
            Blob blob = processor.renderTemplate(this, templateName);
            String format = getSourceTemplate(templateName).getOutputFormat();
            if (blob != null && format != null && !format.isEmpty()) {
                return convertBlob(blob, format);
            } else {
                return blob;
            }
        } else {
            throw new ClientException(
                    "No template processor found for template type="
                            + getTemplateType(templateName));
        }
    }

    public Blob renderAndStoreAsAttachment(String templateName, boolean save)
            throws Exception {
        Blob blob = renderWithTemplate(templateName);
        setBlob(blob);
        if (save) {
            adaptedDoc = getSession().saveDocument(adaptedDoc);
        }
        return blob;
    }

    public boolean isBidirectional() {
        /*
         * TemplateProcessor processor = getTemplateProcessor(); if (processor
         * != null) { return processor instanceof
         * BidirectionalTemplateProcessor; }
         */
        return false;
    }

    public Blob getTemplateBlob(String templateName) throws Exception {
        TemplateSourceDocument source = getSourceTemplate(templateName);
        if (source != null) {
            if (source.useAsMainContent()) {
                BlobHolder bh = getAdaptedDoc().getAdapter(BlobHolder.class);
                if (bh != null) {
                    Blob blob = bh.getBlob();
                    if (blob != null) {
                        return blob;
                    }
                }
            }
            // get the template from the source
            Blob blob = source.getTemplateBlob();
            return blob;
        }
        // fall back
        BlobHolder bh = getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        } else {
            return bh.getBlob();
        }
    }

    public boolean hasParams(String templateName) throws ClientException {
        return getParams(templateName).size() > 0;
    }

    public List<TemplateInput> getParams(String templateName)
            throws ClientException {

        TemplateBinding binding = bindings.get(templateName);
        if (binding != null) {
            String xml = binding.getData();
            try {
                return XMLSerializer.readFromXml(xml);
            } catch (Exception e) {
                log.error("Unable to parse parameters", e);
                return new ArrayList<TemplateInput>();
            }
        }
        return new ArrayList<TemplateInput>();
    }

    public DocumentModel saveParams(String templateName,
            List<TemplateInput> params, boolean save) throws Exception {
        TemplateBinding binding = bindings.get(templateName);
        if (binding != null) {
            binding.setData(params);
            bindings.save(adaptedDoc);
        }
        if (save) {
            doSave();
        }
        return adaptedDoc;
    }

    protected TemplateProcessor getTemplateProcessor(String templateName) {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        return tps.getProcessor(getTemplateType(templateName));
    }

    public boolean hasEditableParams(String templateName)
            throws ClientException {
        for (TemplateInput param : getParams(templateName)) {
            if (!param.isReadOnly()) {
                return true;
            }
        }
        return false;
    }

    public String getTemplateNameForRendition(String renditionName) {
        try {
            for (TemplateBinding binding : bindings) {
                if (renditionName.equals(getSourceTemplate(binding.getName()).getTargetRenditionName())) {
                    return binding.getName();
                }
            }
        } catch (Exception e) {
            log.error("Unable to resolve rendition binding", e);
        }
        return null;
    }

    public List<String> getTemplateNames() {
        return bindings.getNames();
    }

}

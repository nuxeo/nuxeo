/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 *
 */
package org.nuxeo.template.adapters.doc;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.blob.ConvertBlob;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.XMLSerializer;
import org.nuxeo.template.adapters.AbstractTemplateDocument;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;

/**
 * Default implementation of {@link TemplateBasedDocument} adapter. This adapter mainly expect from the underlying
 * {@link DocumentModel} to have the "TemplateBased" facet
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class TemplateBasedDocumentAdapterImpl extends AbstractTemplateDocument implements Serializable,
        TemplateBasedDocument {

    private static final long serialVersionUID = 1L;

    public static final String TEMPLATEBASED_FACET = "TemplateBased";

    protected final TemplateBindings bindings;

    public TemplateBasedDocumentAdapterImpl(DocumentModel doc) {
        adaptedDoc = doc;
        bindings = new TemplateBindings(doc);
    }

    @Override
    public DocumentModel setTemplate(DocumentModel template, boolean save) {

        TemplateSourceDocument source = template.getAdapter(TemplateSourceDocument.class);
        if (source == null) {
            throw new NuxeoException("Can not bind to an non template document");
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
            initializeFromTemplate(templateName, false);
            bindings.save(adaptedDoc);
            if (save) {
                doSave();
            }

        }
        return adaptedDoc;
    }

    @Override
    public DocumentModel removeTemplateBinding(String templateName, boolean save) {
        if (bindings.containsTemplateName(templateName)) {
            bindings.removeByName(templateName);
            bindings.save(adaptedDoc);
            if (save) {
                doSave();
            }
        }
        return adaptedDoc;
    }

    @Override
    public TemplateSourceDocument getSourceTemplate(String templateName) {
        DocumentModel template = getSourceTemplateDoc(templateName);
        if (template != null) {
            return template.getAdapter(TemplateSourceDocument.class);
        }
        return null;
    }

    @Override
    public DocumentRef getSourceTemplateDocRef(String templateName) {
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

    @Override
    public DocumentModel getSourceTemplateDoc(String templateName) {
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
        try {
            return getSession().getDocument(tRef);
        } catch (DocumentSecurityException e) {
            return null;
        }
    }

    @Override
    public List<TemplateSourceDocument> getSourceTemplates() {
        List<TemplateSourceDocument> result = new ArrayList<TemplateSourceDocument>();
        for (TemplateBinding binding : bindings) {
            TemplateSourceDocument template = getSourceTemplate(binding.getName());
            if (template != null) {
                result.add(template);
            }
        }
        return result;
    }

    @Override
    public String getTemplateType(String templateName) {
        TemplateSourceDocument source = getSourceTemplate(templateName);
        if (source != null) {
            return source.getTemplateType();
        }
        return null;
    }

    public DocumentModel initializeFromTemplate(boolean save) {
        return initializeFromTemplate(TemplateBindings.DEFAULT_BINDING, save);
    }

    @Override
    public DocumentModel initializeFromTemplate(String templateName, boolean save) {

        TemplateSourceDocument tmpl = getSourceTemplate(templateName);
        if (tmpl == null) {
            throw new NuxeoException("No associated template for name " + templateName);
        }

        // copy Params but set as readonly all params set in template
        List<TemplateInput> params = tmpl.getParams();
        List<TemplateInput> myParams = new ArrayList<TemplateInput>();
        for (TemplateInput param : params) {
            boolean readOnly = param.isSet() && !tmpl.allowInstanceOverride();
            TemplateInput myParam = param.getCopy(readOnly);
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
    protected void doSave() {
        bindings.save(adaptedDoc);
        super.doSave();
    }

    protected void setBlob(Blob blob) {
        adaptedDoc.getAdapter(BlobHolder.class).setBlob(blob);
    }

    @Override
    public Blob renderWithTemplate(String templateName) {
        TemplateProcessor processor = getTemplateProcessor(templateName);
        if (processor != null) {
            Blob blob;
            try {
                blob = processor.renderTemplate(this, templateName);
            } catch (IOException e) {
                throw new NuxeoException("Failed to render template: " + templateName, e);
            }
            TemplateSourceDocument template = getSourceTemplate(templateName);
            if (template == null) {
                throw new NuxeoException("No associated template for name " + templateName);
            }
            String format = template.getOutputFormat();
            if (blob != null && format != null && !format.isEmpty()) {
                try {
                    return convertBlob(templateName, blob, format);
                } catch (OperationException e) {
                    throw new NuxeoException(e);
                }
            } else {
                return blob;
            }
        } else {
            String templateType = getTemplateType(templateName);
            if (templateType == null) {
                throw new NuxeoException(
                        "Template type is null : if you don't set it explicitly, your template file should have an extension or a mimetype so that it can be automatically determined");
            } else {
                throw new NuxeoException("No template processor found for template type=" + templateType);
            }
        }
    }

    private Blob convertBlob(String templateName, Blob blob, String outputFormat) throws OperationException {
        OutputFormatDescriptor outFormat = getOutputFormatDescriptor(outputFormat);
        String chainId = outFormat.getChainId();
        String mimeType = outFormat.getMimeType();
        AutomationService automationService = Framework.getLocalService(AutomationService.class);
        try (OperationContext ctx = initOperationContext(blob, templateName)) {
            Object result = null;
            if (chainId != null) {
                ctx.put("templateSourceDocument", getSourceTemplateDoc(templateName));
                ctx.put("templateBasedDocument", adaptedDoc);
                result = automationService.run(ctx, chainId);
            } else if (mimeType != null) {
                OperationChain chain = new OperationChain("convertToMimeType");
                chain.add(ConvertBlob.ID).set("mimeType", mimeType);
                result = automationService.run(ctx, chain);
            }
            if (result != null && result instanceof Blob) {
                return (Blob) result;
            } else {
                return blob;
            }
        }
    }

    protected OperationContext initOperationContext(Blob blob, String templateName) {
        OperationContext ctx = new OperationContext();
        ctx.put("templateName", templateName);
        ctx.setInput(blob);
        ctx.setCommit(false);
        ctx.setCoreSession(getSession());
        return ctx;
    }

    @Override
    public Blob renderAndStoreAsAttachment(String templateName, boolean save) {
        Blob blob = renderWithTemplate(templateName);
        setBlob(blob);
        if (save) {
            adaptedDoc = getSession().saveDocument(adaptedDoc);
        }
        return blob;
    }

    public boolean isBidirectional() {
        /*
         * TemplateProcessor processor = getTemplateProcessor(); if (processor != null) { return processor instanceof
         * BidirectionalTemplateProcessor; }
         */
        return false;
    }

    @Override
    public Blob getTemplateBlob(String templateName) {
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

    @Override
    public boolean hasParams(String templateName) {
        return getParams(templateName).size() > 0;
    }

    @Override
    public List<TemplateInput> getParams(String templateName) {

        TemplateBinding binding = bindings.get(templateName);
        if (binding != null) {
            String xml = binding.getData();
            try {
                return XMLSerializer.readFromXml(xml);
            } catch (DocumentException e) {
                log.error("Unable to parse parameters", e);
                return new ArrayList<TemplateInput>();
            }
        }
        return new ArrayList<TemplateInput>();
    }

    @Override
    public DocumentModel saveParams(String templateName, List<TemplateInput> params, boolean save) {
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

    protected OutputFormatDescriptor getOutputFormatDescriptor(String outputFormat) {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        return tps.getOutputFormatDescriptor(outputFormat);
    }

    @Override
    public boolean hasEditableParams(String templateName) {
        for (TemplateInput param : getParams(templateName)) {
            if (!param.isReadOnly()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getTemplateNameForRendition(String renditionName) {
        for (TemplateBinding binding : bindings) {
            TemplateSourceDocument template = getSourceTemplate(binding.getName());
            if (template != null && renditionName.equals(template.getTargetRenditionName())) {
                return binding.getName();
            }
        }
        return null;
    }

    @Override
    public List<String> getTemplateNames() {
        return bindings.getNames();
    }

}

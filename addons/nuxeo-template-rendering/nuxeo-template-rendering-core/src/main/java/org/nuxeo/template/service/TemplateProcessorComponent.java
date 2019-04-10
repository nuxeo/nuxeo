/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.template.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.template.adapters.doc.TemplateBasedDocumentAdapterImpl;
import org.nuxeo.template.adapters.doc.TemplateBinding;
import org.nuxeo.template.adapters.doc.TemplateBindings;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.api.context.ContextExtensionFactory;
import org.nuxeo.template.api.context.DocumentWrapper;
import org.nuxeo.template.api.descriptor.ContextExtensionFactoryDescriptor;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;
import org.nuxeo.template.context.AbstractContextBuilder;
import org.nuxeo.template.fm.FreeMarkerVariableExtractor;
import org.nuxeo.template.processors.IdentityProcessor;

/**
 * Runtime Component used to handle Extension Points and expose the {@link TemplateProcessorService} interface
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TemplateProcessorComponent extends DefaultComponent implements TemplateProcessorService {

    protected static final Log log = LogFactory.getLog(TemplateProcessorComponent.class);

    public static final String PROCESSOR_XP = "processor";

    public static final String CONTEXT_EXTENSION_XP = "contextExtension";

    public static final String OUTPUT_FORMAT_EXTENSION_XP = "outputFormat";

    private static final String FILTER_VERSIONS_PROPERTY = "nuxeo.templating.filterVersions";

    protected ContextFactoryRegistry contextExtensionRegistry;

    protected TemplateProcessorRegistry processorRegistry;

    protected OutputFormatRegistry outputFormatRegistry;

    protected volatile Map<String, List<String>> type2Template;

    @Override
    public void activate(ComponentContext context) {
        processorRegistry = new TemplateProcessorRegistry();
        contextExtensionRegistry = new ContextFactoryRegistry();
        outputFormatRegistry = new OutputFormatRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        processorRegistry = null;
        contextExtensionRegistry = null;
        outputFormatRegistry = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PROCESSOR_XP.equals(extensionPoint)) {
            processorRegistry.addContribution((TemplateProcessorDescriptor) contribution);
        } else if (CONTEXT_EXTENSION_XP.equals(extensionPoint)) {
            contextExtensionRegistry.addContribution((ContextExtensionFactoryDescriptor) contribution);
            // force recompute of reserved keywords
            FreeMarkerVariableExtractor.resetReservedContextKeywords();
        } else if (OUTPUT_FORMAT_EXTENSION_XP.equals(extensionPoint)) {
            outputFormatRegistry.addContribution((OutputFormatDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PROCESSOR_XP.equals(extensionPoint)) {
            processorRegistry.removeContribution((TemplateProcessorDescriptor) contribution);
        } else if (CONTEXT_EXTENSION_XP.equals(extensionPoint)) {
            contextExtensionRegistry.removeContribution((ContextExtensionFactoryDescriptor) contribution);
        } else if (OUTPUT_FORMAT_EXTENSION_XP.equals(extensionPoint)) {
            outputFormatRegistry.removeContribution((OutputFormatDescriptor) contribution);
        }
    }

    @Override
    public TemplateProcessor findProcessor(Blob templateBlob) {
        TemplateProcessorDescriptor desc = findProcessorDescriptor(templateBlob);
        if (desc != null) {
            return desc.getProcessor();
        } else {
            return null;
        }
    }

    @Override
    public String findProcessorName(Blob templateBlob) {
        TemplateProcessorDescriptor desc = findProcessorDescriptor(templateBlob);
        if (desc != null) {
            return desc.getName();
        } else {
            return null;
        }
    }

    public TemplateProcessorDescriptor findProcessorDescriptor(Blob templateBlob) {
        TemplateProcessorDescriptor processor = null;
        String mt = templateBlob.getMimeType();
        if (mt != null) {
            processor = findProcessorByMimeType(mt);
        }
        if (processor == null) {
            String fileName = templateBlob.getFilename();
            if (fileName != null) {
                String ext = FileUtils.getFileExtension(fileName);
                processor = findProcessorByExtension(ext);
            }
        }
        return processor;
    }

    @Override
    public void addContextExtensions(DocumentModel currentDocument, DocumentWrapper wrapper, Map<String, Object> ctx) {
        Map<String, ContextExtensionFactoryDescriptor> factories = contextExtensionRegistry.getExtensionFactories();
        for (String name : factories.keySet()) {
            ContextExtensionFactory factory = factories.get(name).getExtensionFactory();
            if (factory != null) {
                Object ob = factory.getExtension(currentDocument, wrapper, ctx);
                if (ob != null) {
                    ctx.put(name, ob);
                    // also manage aliases
                    for (String alias : factories.get(name).getAliases()) {
                        ctx.put(alias, ob);
                    }
                }
            }
        }
    }

    @Override
    public List<String> getReservedContextKeywords() {
        List<String> keywords = new ArrayList<>();
        Map<String, ContextExtensionFactoryDescriptor> factories = contextExtensionRegistry.getExtensionFactories();
        for (String name : factories.keySet()) {
            keywords.add(name);
            keywords.addAll(factories.get(name).getAliases());
        }
        for (String keyword : AbstractContextBuilder.RESERVED_VAR_NAMES) {
            keywords.add(keyword);
        }
        return keywords;
    }

    @Override
    public Map<String, ContextExtensionFactoryDescriptor> getRegistredContextExtensions() {
        return contextExtensionRegistry.getExtensionFactories();
    }

    protected TemplateProcessorDescriptor findProcessorByMimeType(String mt) {
        List<TemplateProcessorDescriptor> candidates = new ArrayList<>();
        for (TemplateProcessorDescriptor desc : processorRegistry.getRegistredProcessors()) {
            if (desc.getSupportedMimeTypes().contains(mt)) {
                if (desc.isDefaultProcessor()) {
                    return desc;
                } else {
                    candidates.add(desc);
                }
            }
        }
        if (candidates.size() > 0) {
            return candidates.get(0);
        }
        return null;
    }

    protected TemplateProcessorDescriptor findProcessorByExtension(String extension) {
        List<TemplateProcessorDescriptor> candidates = new ArrayList<>();
        for (TemplateProcessorDescriptor desc : processorRegistry.getRegistredProcessors()) {
            if (desc.getSupportedExtensions().contains(extension)) {
                if (desc.isDefaultProcessor()) {
                    return desc;
                } else {
                    candidates.add(desc);
                }
            }
        }
        if (candidates.size() > 0) {
            return candidates.get(0);
        }
        return null;
    }

    public TemplateProcessorDescriptor getDescriptor(String name) {
        return processorRegistry.getProcessorByName(name);
    }

    @Override
    public TemplateProcessor getProcessor(String name) {
        if (name == null) {
            log.info("no defined processor name, using Identity as default");
            name = IdentityProcessor.NAME;
        }
        TemplateProcessorDescriptor desc = processorRegistry.getProcessorByName(name);
        if (desc != null) {
            return desc.getProcessor();
        } else {
            log.warn("Can not get a TemplateProcessor with name " + name);
            return null;
        }
    }

    protected String buildTemplateSearchQuery(String targetType) {
        StringBuffer sb = new StringBuffer(
                "select * from Document where ecm:mixinType = 'Template' AND ecm:isTrashed = 0");
        if (Boolean.parseBoolean(Framework.getProperty(FILTER_VERSIONS_PROPERTY))) {
            sb.append(" AND ecm:isVersion = 0");
        }
        if (targetType != null) {
            sb.append(" AND tmpl:applicableTypes IN ( 'all', '" + targetType + "')");
        }
        return sb.toString();
    }

    protected String buildTemplateSearchByNameQuery(String name) {
        StringBuffer sb = new StringBuffer(
            "select * from Document where ecm:mixinType = 'Template' AND tmpl:templateName = " + NXQL.escapeString(name));
        if (Boolean.parseBoolean(Framework.getProperty(FILTER_VERSIONS_PROPERTY))) {
            sb.append(" AND ecm:isVersion = 0");
        }
        return sb.toString();
    }

    @Override
    public List<DocumentModel> getAvailableTemplateDocs(CoreSession session, String targetType) {
        String query = buildTemplateSearchQuery(targetType);
        return session.query(query);
    }

    @Override
    public DocumentModel getTemplateDoc(CoreSession session, String name) {
        String query = buildTemplateSearchByNameQuery(name);
        List<DocumentModel> docs = session.query(query);
        return docs.size() == 0 ? null : docs.get(0);
    }

    protected <T> List<T> wrap(List<DocumentModel> docs, Class<T> adapter) {
        List<T> result = new ArrayList<>();
        for (DocumentModel doc : docs) {
            T adapted = doc.getAdapter(adapter);
            if (adapted != null) {
                result.add(adapted);
            }
        }
        return result;
    }

    @Override
    public List<TemplateSourceDocument> getAvailableOfficeTemplates(CoreSession session, String targetType)
            {
        String query = buildTemplateSearchQuery(targetType);
        query = query + " AND tmpl:useAsMainContent=1";
        List<DocumentModel> docs = session.query(query);
        return wrap(docs, TemplateSourceDocument.class);
    }

    @Override
    public List<TemplateSourceDocument> getAvailableTemplates(CoreSession session, String targetType)
            {
        List<DocumentModel> filtredResult = getAvailableTemplateDocs(session, targetType);
        return wrap(filtredResult, TemplateSourceDocument.class);
    }

    @Override
    public List<TemplateBasedDocument> getLinkedTemplateBasedDocuments(DocumentModel source) {
        StringBuffer sb = new StringBuffer(
                "select * from Document where ecm:isVersion = 0 AND ecm:isProxy = 0 AND ");
        sb.append(TemplateBindings.BINDING_PROP_NAME + "/*/" + TemplateBinding.TEMPLATE_ID_KEY);
        sb.append(" = '");
        sb.append(source.getId());
        sb.append("'");
        DocumentModelList docs = source.getCoreSession().query(sb.toString());

        List<TemplateBasedDocument> result = new ArrayList<>();
        for (DocumentModel doc : docs) {
            TemplateBasedDocument templateBasedDocument = doc.getAdapter(TemplateBasedDocument.class);
            if (templateBasedDocument != null) {
                result.add(templateBasedDocument);
            }
        }
        return result;
    }

    @Override
    public Collection<TemplateProcessorDescriptor> getRegisteredTemplateProcessors() {
        return processorRegistry.getRegistredProcessors();
    }

    @Override
    public Map<String, List<String>> getTypeMapping() {
        if (type2Template == null) {
            synchronized (this) {
                if (type2Template == null) {
                    Map<String, List<String>> map = new ConcurrentHashMap<>();
                    TemplateMappingFetcher fetcher = new TemplateMappingFetcher();
                    fetcher.runUnrestricted();
                    map.putAll(fetcher.getMapping());
                    type2Template = map;
                }
            }
        }
        return type2Template;
    }

    @Override
    public synchronized void registerTypeMapping(DocumentModel doc) {
        TemplateSourceDocument tmpl = doc.getAdapter(TemplateSourceDocument.class);
        if (tmpl != null) {
            Map<String, List<String>> mapping = getTypeMapping();
            // check existing mapping for this docId
            List<String> boundTypes = new ArrayList<>();
            for (String type : mapping.keySet()) {
                if (mapping.get(type) != null) {
                    if (mapping.get(type).contains(doc.getId())) {
                        boundTypes.add(type);
                    }
                }
            }
            // unbind previous mapping for this docId
            for (String type : boundTypes) {
                List<String> templates = mapping.get(type);
                templates.remove(doc.getId());
                if (templates.size() == 0) {
                    mapping.remove(type);
                }
            }
            // rebind types (with override)
            for (String type : tmpl.getForcedTypes()) {
                List<String> templates = mapping.get(type);
                if (templates == null) {
                    templates = new ArrayList<>();
                    mapping.put(type, templates);
                }
                if (!templates.contains(doc.getId())) {
                    templates.add(doc.getId());
                }
            }
        }
    }

    @Override
    public DocumentModel makeTemplateBasedDocument(DocumentModel targetDoc, DocumentModel sourceTemplateDoc,
            boolean save) {
        targetDoc.addFacet(TemplateBasedDocumentAdapterImpl.TEMPLATEBASED_FACET);
        TemplateBasedDocument tmplBased = targetDoc.getAdapter(TemplateBasedDocument.class);
        // bind the template
        return tmplBased.setTemplate(sourceTemplateDoc, save);
    }

    @Override
    public DocumentModel detachTemplateBasedDocument(DocumentModel targetDoc, String templateName, boolean save)
            {
        DocumentModel docAfterDetach = null;
        TemplateBasedDocument tbd = targetDoc.getAdapter(TemplateBasedDocument.class);
        if (tbd != null) {
            if (!tbd.getTemplateNames().contains(templateName)) {
                return targetDoc;
            }
            if (tbd.getTemplateNames().size() == 1) {
                // remove the whole facet since there is no more binding
                targetDoc.removeFacet(TemplateBasedDocumentAdapterImpl.TEMPLATEBASED_FACET);
                if (log.isDebugEnabled()) {
                    log.debug("detach after removeFacet, ck=" + targetDoc.getCacheKey());
                }
                if (save) {
                    docAfterDetach = targetDoc.getCoreSession().saveDocument(targetDoc);
                }
            } else {
                // only remove the binding
                docAfterDetach = tbd.removeTemplateBinding(templateName, true);
            }
        }
        if (docAfterDetach != null) {
            return docAfterDetach;
        }
        return targetDoc;
    }

    @Override
    public Collection<OutputFormatDescriptor> getOutputFormats() {
        return outputFormatRegistry.getRegistredOutputFormat();
    }

    @Override
    public OutputFormatDescriptor getOutputFormatDescriptor(String outputFormatId) {
        return outputFormatRegistry.getOutputFormatById(outputFormatId);
    }
}

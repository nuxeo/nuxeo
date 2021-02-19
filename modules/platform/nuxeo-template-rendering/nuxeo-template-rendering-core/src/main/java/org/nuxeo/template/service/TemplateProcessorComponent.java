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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
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

    protected volatile Map<String, List<String>> type2Template;

    @Override
    public void start(ComponentContext context) {
        // force recompute of reserved keywords
        FreeMarkerVariableExtractor.resetReservedContextKeywords();
    }

    /**
     * Force recompute of reserved keywords
     */
    protected void recomputeReservedKeywords() {
        FreeMarkerVariableExtractor.resetReservedContextKeywords();
    }

    @Override
    public TemplateProcessor findProcessor(Blob templateBlob) {
        return findProcessorDescriptor(templateBlob).map(TemplateProcessorDescriptor::getProcessor).orElse(null);
    }

    @Override
    public String findProcessorName(Blob templateBlob) {
        return findProcessorDescriptor(templateBlob).map(TemplateProcessorDescriptor::getName).orElse(null);
    }

    protected Optional<TemplateProcessorDescriptor> findProcessorDescriptor(Blob templateBlob) {
        Optional<TemplateProcessorDescriptor> processor = Optional.ofNullable(templateBlob.getMimeType())
                                                                  .flatMap(this::findProcessorByMimeType);
        if (processor.isEmpty()) {
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
        List<ContextExtensionFactoryDescriptor> contribs = getRegistryContributions(CONTEXT_EXTENSION_XP);
        for (ContextExtensionFactoryDescriptor desc : contribs) {
            ContextExtensionFactory factory = desc.getExtensionFactory();
            if (factory != null) {
                Object ob = factory.getExtension(currentDocument, wrapper, ctx);
                if (ob != null) {
                    ctx.put(desc.getName(), ob);
                    // also manage aliases
                    for (String alias : desc.getAliases()) {
                        ctx.put(alias, ob);
                    }
                }
            }
        }
    }

    @Override
    public List<String> getReservedContextKeywords() {
        List<String> keywords = new ArrayList<>();
        List<ContextExtensionFactoryDescriptor> contribs = getRegistryContributions(CONTEXT_EXTENSION_XP);
        for (ContextExtensionFactoryDescriptor factoryDesc : contribs) {
            keywords.add(factoryDesc.getName());
            keywords.addAll(factoryDesc.getAliases());
        }
        CollectionUtils.addAll(keywords, AbstractContextBuilder.RESERVED_VAR_NAMES);
        return keywords;
    }

    @Override
    public Map<String, ContextExtensionFactoryDescriptor> getRegistredContextExtensions() {
        Map<String, ContextExtensionFactoryDescriptor> map = this.<MapRegistry<ContextExtensionFactoryDescriptor>> getExtensionPointRegistry(
                CONTEXT_EXTENSION_XP).getContributions();
        return Collections.unmodifiableMap(map);
    }

    protected Optional<TemplateProcessorDescriptor> findProcessor(Predicate<TemplateProcessorDescriptor> predicate) {
        return this.<TemplateProcessorDescriptor> getRegistryContributions(PROCESSOR_XP)
                   .stream()
                   .filter(predicate)
                   // preferably take a default processor first
                   .sorted(Comparator.comparing(TemplateProcessorDescriptor::isDefaultProcessor).reversed())
                   .findFirst();
    }

    protected Optional<TemplateProcessorDescriptor> findProcessorByMimeType(String mt) {
        return findProcessor(desc -> desc.getSupportedMimeTypes().contains(mt));
    }

    protected Optional<TemplateProcessorDescriptor> findProcessorByExtension(String extension) {
        return findProcessor(desc -> desc.getSupportedExtensions().contains(extension));
    }

    public TemplateProcessorDescriptor getDescriptor(String name) {
        return this.<TemplateProcessorDescriptor> getRegistryContribution(PROCESSOR_XP, name).orElse(null);
    }

    @Override
    public TemplateProcessor getProcessor(String name) {
        if (name == null) {
            log.info("no defined processor name, using Identity as default");
            name = IdentityProcessor.NAME;
        }
        TemplateProcessorDescriptor desc = getDescriptor(name);
        if (desc != null) {
            return desc.getProcessor();
        } else {
            log.warn("Can not get a TemplateProcessor with name " + name);
            return null;
        }
    }

    protected String buildTemplateSearchQuery(String targetType) {
        String query = "select * from Document where ecm:mixinType = 'Template' AND ecm:isTrashed = 0";
        if (Boolean.parseBoolean(Framework.getProperty(FILTER_VERSIONS_PROPERTY))) {
            query += " AND ecm:isVersion = 0";
        }
        if (targetType != null) {
            query += " AND tmpl:applicableTypes IN ( 'all', '" + targetType + "')";
        }
        return query;
    }

    protected String buildTemplateSearchByNameQuery(String name) {
        String query = "select * from Document where ecm:mixinType = 'Template' " + "AND tmpl:templateName = "
                + NXQL.escapeString(name);
        if (Boolean.parseBoolean(Framework.getProperty(FILTER_VERSIONS_PROPERTY))) {
            query += " AND ecm:isVersion = 0";
        }
        return query;
    }

    @Override
    public List<DocumentModel> getAvailableTemplateDocs(CoreSession session, String targetType) {
        return session.query(buildTemplateSearchQuery(targetType));
    }

    @Override
    public DocumentModel getTemplateDoc(CoreSession session, String name) {
        String query = buildTemplateSearchByNameQuery(name);
        List<DocumentModel> docs = session.query(query);
        return docs.isEmpty() ? null : docs.get(0);
    }

    protected <T> List<T> wrap(List<DocumentModel> docs, Class<T> adapter) {
        return docs.stream().map(doc -> doc.getAdapter(adapter)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<TemplateSourceDocument> getAvailableOfficeTemplates(CoreSession session, String targetType) {
        String query = buildTemplateSearchQuery(targetType) + " AND tmpl:useAsMainContent=1";
        List<DocumentModel> docs = session.query(query);
        return wrap(docs, TemplateSourceDocument.class);
    }

    @Override
    public List<TemplateSourceDocument> getAvailableTemplates(CoreSession session, String targetType) {
        List<DocumentModel> docs = getAvailableTemplateDocs(session, targetType);
        return wrap(docs, TemplateSourceDocument.class);
    }

    @Override
    public List<TemplateBasedDocument> getLinkedTemplateBasedDocuments(DocumentModel source) {
        String query = String.format(
                "select * from Document where ecm:isVersion = 0 AND ecm:isProxy = 0 AND %s/*/%s = '%s'",
                TemplateBindings.BINDING_PROP_NAME, TemplateBinding.TEMPLATE_ID_KEY, source.getId());
        DocumentModelList docs = source.getCoreSession().query(query);
        return wrap(docs, TemplateBasedDocument.class);
    }

    @Override
    public Collection<TemplateProcessorDescriptor> getRegisteredTemplateProcessors() {
        return Collections.unmodifiableCollection(getRegistryContributions(PROCESSOR_XP));
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
            for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
                if (entry.getValue() != null && entry.getValue().contains(doc.getId())) {
                    boundTypes.add(entry.getKey());
                }
            }
            // unbind previous mapping for this docId
            for (String type : boundTypes) {
                List<String> templates = mapping.get(type);
                templates.remove(doc.getId());
                if (templates.isEmpty()) {
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
    public DocumentModel detachTemplateBasedDocument(DocumentModel targetDoc, String templateName, boolean save) {
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
        return Collections.unmodifiableCollection(getRegistryContributions(OUTPUT_FORMAT_EXTENSION_XP));
    }

    @Override
    public OutputFormatDescriptor getOutputFormatDescriptor(String outputFormatId) {
        return this.<OutputFormatDescriptor> getRegistryContribution(OUTPUT_FORMAT_EXTENSION_XP, outputFormatId)
                   .orElse(null);
    }
}

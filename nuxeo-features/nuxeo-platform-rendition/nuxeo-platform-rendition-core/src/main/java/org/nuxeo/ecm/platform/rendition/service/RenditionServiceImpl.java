/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *   Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.rendition.Constants;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.DefaultAutomationRenditionProvider;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.impl.LiveRendition;
import org.nuxeo.ecm.platform.rendition.impl.StoredRendition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Default implementation of {@link RenditionService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public class RenditionServiceImpl extends DefaultComponent implements RenditionService {

    public static final String RENDITION_DEFINITIONS_EP = "renditionDefinitions";

    public static final String RENDITON_DEFINION_PROVIDERS_EP = "renditionDefinitionProviders";

    public static final String DEFAULT_RENDITION_EP = "defaultRendition";

    /**
     * @since 8.1
     */
    public static final String STORED_RENDITION_MANAGERS_EP = "storedRenditionManagers";

    private static final Log log = LogFactory.getLog(RenditionServiceImpl.class);

    /**
     * @deprecated since 7.2. Not used.
     */
    @Deprecated
    protected AutomationService automationService;

    /**
     * @deprecated since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    @Deprecated
    protected Map<String, RenditionDefinition> renditionDefinitions;

    /**
     * @since 7.3.
     */
    protected RenditionDefinitionRegistry renditionDefinitionRegistry;

    protected RenditionDefinitionProviderRegistry renditionDefinitionProviderRegistry;

    protected List<DefaultRenditionDescriptor> defaultRenditionDescriptors = new ArrayList<>();

    protected static final StoredRenditionManager DEFAULT_STORED_RENDITION_MANAGER = new DefaultStoredRenditionManager();

    /**
     * @since 8.1
     */
    protected Deque<StoredRenditionManagerDescriptor> storedRenditionManagerDescriptors = new LinkedList<>();

    protected final ScriptEngineManager scriptEngineManager;

    public RenditionServiceImpl() {
        scriptEngineManager = new ScriptEngineManager();
    }

    /**
     * @since 8.1
     */
    public StoredRenditionManager getStoredRenditionManager() {
        StoredRenditionManagerDescriptor descr = storedRenditionManagerDescriptors.peekLast();
        return descr == null ? DEFAULT_STORED_RENDITION_MANAGER : descr.getStoredRenditionManager();
    }

    @Override
    public void activate(ComponentContext context) {
        renditionDefinitions = new HashMap<>();
        renditionDefinitionRegistry = new RenditionDefinitionRegistry();
        renditionDefinitionProviderRegistry = new RenditionDefinitionProviderRegistry();
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) {
        renditionDefinitions = null;
        renditionDefinitionRegistry = null;
        renditionDefinitionProviderRegistry = null;
        super.deactivate(context);
    }

    public RenditionDefinition getRenditionDefinition(String name) {
        return renditionDefinitionRegistry.getRenditionDefinition(name);
    }

    /**
     * @deprecated since 7.2 because unused
     */
    @Override
    @Deprecated
    public List<RenditionDefinition> getDeclaredRenditionDefinitions() {
        return new ArrayList<>(renditionDefinitionRegistry.descriptors.values());
    }

    /**
     * @deprecated since 7.2 because unused
     */
    @Override
    @Deprecated
    public List<RenditionDefinition> getDeclaredRenditionDefinitionsForProviderType(String providerType) {
        List<RenditionDefinition> defs = new ArrayList<>();
        for (RenditionDefinition def : getDeclaredRenditionDefinitions()) {
            if (def.getProviderType().equals(providerType)) {
                defs.add(def);
            }
        }
        return defs;
    }

    @Override
    public List<RenditionDefinition> getAvailableRenditionDefinitions(DocumentModel doc) {

        List<RenditionDefinition> defs = new ArrayList<>();
        defs.addAll(renditionDefinitionRegistry.getRenditionDefinitions(doc));
        defs.addAll(renditionDefinitionProviderRegistry.getRenditionDefinitions(doc));

        // XXX what about "lost renditions" ?
        return defs;
    }

    @Override
    public DocumentRef storeRendition(DocumentModel source, String renditionDefinitionName) {
        Rendition rendition = getRendition(source, renditionDefinitionName, true);
        return rendition == null ? null : rendition.getHostDocument().getRef();
    }

    /**
     * @deprecated since 8.1
     */
    @Deprecated
    protected DocumentModel storeRendition(DocumentModel sourceDocument, Rendition rendition, String name) {
        StoredRendition storedRendition = storeRendition(sourceDocument, rendition);
        return storedRendition == null ? null : storedRendition.getHostDocument();
    }

    /**
     * @since 8.1
     */
    protected StoredRendition storeRendition(DocumentModel sourceDocument, Rendition rendition) {
        if (!rendition.isCompleted()) {
            return null;
        }
        List<Blob> renderedBlobs = rendition.getBlobs();
        Blob renderedBlob = renderedBlobs.get(0);
        String mimeType = renderedBlob.getMimeType();
        if (mimeType != null
                && (mimeType.contains(LazyRendition.ERROR_MARKER) || mimeType.contains(LazyRendition.STALE_MARKER))) {
            return null;
        }

        CoreSession session = sourceDocument.getCoreSession();
        DocumentModel version = null;
        boolean isVersionable = sourceDocument.isVersionable();
        if (sourceDocument.isVersion()) {
            version = sourceDocument;
            sourceDocument = session.getDocument(new IdRef(version.getSourceId()));
        } else if (isVersionable) {
            DocumentRef versionRef = createVersionIfNeeded(sourceDocument, session);
            version = session.getDocument(versionRef);
        }

        RenditionDefinition renditionDefinition = getRenditionDefinition(rendition.getName());
        return getStoredRenditionManager().createStoredRendition(sourceDocument, version, renderedBlob,
                renditionDefinition);
    }

    protected DocumentRef createVersionIfNeeded(DocumentModel source, CoreSession session) {
        if (source.isVersionable()) {
            if (source.isVersion()) {
                return source.getRef();
            } else if (source.isCheckedOut()) {
                DocumentRef versionRef = session.checkIn(source.getRef(), VersioningOption.MINOR, null);
                source.refresh(DocumentModel.REFRESH_STATE, null);
                return versionRef;
            } else {
                return session.getLastDocumentVersionRef(source.getRef());
            }
        }
        return null;
    }

    /**
     * @deprecated since 7.2. Not used.
     */
    @Deprecated
    protected AutomationService getAutomationService() {
        return Framework.getService(AutomationService.class);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RENDITION_DEFINITIONS_EP.equals(extensionPoint)) {
            RenditionDefinition renditionDefinition = (RenditionDefinition) contribution;
            renditionDefinitionRegistry.addContribution(renditionDefinition);
        } else if (RENDITON_DEFINION_PROVIDERS_EP.equals(extensionPoint)) {
            renditionDefinitionProviderRegistry.addContribution((RenditionDefinitionProviderDescriptor) contribution);
        } else if (STORED_RENDITION_MANAGERS_EP.equals(extensionPoint)) {
            storedRenditionManagerDescriptors.add(((StoredRenditionManagerDescriptor) contribution));
        } else if (DEFAULT_RENDITION_EP.equals(extensionPoint)) {
            // Save contribution
            defaultRenditionDescriptors.add((DefaultRenditionDescriptor) contribution);
        }
    }

    /**
     * @deprecated since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    @Deprecated
    protected void registerRendition(RenditionDefinition renditionDefinition) {
        String name = renditionDefinition.getName();
        if (name == null) {
            log.error("Cannot register rendition without a name");
            return;
        }
        boolean enabled = renditionDefinition.isEnabled();
        if (renditionDefinitions.containsKey(name)) {
            log.info("Overriding rendition with name: " + name);
            if (enabled) {
                renditionDefinition = mergeRenditions(renditionDefinitions.get(name), renditionDefinition);
            } else {
                log.info("Disabled rendition with name " + name);
                renditionDefinitions.remove(name);
            }
        }
        if (enabled) {
            log.info("Registering rendition with name: " + name);
            renditionDefinitions.put(name, renditionDefinition);
        }

        // setup the Provider
        setupProvider(renditionDefinition);
    }

    /**
     * @deprecated since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    @Deprecated
    protected void setupProvider(RenditionDefinition definition) {
        if (definition.getProviderClass() == null) {
            definition.setProvider(new DefaultAutomationRenditionProvider());
        } else {
            try {
                RenditionProvider provider = definition.getProviderClass().newInstance();
                definition.setProvider(provider);
            } catch (Exception e) {
                log.error("Unable to create RenditionProvider", e);
            }
        }
    }

    protected RenditionDefinition mergeRenditions(RenditionDefinition oldRenditionDefinition,
            RenditionDefinition newRenditionDefinition) {
        String label = newRenditionDefinition.getLabel();
        if (label != null) {
            oldRenditionDefinition.label = label;
        }

        String operationChain = newRenditionDefinition.getOperationChain();
        if (operationChain != null) {
            oldRenditionDefinition.operationChain = operationChain;
        }

        return oldRenditionDefinition;
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RENDITION_DEFINITIONS_EP.equals(extensionPoint)) {
            renditionDefinitionRegistry.removeContribution((RenditionDefinition) contribution);
        } else if (RENDITON_DEFINION_PROVIDERS_EP.equals(extensionPoint)) {
            renditionDefinitionProviderRegistry.removeContribution(
                    (RenditionDefinitionProviderDescriptor) contribution);
        } else if (STORED_RENDITION_MANAGERS_EP.equals(extensionPoint)) {
            storedRenditionManagerDescriptors.remove((contribution));
        } else if (DEFAULT_RENDITION_EP.equals(extensionPoint)) {
            defaultRenditionDescriptors.remove(contribution);
        }
    }

    /**
     * @deprecated since 7.3. RenditionDefinitions are store in {@link #renditionDefinitionRegistry}.
     */
    @Deprecated
    protected void unregisterRendition(RenditionDefinition renditionDefinition) {
        String name = renditionDefinition.getName();
        renditionDefinitions.remove(name);
        log.info("Unregistering rendition with name: " + name);
    }

    @Override
    public Rendition getRendition(DocumentModel doc, String renditionName) {
        RenditionDefinition renditionDefinition = getAvailableRenditionDefinition(doc, renditionName);
        return getRendition(doc, renditionDefinition, renditionDefinition.isStoreByDefault());
    }

    @Override
    public Rendition getRendition(DocumentModel doc, String renditionName, boolean store) {
        RenditionDefinition renditionDefinition = getAvailableRenditionDefinition(doc, renditionName);
        return getRendition(doc, renditionDefinition, store);
    }

    protected Rendition getRendition(DocumentModel doc, RenditionDefinition renditionDefinition, boolean store) {

        Rendition rendition;
        boolean isVersionable = doc.isVersionable();
        if (!isVersionable || !doc.isCheckedOut()) {
            // stored renditions are only done against a non-versionable doc
            // or a versionable doc that is not checkedout
            rendition = getStoredRenditionManager().findStoredRendition(doc, renditionDefinition);
            if (rendition != null) {
                return rendition;
            }
        }

        rendition = new LiveRendition(doc, renditionDefinition);

        if (store) {
            StoredRendition storedRendition = storeRendition(doc, rendition);
            if (storedRendition != null) {
                return storedRendition;
            }
        }
        return rendition;
    }

    protected RenditionDefinition getAvailableRenditionDefinition(DocumentModel doc, String renditionName) {
        RenditionDefinition renditionDefinition = renditionDefinitionRegistry.getRenditionDefinition(renditionName);
        if (renditionDefinition == null) {
            renditionDefinition = renditionDefinitionProviderRegistry.getRenditionDefinition(renditionName, doc);
            if (renditionDefinition == null) {
                String message = "The rendition definition '%s' is not registered";
                throw new NuxeoException(String.format(message, renditionName));
            }
        } else {
            // we have a rendition definition but we must check that it can be used for this doc
            if (!renditionDefinitionRegistry.canUseRenditionDefinition(renditionDefinition, doc)) {
                throw new NuxeoException("Rendition " + renditionName + " cannot be used for this doc " + doc.getId());
            }
        }
        if (!renditionDefinition.getProvider().isAvailable(doc, renditionDefinition)) {
            throw new NuxeoException(
                    String.format("Rendition %s not available for this doc %s", renditionName, doc.getPathAsString()));
        }
        return renditionDefinition;
    }

    @Override
    public List<Rendition> getAvailableRenditions(DocumentModel doc) {
        return getAvailableRenditions(doc, false);
    }

    @Override
    public List<Rendition> getAvailableRenditions(DocumentModel doc, boolean onlyVisible) {
        List<Rendition> renditions = new ArrayList<>();

        List<RenditionDefinition> defs = getAvailableRenditionDefinitions(doc);
        if (defs != null) {
            for (RenditionDefinition def : defs) {
                if (!onlyVisible || def.isVisible()) {
                    Rendition rendition = getRendition(doc, def.getName(), false);
                    if (rendition != null) {
                        renditions.add(rendition);
                    }
                }
            }
        }

        return renditions;
    }

    @Override
    public void deleteStoredRenditions(String repositoryName) {
        StoredRenditionsCleaner cleaner = new StoredRenditionsCleaner(repositoryName);
        cleaner.runUnrestricted();
    }

    private final class StoredRenditionsCleaner extends UnrestrictedSessionRunner {

        private static final int BATCH_SIZE = 100;

        private StoredRenditionsCleaner(String repositoryName) {
            super(repositoryName);
        }

        @Override
        public void run() {
            Map<String, List<String>> sourceIdToRenditionRefs = computeLiveDocumentRefsToRenditionRefs();
            removeStoredRenditions(sourceIdToRenditionRefs);
        }

        /**
         * Computes only live documents renditions, the related versions will be deleted by Nuxeo.
         */
        private Map<String, List<String>> computeLiveDocumentRefsToRenditionRefs() {
            Map<String, List<String>> liveDocumentRefsToRenditionRefs = new HashMap<>();
            String query = String.format("SELECT %s, %s, %s FROM Document WHERE %s IS NOT NULL AND ecm:isVersion = 0",
                    NXQL.ECM_UUID, RENDITION_SOURCE_ID_PROPERTY, RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY,
                    RENDITION_SOURCE_ID_PROPERTY);
            try (IterableQueryResult result = session.queryAndFetch(query, NXQL.NXQL)) {
                for (Map<String, Serializable> res : result) {
                    String renditionRef = res.get(NXQL.ECM_UUID).toString();
                    String sourceId = res.get(RENDITION_SOURCE_ID_PROPERTY).toString();
                    Serializable sourceVersionableId = res.get(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY);

                    String key = sourceVersionableId != null ? sourceVersionableId.toString() : sourceId;
                    liveDocumentRefsToRenditionRefs.computeIfAbsent(key, k -> new ArrayList<>()).add(renditionRef);
                }
            }
            return liveDocumentRefsToRenditionRefs;
        }

        private void removeStoredRenditions(Map<String, List<String>> liveDocumentRefsToRenditionRefs) {
            List<String> liveDocumentRefs = new ArrayList<>(liveDocumentRefsToRenditionRefs.keySet());
            if (liveDocumentRefs.isEmpty()) {
                // no more document to check
                return;
            }

            int processedSourceIds = 0;
            while (processedSourceIds < liveDocumentRefs.size()) {
                // compute the batch of source ids to check for existence
                int limit = processedSourceIds + BATCH_SIZE > liveDocumentRefs.size() ? liveDocumentRefs.size()
                        : processedSourceIds + BATCH_SIZE;
                List<String> batchSourceIds = liveDocumentRefs.subList(processedSourceIds, limit);

                // retrieve still existing documents
                List<String> existingSourceIds = new ArrayList<>();
                String query = NXQLQueryBuilder.getQuery("SELECT ecm:uuid FROM Document WHERE ecm:uuid IN ?",
                        new Object[] { batchSourceIds }, true, true, null);
                try (IterableQueryResult result = session.queryAndFetch(query, NXQL.NXQL)) {
                    result.forEach(res -> existingSourceIds.add(res.get(NXQL.ECM_UUID).toString()));
                }
                batchSourceIds.removeAll(existingSourceIds);

                List<String> renditionRefsToDelete = batchSourceIds.stream()
                                                                   .map(liveDocumentRefsToRenditionRefs::get)
                                                                   .reduce(new ArrayList<>(), (allRefs, refs) -> {
                                                                       allRefs.addAll(refs);
                                                                       return allRefs;
                                                                   });

                if (!renditionRefsToDelete.isEmpty()) {
                    session.removeDocuments(
                            renditionRefsToDelete.stream().map(IdRef::new).collect(Collectors.toList()).toArray(
                                    new DocumentRef[renditionRefsToDelete.size()]));
                }

                if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }

                // next batch
                processedSourceIds += BATCH_SIZE;
            }
        }
    }

    @Override
    public Rendition getDefaultRendition(DocumentModel doc, String reason, Map<String, Serializable> extendedInfos) {
        return getDefaultRendition(doc, reason, false, extendedInfos);
    }

    @Override
    public Rendition getDefaultRendition(DocumentModel doc, String reason, boolean store,
            Map<String, Serializable> extendedInfos) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Serializable> ei = extendedInfos == null ? Collections.emptyMap() : extendedInfos;
        NuxeoPrincipal currentUser = ClientLoginModule.getCurrentPrincipal();
        context.put("Document", doc);
        context.put("Infos", ei);
        context.put("CurrentUser", currentUser);
        ScriptEngine engine = null;
        for (int i = defaultRenditionDescriptors.size() - 1; i >= 0; i--) {
            DefaultRenditionDescriptor desc = defaultRenditionDescriptors.get(i);
            if ((StringUtils.isEmpty(reason) && StringUtils.isEmpty(desc.reason))
                    || (reason != null && reason.equals(desc.reason))) {
                String scriptLanguage = desc.getScriptLanguage();
                if (engine == null || !engine.getFactory().getNames().contains(scriptLanguage)) {
                    // Instantiating an engine may be costly, let's keep previous one if same language
                    engine = scriptEngineManager.getEngineByName(scriptLanguage);
                    if (engine == null) {
                        throw new NuxeoException("Engine not found for language: " + scriptLanguage);
                    }
                }
                if (!(engine instanceof Invocable)) {
                    throw new NuxeoException(
                            "Engine " + engine.getClass().getName() + " not Invocable for language: " + scriptLanguage);
                }
                try {
                    engine.eval(desc.getScript());
                    engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(context);
                    Object result = ((Invocable) engine).invokeFunction("run");
                    if (result == null && desc.override) {
                        break;
                    } else {
                        String defaultRenditionName = (String) result;
                        if (!StringUtils.isBlank(defaultRenditionName)) {
                            try {
                                return getRendition(doc, defaultRenditionName, store);
                            } catch (NuxeoException e) {
                                log.error("Unable to use default rendition " + defaultRenditionName, e);
                            }
                        }
                    }

                } catch (NoSuchMethodException e) {
                    throw new NuxeoException("Script does not contain function: run() in defaultRendition: ", e);
                } catch (ScriptException e) {
                    log.error("Failed to evaluate script: ", e);
                }
            }
        }
        if (log.isWarnEnabled()) {
            log.warn(String.format("Failed to get rendition name for reason %s", reason));
        }
        return null;
    }

    @Override
    public DocumentModel publishRendition(DocumentModel doc, DocumentModel target, String renditionName,
            boolean override) {
        Rendition rendition = StringUtils.isEmpty(renditionName)
                ? getDefaultRendition(doc, Constants.DEFAULT_RENDTION_PUBLISH_REASON, true, null)
                : getRendition(doc, renditionName, true);
        if (rendition == null) {
            throw new NuxeoException("Unable to render the document");
        }
        DocumentModel renditionDocument = rendition.getHostDocument();
        DocumentModel publishedDocument = doc.getCoreSession().publishDocument(renditionDocument, target, override);
        if (override) {
            RenditionsRemover remover = new RenditionsRemover(publishedDocument);
            remover.runUnrestricted();
        }
        return publishedDocument;
    }

}

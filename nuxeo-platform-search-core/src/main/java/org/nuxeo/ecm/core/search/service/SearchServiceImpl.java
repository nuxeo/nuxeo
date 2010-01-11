/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SearchServiceImpl.java 30023 2008-02-11 16:15:50Z atchertchian $
 */

package org.nuxeo.ecm.core.search.service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.ResolvedResourcesFactory;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.common.TypeManagerServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.schemas.DefaultSchemaFieldDescriptorsFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.query.impl.SearchPrincipalImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.events.IndexingEventConf;
import org.nuxeo.ecm.core.search.api.events.IndexingEventDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.blobs.BlobExtractorDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.ecm.core.search.api.internals.IndexingThreadPoolDescriptor;
import org.nuxeo.ecm.core.search.api.internals.SearchPolicyDescriptor;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.ecm.core.search.api.security.SearchPolicy;
import org.nuxeo.ecm.core.search.backend.SearchEngineBackendDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo core search service implementation.
 *
 * @see org.nuxeo.ecm.core.search.api.client.SearchService
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class SearchServiceImpl extends DefaultComponent implements
        SearchServiceInternals {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.search.service.SearchServiceImpl");

    private static final long serialVersionUID = 5428662089095318591L;

    private static final Log log = LogFactory.getLog(SearchServiceImpl.class);

    public static final int DEFAULT_MAX_POOL_SIZE = 5;

    public static final int DEFAULT_DOC_BATCH_SIZE = 1;

    private static final String PT_BACKEND = "searchEngineBackend";

    private static final String PT_RESOURCE = "resource";

    private static final String PT_RESOURCE_TYPE = "resourceType";

    private static final String PT_DOCTYPE_INDEX = "indexableDocType";

    private static final String PT_BLOB_EXTRACTOR_DESC = "blobExtractor";

    private static final String PT_FULLTEXT = "fullTextField";

    private static final String PT_EVENTS = "indexingEvent";

    private static final String PT_INDEXING_THREAD_POOL = "indexingThreadPool";

    private static final String PT_POLICIES = "policies";

    /** Is the search service enabled ? */
    private boolean activated = true;

    /** Is the search service reindexing all */
    private boolean reindexingAll = false;

    /** Default backend name for the service. */
    private String defaultBackendName;

    /** Max size of the indexing thread pool */
    private int threadPoolSizeMax = DEFAULT_MAX_POOL_SIZE;

    /**
     * Document batch size. In case of batch indexing we can choose to save the
     * indexing session for a given amount of document. Default is one meaning
     * the session is saved after every insertion. You might want to increase
     * this while performing bulk document imports.
     */
    private int docBatchSize = DEFAULT_DOC_BATCH_SIZE;

    /** Map from backend id to backend instance. */
    private final Map<String, SearchEngineBackend> backends = new HashMap<String, SearchEngineBackend>();

    /** Map from backend id to backend descriptors. */
    private final Map<String, SearchEngineBackendDescriptor> backendDescriptors = new HashMap<String, SearchEngineBackendDescriptor>();

    /**
     * Map from indexable resource conf name to indexable resource conf
     * instance. Key here is the name. See the next one with prefix as a key.
     */
    private final Map<String, IndexableResourceConf> namedResources = new HashMap<String, IndexableResourceConf>();

    /**
     * Map from indexable resource conf prefix to indexable resource conf
     * instance. Here to perform faster lookups.
     */
    private final Map<String, IndexableResourceConf> prefixedResources = new HashMap<String, IndexableResourceConf>();

    /**
     * Map from computed indexable resource conf name to indexable resource conf
     * instance.
     */
    private Map<String, IndexableResourceConf> cNamedResources = new HashMap<String, IndexableResourceConf>();

    /**
     * Map from computed indexable resource conf prefix to indexable resource
     * conf instance.
     */
    private final Map<String, IndexableResourceConf> cPrefixedResources = new HashMap<String, IndexableResourceConf>();

    /** Map from doctype name to indexable resources names. */
    private final Map<String, IndexableDocType> docType2IndexableResourceTypes = new HashMap<String, IndexableDocType>();

    /** Full text descriptor. */
    private final Map<String, FulltextFieldDescriptor> fullTextDescriptors = new HashMap<String, FulltextFieldDescriptor>();

    /** Registry of indexing events */
    private final Map<String, IndexingEventConf> indexingEvents = new HashMap<String, IndexingEventConf>();

    /** Registry of blob extractors * */
    private final Map<String, BlobExtractor> blobExtractors = new HashMap<String, BlobExtractor>();

    /** Registry of resource types * */
    private final Map<String, ResourceTypeDescriptor> resourceTypes = new HashMap<String, ResourceTypeDescriptor>();

    private Map<String, SearchPolicyDescriptor> policyDescriptors;

    private List<SearchPolicy> policies;

    private SchemaManager typeManagerService;

    /** Used for 2-phase registration of backends * */
    private boolean backendsConstructed = false;

    @Override
    public void activate(ComponentContext context) throws Exception {
        policyDescriptors = new Hashtable<String, SearchPolicyDescriptor>();
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        policyDescriptors = null;
        policies = null;
        super.deactivate(context);
    }

    /**
     * Adds a computed indexable resource conf in cache.
     *
     * @param conf an indexable resource configuration instance.
     */
    private void setToCache(IndexableResourceConf conf) {
        cNamedResources.put(conf.getName(), conf);
        cPrefixedResources.put(conf.getPrefix(), conf);
    }

    /**
     * Removes a computed indexable resource conf from cache.
     *
     * @param conf an indexable resource configuration instance.
     */
    private void removeFromCache(IndexableResourceConf conf) {
        cNamedResources.remove(conf.getName());
        cPrefixedResources.remove(conf.getPrefix());
    }

    /**
     * Initializes the type manager service.
     *
     * @return the type manager service
     */
    private SchemaManager getTypeManagerService() {
        if (typeManagerService == null) {
            typeManagerService = TypeManagerServiceDelegate.getRemoteTypeManagerService();
        }
        return typeManagerService;
    }

    public final String getDefaultSearchEngineBakendName() {
        return defaultBackendName;
    }

    public final void setDefaultSearchEngineBackendName(String backendName) {
        defaultBackendName = backendName;
    }

    public final String getPreferedBackendNameFor(ResolvedResource resource) {
        // :TODO:
        // Not possible to dispatch indexing / search in between different
        // backends yet. We need to determine where and how allowing to give the
        // ability to specify this. This doesn't concern the indexing
        // configuration and shouldn't be exposed along with the schema, jpa,
        // whatever indexing configuration. We will have to
        // define a new extension dedicated for backends / resources handling
        // related mappings. As well, the biggest challenge here is to perform
        // join from different backends will searching...
        return defaultBackendName;
    }

    /**
     * Returns the default backend registered for this search service instance.
     *
     * @return a <code>SearchEngineBackend</code> instance
     */
    protected SearchEngineBackend getDefaultBackend() {
        return getSearchEngineBackendByName(defaultBackendName);
    }

    public void index(ResolvedResources sources) throws IndexingException {
        // :XXX: implement backend dispatch.
        SearchEngineBackend backend = getDefaultBackend();
        if (backend != null) {
            backend.index(sources);
        } else {
            log.warn("No search engine backend found !");
        }
    }

    public void index(IndexableResources sources, boolean fulltext)
            throws IndexingException {
        // Do the resolution of resources.
        ResolvedResources resolved = ResolvedResourcesFactory.computeAggregatedResolvedResourcesFrom(
                sources, fulltext);
        index(resolved);
    }

    public void unindex(DocumentModel dm) throws IndexingException {
        deleteAggregatedResources(dm.getId());

        // Perform a query to get all matching children under the path
        ResultSet rset = null;
        try {
            rset = searchQuery(createUnindexPathQuery(dm), 0, 100);
        } catch (SearchException se) {
            throw new IndexingException(se);
        } catch (QueryException qe) {
            throw new IndexingException(qe);
        }

        if (rset == null) {
            if (log.isDebugEnabled()) {
                log.debug("No children to unindex for dm= " + dm);
            }
            return;
        }

        while (true) {
            for (ResultItem item : rset) {
                String key = (String) item.get(BuiltinDocumentFields.FIELD_DOC_UUID);
                if (key != null) {
                    deleteAggregatedResources(key);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No UUID indexed for dm=");
                    }
                }
            }

            if (rset.hasNextPage()) {
                try {
                    rset = rset.nextPage();
                } catch (SearchException se) {
                    throw new IndexingException(se);
                }
            } else {
                break;
            }
        }
    }

    private static ComposedNXQuery createUnindexPathQuery(DocumentModel dm) {
        String queryStr = "SELECT * FROM Document WHERE "
                + BuiltinDocumentFields.FIELD_DOC_PATH + " STARTSWITH " + "'"
                + dm.getPathAsString() + "'";
        return new ComposedNXQueryImpl(queryStr);
    }

    public void clear() throws IndexingException {
        // :XXX: implement backend dispatch
        SearchEngineBackend backend = getDefaultBackend();
        if (backend != null) {
            backend.clear();
        } else {
            log.warn("No search engine backend found !");
        }
    }

    public void deleteAggregatedResources(String key) throws IndexingException {
        // :XXX: implement backend dispatch.
        SearchEngineBackend backend = getDefaultBackend();
        if (backend != null) {
            backend.deleteAggregatedResources(key);
        } else {
            log.warn("No search engine backend found !");
        }
    }

    public void deleteAtomicResource(String key) throws IndexingException {
        // :XXX: implement backend dispatch.
        SearchEngineBackend backend = getDefaultBackend();
        if (backend != null) {
            backend.deleteAtomicResource(key);
        } else {
            log.warn("No search engine backend found !");
        }
    }

    public final SearchEngineBackend getSearchEngineBackendByName(String name) {
        lookupBackends();
        return backends.get(name);
    }

    public final Map<String, SearchEngineBackend> getSearchEngineBackends() {
        lookupBackends();
        return backends;
    }

    private void lookupBackends() {
        if (backendsConstructed) {
            return;
        }

        for (String name : backendDescriptors.keySet()) {
            // :XXX: Deal with override
            backends.put(name, constructBackend(backendDescriptors.get(name)));
        }
        backendsConstructed = true;
    }

    private static SearchEngineBackend constructBackend(
            SearchEngineBackendDescriptor desc) {

        SearchEngineBackend engine = null;

        try {
            engine = Framework.getService(desc.getKlass());
        } catch (Exception e) {
            log.error("Exception in nxruntime's service lookup: "
                    + e.getMessage());
        }
        if (engine == null) { // maybe not NXRuntime managed
            try {
                engine = desc.getKlass().newInstance();
            } catch (InstantiationException e) {
                log.error("Exception in search backend instantiation: "
                        + e.getMessage());
            } catch (IllegalAccessException e) {
                log.error("Exception in search backend instantiation: "
                        + e.getMessage());
            }
        }
        log.debug("Instantiated search engine backend: " + desc.getName());
        String configurationFileName = desc.getConfigurationFileName();
        engine.setName(desc.getName());
        engine.setConfigurationFileName(configurationFileName);

        return engine;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(PT_BACKEND)) {

            // Search engine backend registration
            // Those that are registered as services to NXRuntime
            // are looked up this way. We instantiate the
            // remaining ones.

            SearchEngineBackendDescriptor desc = (SearchEngineBackendDescriptor) contribution;

            if (desc.getName() != null) {
                try {
                    String name = desc.getName();
                    backendDescriptors.put(name, desc);
                    log.debug("Registered search engine descriptor: " + name);
                    defaultBackendName = name;
                    // log.debug(name + " registered as DEFAULT backend");
                    // FIXME: wrong exception to catch
                } catch (NullPointerException ne) {
                    log.error(ne);
                    // FIXME: then what?
                }
            } else {
                log.error("No name for the supplied search engine plugin "
                        + "registration... Cancelling...");
            }

        } else if (extensionPoint.equals(PT_RESOURCE)) {
            IndexableResourceConf conf = (IndexableResourceConf) contribution;

            String resourceName = conf.getName();
            if (resourceName != null) {
                boolean isOverride = namedResources.containsKey(resourceName);
                namedResources.put(resourceName, conf);
                prefixedResources.put(conf.getPrefix(), conf);
                removeFromCache(conf);
                if (isOverride) {
                    log.info("Reregistered resource:" + resourceName);
                } else {
                    log.info("Registered resource: " + resourceName);
                }
            } else {
                log.warn("You need to supply a resource name...");
            }

        } else if (extensionPoint.equals(PT_RESOURCE_TYPE)) {
            ResourceTypeDescriptor desc = (ResourceTypeDescriptor) contribution;
            if (desc.getName() != null) {
                resourceTypes.put(desc.getName(), desc);
                log.info("Registered resource type: " + desc.getName());
            } else {
                log.warn("Resource type can't be registered cause no type name specified");
            }

        } else if (extensionPoint.equals(PT_DOCTYPE_INDEX)) {
            // Doctype to indexable resources mapping registration
            IndexableDocTypeDescriptor desc = (IndexableDocTypeDescriptor) contribution;

            String docType = desc.getType();
            if (docType != null) {
                docType2IndexableResourceTypes.put(docType, desc);
                log.info("Registered indexable doc type: " + docType);
            } else {
                log.error("type is compulsory......."
                        + " Skipping contribution...");
            }

        } else if (extensionPoint.equals(PT_FULLTEXT)) {
            FulltextFieldDescriptor desc = (FulltextFieldDescriptor) contribution;
            log.info("Registered fulltext: " + desc.getName());
            fullTextDescriptors.put(desc.getName(), desc);

        } else if (extensionPoint.equals(PT_EVENTS)) {
            IndexingEventDescriptor desc = (IndexingEventDescriptor) contribution;
            log.info("Registered event: " + desc.getName());
            indexingEvents.put(desc.getName(), desc);

        } else if (extensionPoint.equals(PT_BLOB_EXTRACTOR_DESC)) {
            BlobExtractorDescriptor desc = (BlobExtractorDescriptor) contribution;
            try {
                BlobExtractor extractor = desc.getKlass().newInstance();
                blobExtractors.put(desc.getName(), extractor);
            } catch (InstantiationException e) {
                log.error(e.getMessage());
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }

        } else if (extensionPoint.equals(PT_INDEXING_THREAD_POOL)) {
            IndexingThreadPoolDescriptor desc = (IndexingThreadPoolDescriptor) contribution;
            setNumberOfIndexingThreads(desc.getMaxPoolSize());
            setIndexingDocBatchSize(desc.getDocBatchSize());

        } else if (extensionPoint.equals(PT_POLICIES)) {
            SearchPolicyDescriptor desc = (SearchPolicyDescriptor) contribution;
            registerSearchPolicyDescriptor(desc);

        } else {
            log.error("Wrong extension point name for registration..."
                    + " Check your fragments...=>" + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(PT_BACKEND)) {
            // Search engine backend unregistration
            SearchEngineBackendDescriptor desc = (SearchEngineBackendDescriptor) contribution;

            if (desc.getName() != null) {
                log.debug("Starting a search engine plugin unregistration "
                        + "with name=" + desc.getName());
                backends.remove(desc.getName());
            } else {
                log.error("No name for the supplied search engine plugin "
                        + "unregistration... Cancelling...");
            }

        } else if (extensionPoint.equals(PT_RESOURCE)) {
            // Indexable schema configuration registration
            IndexableResourceDescriptor schema = (IndexableResourceDescriptor) contribution;

            namedResources.remove(schema.getName());
            prefixedResources.remove(schema.getPrefix());

            removeFromCache(schema);

            log.debug("Indexable schema with name=" + schema.getName()
                    + " has been unregistered");

        } else if (extensionPoint.equals(PT_RESOURCE_TYPE)) {
            ResourceTypeDescriptor desc = (ResourceTypeDescriptor) contribution;
            if (desc.getName() != null) {
                resourceTypes.remove(desc.getName());
                log.debug("Resource type with name = " + desc.getName()
                        + " has been unregistered");
            } else {
                log.warn("Resource type can't be registered cause no type name specified");
            }

        } else if (extensionPoint.equals(PT_DOCTYPE_INDEX)) {
            // Doctype to indexable resources mapping registration.
            IndexableDocTypeDescriptor desc = (IndexableDocTypeDescriptor) contribution;

            String docType = desc.getType();
            if (docType != null) {
                if (docType2IndexableResourceTypes.containsKey(docType)) {
                    docType2IndexableResourceTypes.remove(docType);
                    log.debug("Unregister doctype to indexable resources "
                            + "mapping for doctype=" + docType);
                }
            }

        } else if (extensionPoint.equals(PT_FULLTEXT)) {
            FulltextFieldDescriptor desc = (FulltextFieldDescriptor) contribution;
            if (fullTextDescriptors.containsKey(desc.getName())) {
                log.debug("Unregistering fulltext descriptor with name"
                        + desc.getName());
                fullTextDescriptors.remove(desc.getName());
            }

        } else if (extensionPoint.equals(PT_BLOB_EXTRACTOR_DESC)) {
            BlobExtractorDescriptor desc = (BlobExtractorDescriptor) contribution;
            blobExtractors.remove(desc.getName());
            log.debug("Full text extractor with name : " + desc.getName()
                    + " has been unregistered");

        } else if (extensionPoint.equals(PT_POLICIES)) {
            SearchPolicyDescriptor desc = (SearchPolicyDescriptor) contribution;
            unregisterSearchPolicyDescriptor(desc);

        } else {
            log.debug("Nothing to do to unregister contrib=" + extensionPoint);
        }
    }

    public final IndexableResourceConf getIndexableResourceConfByName(
            String name, boolean full) {

        IndexableResourceConf conf = namedResources.get(name);
        if (full) {
            // Take it from the cache if already generated.
            if (cNamedResources.containsKey(name)) {
                return cNamedResources.get(name);
            }

            // Ask for a full computation.
            IndexableResourceConf computedConf = computeResourceConfByName(name);

            // :BBB: Cover case when the schema registered doesn't actually
            // exist. Mostly a testing issue. We might remove this later on.
            if (computedConf != null) {
                conf = computedConf;
                setToCache(conf);
            }
        }
        return conf;
    }

    public final IndexableResourceConf getIndexableResourceConfByPrefix(
            String prefix, boolean full) {

        IndexableResourceConf conf = prefixedResources.get(prefix);

        if (full) {
            // Take it from the cache if already generated.
            if (cPrefixedResources.containsKey(prefix)) {
                return computeResourceConfByPrefix(prefix);
            }

            // Ask for a full computation.
            IndexableResourceConf computeConf = computeResourceConfByPrefix(prefix);
            if (computeConf != null) {
                // :BBB: Cover case when the schema registered doesn't actually
                // exist. Mostly a testing issue. We might remove this later on.
                conf = computeConf;
                setToCache(conf);
            }
        }
        return conf;
    }

    public final Map<String, IndexableResourceConf> getIndexableResourceConfs() {
        return namedResources;
    }

    public final Map<String, IndexableDocType> getIndexableDocTypes() {
        return docType2IndexableResourceTypes;
    }

    public final IndexableDocType getIndexableDocTypeFor(String docType) {
        return docType2IndexableResourceTypes.get(docType);
    }

    public List<String> getSupportedAnalyzersFor(String backendName) {
        SearchEngineBackend backend = getSearchEngineBackendByName(backendName);
        if (backend != null) {
            return backend.getSupportedAnalyzersFor();
        }
        return Collections.emptyList();
    }

    public List<String> getSupportedFieldTypes(String backendName) {
        SearchEngineBackend backend = getSearchEngineBackendByName(backendName);
        if (backend != null) {
            return backend.getSupportedFieldTypes();
        }
        return Collections.emptyList();
    }

    public ResultSet searchQuery(ComposedNXQuery nxqlQuery, int offset,
            int range) throws SearchException, QueryException {
        try {
            // :XXX: implement backend dispatch.
            String backendName = defaultBackendName;
            SearchEngineBackend backend = getSearchEngineBackendByName(backendName);
            if (backend != null) {
                List<SearchPolicy> policies = getSearchPolicies();
                for (SearchPolicy policy : policies) {
                    nxqlQuery = policy.applyPolicy(nxqlQuery);
                }
                return backend.searchQuery(nxqlQuery, offset, range);
            } else {
                throw new SearchException("No backend with name=" + backendName
                        + " found ! ");
            }
        } catch (Throwable t) {
            throw new SearchException(t);
        }
    }

    public ResultSet searchQuery(NativeQuery nativeQuery, int offset, int range)
            throws SearchException, QueryException {
        try {
            // :XXX: implement backend dispatch.
            SearchEngineBackend backend = getSearchEngineBackendByName(nativeQuery.getBackendName());
            if (backend != null) {
                return backend.searchQuery(nativeQuery, offset, range);
            } else {
                throw new SearchException("No backend with name="
                        + nativeQuery.getBackendName() + " found ! ");
            }
        } catch (Throwable t) {
            throw new SearchException(t);
        }
    }

    public ResultSet searchQuery(NativeQueryString queryString,
            String backendName, int offset, int range) throws SearchException,
            QueryException {
        try {
            // :XXX: implement backend dispatch.
            SearchEngineBackend backend = getSearchEngineBackendByName(backendName);
            if (backend != null) {
                return backend.searchQuery(queryString, offset, range);
            } else {
                throw new SearchException("No backend with name=" + backendName
                        + " found ! ");
            }
        } catch (Throwable t) {
            throw new SearchException(t);
        }
    }

    public final String[] getAvailableBackendNames() {
        lookupBackends();
        String[] names = new String[backends.size()];
        backends.keySet().toArray(names);
        return names;
    }

    public final IndexableResourceDataConf getIndexableDataConfFor(
            String dataName) {
        String[] split = dataName.split(":", 2);
        if (split.length < 2) {
            return null;
        }

        IndexableResourceConf conf = getIndexableResourceConfByPrefix(split[0],
                true);
        if (conf != null) {
            return conf.getIndexableFields().get(split[1]);
        }

        // :XXX: Deal with other types
        return null;
    }

    public final IndexableResourceDataConf getIndexableDataConfByName(
            String name) {
        // :XXX: SERIOUS OPTIMIZATION NEEDED USING CACHE AND CROSS REFERENCES.
        // :XXX: OR TO BE REMOVED (SOON)
        IndexableResourceDataConf matchingDataConf = null;
        for (IndexableResourceConf conf : namedResources.values()) {
            matchingDataConf = conf.getIndexableFields().get(name);
            if (matchingDataConf != null) {
                return matchingDataConf;
            }
        }
        return matchingDataConf;
    }

    public final SearchPrincipal getSearchPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        String name = principal.getName();
        // :FIXME: find a better way to find this out. For now this is the
        // only available way of doing it.
        boolean isSystemUser = SecurityConstants.SYSTEM_USERNAME.equals(name);

        String[] groups;
        boolean isAdministrator = false;
        if (principal instanceof NuxeoPrincipal) {
            NuxeoPrincipal nuxeoPrincipal = (NuxeoPrincipal) principal;
            // security checks are done on the transitive closure of group
            // membership
            groups = nuxeoPrincipal.getAllGroups().toArray(
                    new String[nuxeoPrincipal.getAllGroups().size() + 1]);
            groups[groups.length - 1] = SecurityConstants.EVERYONE;
            isAdministrator = nuxeoPrincipal.isAdministrator();
        } else {
            // decided not to add EVERYONE because that's a Nuxeo concept
            // TODO adapt when we have real use cases of this
            groups = new String[0];
        }
        return new SearchPrincipalImpl(name, groups, isSystemUser,
                isAdministrator, principal);
    }

    public final boolean isEnabled() {
        return activated;
    }

    public final void setStatus(boolean active) {
        log.debug("Set to status: " + active);
        activated = active;
    }

    public final FulltextFieldDescriptor getFullTextDescriptorByName(String name) {
        if (name != null) {
            return fullTextDescriptors.get(name);
        }
        return null;
    }

    public IndexingEventConf getIndexingEventConfByName(String name) {
        return indexingEvents.get(name);
    }

    /**
     * Computes resource conf given its name.
     *
     * @param name resource name.
     * @return an <code>IndexableResourceConf</code> instance.
     */
    private IndexableResourceConf computeResourceConfByName(String name) {

        IndexableResourceConf conf = getIndexableResourceConfByName(name, false);

        // No configuration found for this resource. Let's compute it.
        if (conf == null) {

            // :XXX: Move this code somewhere else.

            // Handle schema case : we need a dedicated helper for each resource
            // type. Here the resource might not even be registered at all in
            // case of automatic resource configurations
            DefaultSchemaFieldDescriptorsFactory schemaFieldFactory = new DefaultSchemaFieldDescriptorsFactory();

            // :FIXME: we cannot be sure this is a schema just given its name...
            Schema schema = schemaFieldFactory.getSchemaByName(name);
            if (schema != null) {

                String prefix = schema.getNamespace().prefix;
                if (prefix.equals("")) {
                    prefix = name;
                }

                Set<String> excludedFields = new HashSet<String>();
                Map<String, IndexableResourceDataConf> fields = new HashMap<String, IndexableResourceDataConf>();

                List<IndexableFieldDescriptor> descs = schemaFieldFactory.getFieldDescriptorsBySchemaName(
                        name, excludedFields);
                for (IndexableFieldDescriptor desc : descs) {
                    fields.put(desc.getIndexingName(), desc);
                }

                conf = new IndexableResourceDescriptor(name, prefix, true,
                        excludedFields, fields, ResourceType.SCHEMA);
            }
        } else {

            // :XXX: Move this code somwhere else.

            // Exclude the ones specified by the user.
            for (String excludedField : conf.getExcludedFields()) {
                if (conf.getIndexableFields().containsKey(excludedField)) {
                    conf.getIndexableFields().remove(excludedField);
                }
            }

            // Here let's complete the configuration to include all
            // fields.
            if (conf.areAllFieldsIndexable()) {

                DefaultSchemaFieldDescriptorsFactory schemaFieldFactory = new DefaultSchemaFieldDescriptorsFactory();

                // Let's exclude the one already provided by
                // configuration along with the one explictly
                // specified to not index.
                Set<String> excluded = new HashSet<String>();
                excluded.addAll(conf.getIndexableFields().keySet());
                for (String explicitExclude : conf.getExcludedFields()) {
                    if (!excluded.contains(explicitExclude)) {
                        excluded.add(explicitExclude);
                    }
                }

                List<IndexableFieldDescriptor> fieldConfs = schemaFieldFactory.getFieldDescriptorsBySchemaName(
                        name, excluded);

                // Add them to the configuration object.
                for (IndexableFieldDescriptor fieldConf : fieldConfs) {
                    conf.getIndexableFields().put(fieldConf.getIndexingName(),
                            fieldConf);
                }
            }
        }

        return conf;
    }

    /**
     * Computes resource conf given its prefix.
     *
     * @param prefix the resource prefix.
     * @return an <code>IndexableResourceConf</code> instance.
     */
    private IndexableResourceConf computeResourceConfByPrefix(String prefix) {

        // Handle schema case : we need a dedicated helper for each resource
        // type. Here the resource might not even be registered at all in case
        // of automatic resource configurations
        DefaultSchemaFieldDescriptorsFactory schemaFieldFactory = new DefaultSchemaFieldDescriptorsFactory();

        // :FIXME: we cannot be sure this is a schema just given its name...
        Schema schema = schemaFieldFactory.getSchemaByPrefix(prefix);
        if (schema == null) {
            // Try out with the name if no prefix are declared on the schema
            schema = schemaFieldFactory.getSchemaByName(prefix);
        }

        if (schema != null) {
            return computeResourceConfByName(schema.getName());
        }
        return null;
    }

    public final Set<String> getDocumentTypeNamesForFacet(String facet) {
        SchemaManager typeManager = getTypeManagerService();
        if (typeManager != null) {
            return typeManagerService.getDocumentTypeNamesForFacet(facet);
        } else {
            log.error("Type manager service cannot be found....");
            return new HashSet<String>();
        }
    }

    public Set<String> getDocumentTypeNamesExtending(String docType) {
        SchemaManager typeManager = getTypeManagerService();
        if (typeManager != null) {
            return typeManagerService.getDocumentTypeNamesExtending(docType);
        } else {
            log.error("Type manager service cannot be found....");
            return null;
        }
    }

    public final Set<String> getDocumentTypeNamesForFacet(
            Collection<String> facets) {

        SchemaManager typeManager = getTypeManagerService();
        if (typeManager == null) {
            log.error("Type manager service cannot be found...");
            return new HashSet<String>();
        }

        Set<String> result = null;
        for (String facet : facets) {
            Set<String> forOne = typeManager.getDocumentTypeNamesForFacet(facet);
            if (forOne == null) {
                continue;
            }
            if (result == null) {
                result = new HashSet<String>();
            }
            result.addAll(forOne);
        }
        return result;
    }

    public final void invalidateComputedIndexableResourceConfs() {
        cNamedResources = new HashMap<String, IndexableResourceConf>();
    }

    public final BlobExtractor getBlobExtractorByName(String name) {
        return blobExtractors.get(name);
    }

    public ResourceTypeDescriptor getResourceTypeDescriptorByName(String name) {
        return resourceTypes.get(name);
    }

    public long getIndexingWaitingQueueSize() {
        return 0;
    }

    public int getNumberOfIndexingThreads() {
        return threadPoolSizeMax;
    }

    public void closeSession(String sid) {
        // :XXX: implement backend dispatch.
        SearchEngineBackend backend = getDefaultBackend();
        if (backend != null) {
            backend.closeSession(sid);
        } else {
            log.warn("No search engine backend found !");
        }
    }

    public SearchServiceSession openSession() {
        // :XXX: implement backend dispatch.
        SearchEngineBackend backend = getDefaultBackend();
        if (backend != null) {
            return backend.createSession();
        } else {
            log.warn("No search engine backend found !");
            return null;
        }
    }

    public int getIndexingDocBatchSize() {
        return docBatchSize;
    }

    public void setIndexingDocBatchSize(int docBatchSize) {
        this.docBatchSize = docBatchSize;
        log.info("Setting indexing batch size: "
                + Integer.toString(this.docBatchSize));
    }

    public void setNumberOfIndexingThreads(int numberOfIndexingThreads) {
        log.info("Setting indexing thread pool size: "
                + Integer.toString(numberOfIndexingThreads));
        threadPoolSizeMax = numberOfIndexingThreads;
    }

    public void saveAllSessions() throws IndexingException {
        // :XXX: implement backend dispatch.

        reindexingAll = true;

        try {
            SearchEngineBackend backend = getDefaultBackend();
            if (backend != null) {
                backend.saveAllSessions();
                log.debug("Saving all sessions");
            } else {
                log.warn("No search engine backend found !");
            }

        } finally {
            reindexingAll = false;
        }
    }

    public static CoreSession getCoreSession(String repoName)
            throws IndexingException {
        log.debug("Opening a new Session against Nuxeo Core");
        try {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            return mgr.getRepository(repoName).open();
        } catch (Exception e) {
            throw new IndexingException(e);
        }
    }

    public void reindexAll(String repoName, String path, boolean fulltext)
            throws IndexingException {
        log.error("reindexAll is deprecated and does nothing");
    }

    public int getActiveIndexingTasks() {
        return 0;
    }

    public long getTotalCompletedIndexingTasks() {
        return 0;
    }

    public boolean isReindexingAll() {
        return reindexingAll;
    }

    public void setReindexingAll(boolean flag) {
        reindexingAll = flag;
    }

    // search policy methods

    private void computeSearchPolicies() {
        policies = new ArrayList<SearchPolicy>();
        List<SearchPolicyDescriptor> orderedDescriptors = new ArrayList<SearchPolicyDescriptor>();
        for (SearchPolicyDescriptor descriptor : policyDescriptors.values()) {
            if (descriptor.isEnabled()) {
                orderedDescriptors.add(descriptor);
            }
        }
        Collections.sort(orderedDescriptors);
        List<String> policyNames = new ArrayList<String>();
        for (SearchPolicyDescriptor descriptor : orderedDescriptors) {
            if (descriptor.isEnabled()) {
                try {
                    Object policy = descriptor.getPolicy().newInstance();
                    if (policy instanceof SearchPolicy) {
                        policies.add((SearchPolicy) policy);
                        policyNames.add(descriptor.getName());
                    } else {
                        log.error(String.format(
                                "Invalid contribution to search policy %s:"
                                        + " must implement SearchPolicy interface",
                                descriptor.getName()));
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
        log.debug("Ordered search policies: " + policyNames.toString());
    }

    private List<SearchPolicy> getSearchPolicies() {
        if (policies == null) {
            computeSearchPolicies();
        }
        return policies;
    }

    private void resetSearchPolicies() {
        policies = null;
    }

    private void registerSearchPolicyDescriptor(
            SearchPolicyDescriptor descriptor) {
        String id = descriptor.getName();
        if (policyDescriptors.containsKey(id)) {
            log.info("Overriding security policy: " + id);
        }
        policyDescriptors.put(id, descriptor);
        resetSearchPolicies();
    }

    private void unregisterSearchPolicyDescriptor(
            SearchPolicyDescriptor descriptor) {
        String id = descriptor.getName();
        if (policyDescriptors.containsKey(id)) {
            policyDescriptors.remove(id);
            resetSearchPolicies();
        }
    }

}

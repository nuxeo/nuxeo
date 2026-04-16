/*
 * (C) Copyright 2024-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static java.util.Objects.requireNonNull;
import static org.nuxeo.ecm.core.search.index.IndexingBackgroundAction.IndexingBackgroundComputation.INDEXES_PARAM;
import static org.nuxeo.runtime.api.login.LoginComponent.SYSTEM_USERNAME;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.function.ThrowablePredicate;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.search.index.IndexingAction;
import org.nuxeo.ecm.core.search.index.IndexingBackgroundAction;
import org.nuxeo.ecm.core.search.index.IndexingJsonWriter;
import org.nuxeo.ecm.core.search.index.IndexingProcessor;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 2025.0
 */
public class SearchServiceImpl implements SearchService, SearchIndexingService {

    private static final Logger log = LogManager.getLogger(SearchServiceImpl.class);

    /**
     * Property to enable indexing of Relation documents. When set to {@code true}, Relation documents are included in
     * search indexing and queries. Default is {@code false}.
     *
     * @since 2025.19
     */
    public static final String RELATION_INDEXING_ENABLED_PROP = "nuxeo.search.indexing.relation.enabled";

    protected static final int LOAD_SOURCES_TIMEOUT = (int) Duration.ofMinutes(1).toSeconds();

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String SELECT_DOCUMENTS_IN = "SELECT * FROM %s WHERE ecm:uuid IN ('%s')";

    protected static final String NXQL_ALL_DOCUMENTS = "SELECT * FROM %s";

    protected final Map<String, SearchClient> searchClients = new HashMap<>();

    protected final Map<String, SearchIndex> searchIndexes = new HashMap<>();

    protected final Map<String, String> repoToDefaultIndex = new HashMap<>();

    protected final Map<String, List<String>> repoToIndexes = new HashMap<>();

    protected final Map<String, IndexingJsonWriter> indexToJsonWriter = new HashMap<>();

    protected final String defaultRepository;

    public SearchServiceImpl(List<SearchClientDescriptor> clients, List<SearchIndexDescriptor> indexes) {
        RepositoryManager repoManager = Framework.getService(RepositoryManager.class);
        if (repoManager != null) {
            defaultRepository = repoManager.getDefaultRepositoryName();
        } else if (Framework.isTestModeSet()) {
            defaultRepository = "test";
        } else {
            throw new IllegalStateException("No repository manager available to get the default repository");
        }
        // collect clients used by contributed indexes
        var contributedClientIds = indexes.stream().map(SearchIndexDescriptor::getClient).collect(Collectors.toSet());
        for (SearchClientDescriptor descriptor : clients) {
            if (contributedClientIds.contains(descriptor.getId())) {
                log.debug("Retrieving SearchClient: '{}' with factory: '{}", descriptor::getId,
                        descriptor::getFactoryClass);
                var searchClient = Framework.getService(descriptor.getFactoryClass())
                                            .getSearchClient(descriptor.getName());
                searchClients.put(descriptor.getId(), searchClient);
                if (!searchClient.isReady()) {
                    throw new IllegalStateException("SearchClient: " + searchClient + " is not ready.");
                }
            }
        }
        // collect indexes
        for (SearchIndexDescriptor descriptor : indexes) {
            String repo = descriptor.getRepositoryName();
            SearchIndex index = SearchIndex.of(repo, searchClients.get(descriptor.getClient()).getName(),
                    descriptor.getId());
            searchIndexes.put(index.index(), index);
            repoToIndexes.computeIfAbsent(repo, k -> new ArrayList<>()).add(index.index());
            if (descriptor.isDefault() || !repoToDefaultIndex.containsKey(repo)) {
                var previousIndex = repoToDefaultIndex.put(repo, index.index());
                if (previousIndex != null) {
                    log.warn("The index: {} is overriding the index: {} to be the default for the repository: {}",
                            index.index(), previousIndex, repo);
                }
            }
            indexToJsonWriter.put(index.index(), descriptor.newWriterInstance());
        }
    }

    /**
     * Returns the NXQL FROM clause depending on whether Relation indexing is enabled.
     *
     * @since 2025.19
     */
    public static String getFromClause() {
        return Framework.isBooleanPropertyTrue(RELATION_INDEXING_ENABLED_PROP) ? "Document, Relation" : "Document";
    }

    @Override
    public String getDefaultRepositoryName() {
        return defaultRepository;
    }

    @Override
    public Set<String> getRepositoryNames() {
        return repoToIndexes.keySet();
    }

    @Override
    public String getDefaultIndexName(String repository) {
        return repoToDefaultIndex.get(repository);
    }

    @Override
    public List<String> getIndexNames(String repository) {
        return Collections.unmodifiableList(repoToIndexes.getOrDefault(repository, Collections.emptyList()));
    }

    @Override
    public SearchIndex getSearchIndex(String indexName) {
        return requireNonNull(searchIndexes.get(indexName), () -> "Unknown index: '" + indexName + "'");
    }

    @SuppressWarnings("removal")
    @Override
    public SearchIndex getDefaultSearchIndexForRepository(String repository) {
        return getSearchIndex(repoToDefaultIndex.get(repository));
    }

    @SuppressWarnings("removal")
    @Override
    public List<SearchIndex> getSearchIndexForRepository(String repository) {
        return getIndexNames(repository).stream().map(this::getSearchIndex).toList();
    }

    @Override
    public SearchResponse search(SearchQuery query) {
        log.debug("Searching: {}", query);
        var client = searchClients.get(query.getSearchIndexes().getFirst().client());
        if (query.isMultiRepositories() && !client.hasCapability(SearchClient.Capability.MULTI_REPOSITORIES)) {
            throw new SearchClientException("Client: " + client + " has no multi repository search capability");
        }
        SearchResponse response = client.search(query);
        log.debug("Response: {}", response);
        return response;
    }

    @Override
    public SearchResponse searchScroll(SearchScrollContext scrollContext) {
        log.debug("Searching next scroll: {}", scrollContext);
        var client = searchClients.get(scrollContext.searchQuery().getSearchIndexes().getFirst().client());
        SearchResponse response = client.searchScroll(scrollContext);
        log.debug("Response: {}", response);
        return response;
    }

    @Override
    public boolean clearSearchScroll(SearchScrollContext scrollContext) {
        log.debug("Clear search scroll: {}", scrollContext);
        var client = searchClients.get(scrollContext.searchQuery().getSearchIndexes().getFirst().client());
        boolean response = client.clearScroll(scrollContext);
        log.debug("Response: {}", response);
        return response;
    }

    @Override
    public BulkIndexingResponse indexDocuments(BulkIndexingRequest request) {
        loadSources(request);
        log.debug("Indexing {} documents ({} delete): {}", request::size, request::sizeDelete, () -> request);
        var response = getClient(request.getSearchIndex().client()).indexDocuments(request);
        log.debug("Response: {}", response);
        return response;
    }

    protected void loadSources(BulkIndexingRequest bulk) {
        List<String> documentIds = bulk.getRequests()
                                       .stream()
                                       .filter(IndexingRequest::isUpsert)
                                       .map(IndexingRequest::getDocumentId)
                                       .toList();
        if (documentIds.isEmpty()) {
            return;
        }
        // reset sources if any
        bulk.getRequests()
            .stream()
            .filter(req -> documentIds.contains(req.documentId))
            .forEach(req -> req.setSource(null));
        log.debug("Loading {} document sources for: {}", documentIds.size(), documentIds);
        TransactionHelper.runInTransaction(LOAD_SOURCES_TIMEOUT, () -> {
            DocumentModelList documents;
            try (NuxeoLoginContext ignored = Framework.loginSystem()) {
                CoreSession session = CoreInstance.getCoreSession(bulk.getSearchIndex().repository());
                documents = loadDocuments(session, documentIds);
            }
            IndexingJsonWriter writer = indexToJsonWriter.get(bulk.getSearchIndex().index());
            for (DocumentModel doc : documents) {
                String json = json(writer, doc);
                bulk.getRequests()
                    .stream()
                    .filter(r -> r.getDocumentId().equals(doc.getId()))
                    .forEach(r -> r.setSource(json));
            }
        });
        log.debug("Loaded");
    }

    protected DocumentModelList loadDocuments(CoreSession session, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return new DocumentModelListImpl(0);
        }
        try {
            DocumentModelList ret = session.query(
                    SELECT_DOCUMENTS_IN.formatted(getFromClause(), String.join("', '", documentIds)));
            if (log.isDebugEnabled() && ret.size() < documentIds.size()) {
                // some documents might have been deleted since scroller projection
                List<String> notFound = new ArrayList<>(documentIds);
                ret.forEach(doc -> notFound.remove(doc.getId()));
                log.debug("Some documents are not accessible: {}", notFound);
            }
            return ret;
        } catch (DocumentNotFoundException | PropertyConversionException | IllegalArgumentException e) {
            // A corrupted document prevents to load the batch of docs
            log.warn("Fail to loadDocuments because of: {}, retrying without batching", e.getMessage());
            return loadDocumentsOneByOne(session, documentIds);
        }
    }

    protected DocumentModelList loadDocumentsOneByOne(CoreSession session, List<String> documentIds) {
        DocumentModelList ret = new DocumentModelListImpl(documentIds.size());
        for (String documentId : documentIds) {
            try {
                ret.add(session.getDocument(new IdRef(documentId)));
            } catch (DocumentNotFoundException e) {
                log.debug("Document: {} does not exists: {}", documentId, e.getMessage());
            } catch (PropertyConversionException | IllegalArgumentException e) {
                log.atError()
                   .withThrowable(log.isDebugEnabled() ? e : null)
                   .log("Skipping corrupted doc: {}, because of: {}", documentId, e.getMessage());
            }
        }
        return ret;
    }

    protected String json(IndexingJsonWriter writer, DocumentModel doc) {
        try (StringWriter stringWriter = new StringWriter();
                JsonGenerator generator = MAPPER.getFactory().createGenerator(stringWriter)) {
            writer.writeDocument(generator, doc);
            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String reindexRepository(String repository) {
        return reindexRepository(repository, null);
    }

    @Override
    public String reindexRepository(String repository, List<String> indexNames) {
        List<SearchIndex> indexes;
        log.debug("Reindexing repository: {} on indexes: {}", repository, indexNames);
        if (CollectionUtils.isEmpty(indexNames)) {
            indexes = getIndexNames(repository).stream().map(this::getSearchIndex).toList();
        } else {
            checkAllIndexInRepo(repository, indexNames);
            indexes = indexNames.stream().map(this::getSearchIndex).toList();
        }
        var idxNames = indexes.stream().map(SearchIndex::index).toList();
        BulkService bulkService = Framework.getService(BulkService.class);
        indexes.forEach(index -> getClient(index.client()).dropAndInitIndex(index.index()));
        String commandId = bulkService.submit(new BulkCommand.Builder(IndexingBackgroundAction.ACTION_NAME,
                NXQL_ALL_DOCUMENTS.formatted(getFromClause()), SYSTEM_USERNAME)
                                                                               .repository(repository)
                                                                               .param(INDEXES_PARAM,
                                                                                       (Serializable) idxNames)
                                                                               .build());
        log.warn("Reindexing repository: {}, with bulk command: {} on indexes: {}", repository, commandId, idxNames);
        return commandId;
    }

    @Override
    public String reindexDocuments(String repository, String nxql, long queryLimit) {
        return reindexDocuments(repository, nxql, queryLimit, null);
    }

    @Override
    public String reindexDocuments(String repository, String nxql, long queryLimit, List<String> indexNames) {
        log.debug("Reindexing documents on repository: {} with nxql: {}, limit: {}, indexes: {}", repository, nxql,
                queryLimit, indexNames);
        BulkService bulkService = Framework.getService(BulkService.class);
        var builder = new BulkCommand.Builder(IndexingBackgroundAction.ACTION_NAME, nxql, SYSTEM_USERNAME).repository(
                repository);
        if (queryLimit > 0) {
            builder.queryLimit(queryLimit);
        }
        if (CollectionUtils.isNotEmpty(indexNames)) {
            checkAllIndexInRepo(repository, indexNames);
            builder.param(INDEXES_PARAM, (Serializable) indexNames);
        }
        String commandId = bulkService.submit(builder.build());
        log.warn("Reindexing documents on repository: {} using: {}{}, with bulk command: {} on indexes: {}", repository,
                nxql, queryLimit > 0 ? " limit: " + queryLimit : "", commandId,
                CollectionUtils.isEmpty(indexNames) ? "all" : indexNames);
        return commandId;
    }

    protected void checkAllIndexInRepo(String repository, List<String> indexNames) {
        try {
            List<SearchIndex> indexes = indexNames.stream().map(this::getSearchIndex).toList();
            boolean allSameRepository = indexes.stream().allMatch(index -> repository.equals(index.repository()));
            if (!allSameRepository) {
                throw new IllegalArgumentException("All search indexes must point to the same repository");
            }
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void refresh(SearchIndex index) {
        log.debug("Refreshing index: {}", index);
        getClient(index.client()).refresh(index.index());
    }

    @Override
    public SearchClient getClient(String clientName) {
        return searchClients.get(clientName);
    }

    @Override
    public boolean await(Duration duration) throws InterruptedException {
        long start = System.currentTimeMillis();
        // only wait for async, the sync processing is done on afterCompletion
        if (Framework.getService(StreamService.class)
                     .await(Name.ofUrn(IndexingProcessor.STREAM_NAME),
                             Name.ofUrn(IndexingProcessor.ASYNC_COMPUTATION_NAME), duration)) {
            // now wait for BulkService as async indexing might submit bulk command
            var bulkService = Framework.getService(BulkService.class);
            if (bulkService.getStatuses(SYSTEM_USERNAME)
                           .stream()
                           .filter(bulkStatus -> IndexingAction.ACTION_NAME.equals(bulkStatus.getAction()))
                           .allMatch(ThrowablePredicate.asPredicate(bulkStatus -> bulkService.await(bulkStatus.getId(),
                                   // compute remaining duration
                                   duration.minusMillis(System.currentTimeMillis() - start))))) {
                log.debug("Async indexing completed in {}ms", () -> System.currentTimeMillis() - start);
                return true;
            }
        }
        log.warn("Await timeout on async indexing");
        return false;
    }
}

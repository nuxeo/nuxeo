/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.query.api.PageProvider.HIGHLIGHT_CTX_DATA;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * @since 2025.0
 */
public class SearchResponseImpl implements SearchResponse {

    private static final Logger log = LogManager.getLogger(SearchResponseImpl.class);

    protected static final String SELECT_DOCUMENTS_IN = "SELECT * FROM Document WHERE ecm:uuid IN ('%s')";

    protected final List<SearchHit> hits;

    protected final List<SearchLimitation> limitations;

    protected final long total;

    protected final boolean totalAccurate;

    @Nullable
    protected final SearchScrollContext scrollContext;

    protected final List<Aggregate<? extends Bucket>> aggregates;

    protected SearchResponseImpl(Builder builder) {
        this.hits = builder.hits;
        this.limitations = builder.limitations;
        this.total = builder.total;
        this.totalAccurate = builder.totalAccurate;
        this.scrollContext = builder.scrollContext;
        this.aggregates = builder.aggregates;
    }

    @Override
    public List<SearchLimitation> getLimitations() {
        return limitations;
    }

    @Override
    public List<SearchHit> getHits() {
        return hits;
    }

    @Override
    public long getHitsCount() {
        return getHits().size();
    }

    @Override
    public PartialList<Map<String, Serializable>> getHitsAsMap() {
        return new PartialList<>(getHits().stream().map(SearchHit::getFields).toList(), getTotal());
    }

    @Override
    public IterableQueryResult getHitsAsIterator() {
        return new IterableQueryResultImpl(this);
    }

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public boolean isTotalAccurate() {
        return totalAccurate;
    }

    @Override
    @Nullable
    public SearchScrollContext getScrollContext() {
        return scrollContext;
    }

    @Override
    public List<Aggregate<? extends Bucket>> getAggregates() {
        return aggregates;
    }

    @Override
    public DocumentModelList loadDocuments(CoreSession defaultSession) {
        if (getHitsCount() == 0) {
            DocumentModelListImpl ret = new DocumentModelListImpl(0);
            ret.setTotalSize(getTotal());
            return ret;
        }
        var repositories = hits.stream().map(SearchHit::getRepository).collect(Collectors.toSet());
        Map<String, DocumentModelListImpl> repoDocs = new HashMap<>(repositories.size());
        for (String repository : repositories) {
            CoreSession session = defaultSession.getRepositoryName().equals(repository) ? defaultSession
                    : CoreInstance.getCoreSession(repository, defaultSession.getPrincipal());
            var docs = (DocumentModelListImpl) loadDocumentsUsingSession(session);
            if (!docs.isEmpty()) {
                repoDocs.put(repository, docs);
            }
        }
        DocumentModelListImpl ret;
        if (repoDocs.isEmpty()) {
            ret = new DocumentModelListImpl(0);
        } else if (repoDocs.size() == 1) {
            ret = repoDocs.values().stream().findFirst().get();
        } else {
            int size = repoDocs.values().stream().mapToInt(DocumentModelList::size).sum();
            ret = new DocumentModelListImpl(size);
            repoDocs.values().forEach(ret::addAll);
        }
        ret.setTotalSize(getTotal());
        List<String> documentIds = hits.stream().map(SearchHit::getDocId).toList();
        ret.sort(Comparator.comparingInt(doc -> documentIds.indexOf(doc.getId())));
        // Attach highlights
        for (var hit : hits) {
            if (hit.getDocId() == null || hit.getHighlights().isEmpty()) {
                continue;
            }
            ret.stream()
               .filter(d -> hit.getDocId().equals(d.getId()))
               .findFirst()
               .ifPresent(doc -> doc.putContextData(HIGHLIGHT_CTX_DATA, (Serializable) hit.getHighlights()));
        }
        return ret;
    }

    protected DocumentModelList loadDocumentsUsingSession(CoreSession session) {
        String repository = session.getRepositoryName();
        List<String> documentIds = hits.stream()
                                       .filter(hit -> repository.equals(hit.getRepository()))
                                       .map(SearchHit::getDocId)
                                       .toList();
        if (documentIds.isEmpty()) {
            return new DocumentModelListImpl(0);
        }
        DocumentModelList docs;
        try {
            docs = session.query(String.format(SELECT_DOCUMENTS_IN, String.join("', '", documentIds)));
        } catch (DocumentNotFoundException | PropertyConversionException | IllegalArgumentException e) {
            // A corrupted document prevents to load the batch of docs
            log.warn("Fail to load documents because of: {}, retrying one by one", e.getMessage());
            docs = loadDocumentsOneByOne(session, documentIds);
        }
        ((DocumentModelListImpl) docs).setTotalSize(getTotal());
        if (docs.size() < documentIds.size()) {
            List<String> notFound = new ArrayList<>(documentIds);
            docs.forEach(doc -> notFound.remove(doc.getId()));
            // some documents might have been deleted or search index is desynchronized or documents are corrupted
            log.warn("Fail to load {} documents out of {}: {}", notFound.size(), documentIds.size(), notFound);
        }
        return docs;
    }

    protected DocumentModelList loadDocumentsOneByOne(CoreSession session, List<String> documentIds) {
        var ret = new DocumentModelListImpl(documentIds.size());
        for (String documentId : documentIds) {
            try {
                ret.add(session.getDocument(new IdRef(documentId)));
            } catch (DocumentNotFoundException | PropertyConversionException | IllegalArgumentException e) {
                log.atError()
                   .withThrowable(log.isDebugEnabled() ? e : null)
                   .log("Skipping corrupted doc: {}, because of: {}", documentId, e.getMessage());
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class Builder {

        protected final List<SearchHit> hits;

        protected List<SearchLimitation> limitations = List.of();

        protected long total = -1;

        protected boolean totalAccurate;

        protected List<Aggregate<? extends Bucket>> aggregates = List.of();

        protected SearchScrollContext scrollContext;

        public Builder(List<SearchHit> hits) {
            this.hits = Collections.unmodifiableList(hits);
        }

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public Builder totalAccurate(boolean totalAccurate) {
            this.totalAccurate = totalAccurate;
            return this;
        }

        public Builder scroll(SearchScrollContext scrollContext) {
            this.scrollContext = scrollContext;
            return this;
        }

        public Builder aggregates(List<Aggregate<? extends Bucket>> aggregates) {
            this.aggregates = Collections.unmodifiableList(aggregates);
            return this;
        }

        /**
         * @apiNote This method will set {@code limitations}, in case both {@link #missingCapabilities(List)} and
         *          {@link #limitations(List)} are called, only the latter will be taken into account.
         * @deprecated since 2025.17, use {@link #limitations(List)} instead
         */
        @Deprecated(since = "2025.17", forRemoval = true)
        public Builder missingCapabilities(List<SearchClient.Capability> missingCapabilities) {
            this.limitations = missingCapabilities.stream()
                                                  .map(capability -> SearchLimitation.of(LimitationKind.UNSUPPORTED,
                                                          capability, "Client does not support " + capability))
                                                  .toList();
            return this;
        }

        /**
         * Sets structured limitations. When non-empty, {@link #missingCapabilities(List)} is ignored and missing
         * capabilities are derived from limitations.
         *
         * @since 2025.17
         */
        public Builder limitations(List<SearchLimitation> limitations) {
            this.limitations = Collections.unmodifiableList(limitations);
            return this;
        }

        public SearchResponse build() {
            return new SearchResponseImpl(this);
        }
    }

    public record Error(int code, String message) {

    }
}

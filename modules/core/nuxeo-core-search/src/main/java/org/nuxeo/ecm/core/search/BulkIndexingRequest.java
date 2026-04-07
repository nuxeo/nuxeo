/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.utils.Timestamp;

/**
 * @since 2025.0
 */
public class BulkIndexingRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 20240617L;

    protected final SearchIndex index;

    protected final boolean refresh;

    protected final long version;

    protected final List<IndexingRequest> requests;

    protected BulkIndexingRequest(SearchIndex index, Builder builder) {
        this.index = index;
        this.refresh = builder.refresh;
        this.version = builder.version;
        this.requests = Collections.unmodifiableList(builder.requests);
    }

    public SearchIndex getSearchIndex() {
        return index;
    }

    public long getVersion() {
        return version;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public List<IndexingRequest> getRequests() {
        return requests;
    }

    public boolean isEmpty() {
        return requests.isEmpty();
    }

    public int size() {
        return requests.size();
    }

    public int sizeDelete() {
        return (int) requests.stream().filter(IndexingRequest::isDelete).count();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Builder buildRequest(boolean refresh) {
        return new Builder(refresh);
    }

    public static class Builder {

        protected final boolean refresh;

        protected final long version;

        protected final List<IndexingRequest> requests;

        protected Builder(boolean refresh) {
            this.refresh = refresh;
            this.version = Timestamp.currentTimeMicros();
            this.requests = new ArrayList<>();
        }

        public Builder add(IndexingRequest request) {
            if (request == null) {
                throw new IllegalArgumentException("Null indexing request provided");
            }
            requests.add(request);
            return this;
        }

        public int size() {
            return requests.size();
        }

        public BulkIndexingRequest build(SearchIndex index) {
            if (index == null) {
                throw new IllegalArgumentException("The search index is required");
            }
            return new BulkIndexingRequest(index, this);
        }
    }
}

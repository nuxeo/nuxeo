/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.query.scroll;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * @since 2025.18
 */
public abstract class AbstractQueryBuilderScroll implements Scroll {

    protected QueryBuilder queryBuilder;

    protected int size;

    protected Duration timeout;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        if (!(request instanceof QueryBuilderScrollRequest scrollRequest)) {
            throw new IllegalArgumentException(
                    "Requires a QueryBuilderScrollRequest, got a " + request.getClass().getCanonicalName());
        }
        queryBuilder = scrollRequest.getQueryBuilder();
        size = scrollRequest.getSize();
        timeout = scrollRequest.getTimeout();
    }

    public static abstract class Query extends AbstractQueryBuilderScroll {

        protected boolean hasNext;

        @Override
        public void init(ScrollRequest request, Map<String, String> options) {
            super.init(request, options);
            // override offset and limit before starting the scroll, add 1 to have visibility on the end of the scroll
            queryBuilder.offset(0L).limit(size + 1);
            hasNext = true;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public List<String> next() {
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            var entries = queryIds(queryBuilder);
            // prepare result and next call to next
            queryBuilder.offset(queryBuilder.offset() + size);
            if (entries.size() <= size) {
                hasNext = false;
                return entries;
            } else {
                hasNext = true;
                return entries.subList(0, size);
            }
        }

        protected abstract List<String> queryIds(QueryBuilder queryBuilder);
    }

    public static abstract class Scroll extends AbstractQueryBuilderScroll {

        protected ScrollResult<String> entries;

        protected String scrollId;

        protected boolean end;

        @Override
        public boolean hasNext() {
            fetchNextScrollIfNeeded();
            return !end;
        }

        @Override
        public List<String> next() {
            fetchNextScrollIfNeeded();
            if (end) {
                throw new NoSuchElementException();
            }
            var results = entries.getResults();
            // consume the current batch so that the next call will advance the scroll
            entries = null;
            return results;
        }

        protected void fetchNextScrollIfNeeded() {
            // skip if already exhausted (end) or if a buffered batch is waiting to be consumed (entries != null)
            if (!end && entries == null) {
                // first call: start the scroll session; subsequent calls: continue with the existing scroll id
                if (scrollId == null) {
                    entries = scrollIds(queryBuilder, size, timeout);
                } else {
                    entries = scrollIds(scrollId);
                }
                scrollId = entries.getScrollId();
                end = !entries.hasResults();
            }
        }

        @Override
        public void close() {
            if (scrollId != null) {
                clearScroll(scrollId);
            }
            entries = null;
            scrollId = null;
            end = true;
        }

        protected abstract ScrollResult<String> scrollIds(QueryBuilder queryBuilder, int batchSize, Duration keepAlive);

        protected abstract ScrollResult<String> scrollIds(String scrollId);

        protected abstract void clearScroll(String scrollId);
    }
}

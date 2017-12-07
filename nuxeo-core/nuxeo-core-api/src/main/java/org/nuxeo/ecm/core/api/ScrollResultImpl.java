/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api;

import java.util.Collections;
import java.util.List;

/**
 * @since 8.4
 */
public class ScrollResultImpl<T> implements ScrollResult {

    protected static final ScrollResult EMPTY_RESULT = new ScrollResultImpl("empty", Collections.emptyList());

    protected final String scrollId;

    protected final List<String> ids;

    protected final List<T> results;

    public static ScrollResult emptyResult() {
        return EMPTY_RESULT;
    }

    public ScrollResultImpl(String scrollId, List<String> ids) {
        this(scrollId, ids, Collections.emptyList());
    }

    /**
     * @since 9.3
     */
    public ScrollResultImpl(String scrollId, List<String> ids, List<T> results) {
        this.scrollId = scrollId;
        this.ids = Collections.unmodifiableList(ids);
        this.results = Collections.unmodifiableList(results);
    }

    @Override
    public String getScrollId() {
        return scrollId;
    }

    @Override
    public List<String> getResultIds() {
        return ids;
    }

    @Override
    public boolean hasResults() {
        return ids != null && !ids.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getResults() {
        return results;
    }

}

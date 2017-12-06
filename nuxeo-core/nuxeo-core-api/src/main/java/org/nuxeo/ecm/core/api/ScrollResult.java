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

import java.util.List;

/**
 * The result of a {@link CoreSession#scroll} call, giving access to result and the scroll id.
 *
 * @param <T> the result type.
 * @since 8.4
 */
public interface ScrollResult<T> {

    /**
     * Returns the scroll identifier, which can be passed to CoreSession.scroll(String scrollId) to get more results.
     */
    String getScrollId();

    /**
     * Returns the list of document ids
     *
     * @deprecated since 9.10 use {@link #getResults()} instead.
     */
    @Deprecated
    default List<String> getResultIds() {
        throw new UnsupportedOperationException("Use getResults instead.");
    }

    /**
     * Returns {@code true} when this {@code ScrollResult} contains results.
     */
    boolean hasResults();

    /**
     * Returns the list of results
     *
     * @since 9.3
     */
    List<T> getResults();

}

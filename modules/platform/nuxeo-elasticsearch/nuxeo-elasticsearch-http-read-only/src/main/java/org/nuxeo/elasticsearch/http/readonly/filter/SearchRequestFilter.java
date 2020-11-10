/*
 * (C) Copyright 2015-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.elasticsearch.http.readonly.filter;

import org.json.JSONException;
import org.nuxeo.ecm.core.api.CoreSession;

public interface SearchRequestFilter {

    String getPayload() throws JSONException;

    /**
     * @deprecated since 11.4, types have been removed since Elasticsearch 7.x, use
     *             {@link #init(CoreSession, String, String, String)} instead
     */
    @Deprecated(since = "11.4", forRemoval = true)
    default void init(CoreSession session, String indices, String types, String rawQuery, String payload) {
        init(session, indices, rawQuery, payload);
    }

    /**
     * @since 11.4
     */
    void init(CoreSession session, String indices, String rawQuery, String payload);

    String getUrl();

    String getIndices();

    /**
     * @deprecated since 11.4, types have been removed since Elasticsearch 7.x
     */
    @Deprecated(since = "11.4", forRemoval = true)
    String getTypes();

}

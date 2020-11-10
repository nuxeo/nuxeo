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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.http.readonly;

import javax.validation.constraints.NotNull;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Rewrite a Document get request to add security.
 *
 * @since 7.3
 */
public class DocRequestFilter {
    private final NuxeoPrincipal principal;

    private final String indices;

    /** @deprecated since 11.4, types have been removed since Elasticsearch 7.x */
    @Deprecated(since = "11.4", forRemoval = true)
    private final String types = null;

    private final String documentId;

    private final String rawQuery;

    public DocRequestFilter(NuxeoPrincipal principal, String indices, String documentId, String rawQuery) {
        this.principal = principal;
        this.indices = indices;
        this.documentId = documentId;
        this.rawQuery = rawQuery;
    }

    /**
     * @deprecated since 11.4, types have been removed since Elasticsearch 7.x, use
     *             {@link #DocRequestFilter(NuxeoPrincipal, String, String, String)} instead
     */
    @Deprecated(since = "11.4", forRemoval = true)
    public DocRequestFilter(NuxeoPrincipal principal, String indices, String types, String documentId,
            String rawQuery) {
        this(principal, indices, documentId, rawQuery);
    }

    protected @NotNull String getUrl() {
        String url = "/" + indices + "/" + documentId;
        if (rawQuery != null) {
            url += "?" + rawQuery;
        }
        return url;
    }

    @Override
    public String toString() {
        return "Get Doc url: " + getUrl() + " user: " + principal;
    }

    public String getCheckAccessUrl() {
        return "/" + indices + "/" + documentId + "?fields=ecm:acl";
    }
}

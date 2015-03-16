package org.nuxeo.elasticsearch.http.readonly;
/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */

import javax.validation.constraints.NotNull;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @since 7.3
 */
public class DocRequestFilter {
    private final NuxeoPrincipal principal;

    private final String indices;

    private final String types;

    private final String documentId;

    private final String rawQuery;

    public DocRequestFilter(NuxeoPrincipal principal, String indices, String types, String documentId,
                            String rawQuery) {
        this.principal = principal;
        this.indices = indices;
        this.types = types;
        this.documentId = documentId;
        this.rawQuery = rawQuery;
    }

    protected @NotNull String getUrl() {
        String url = "/" + indices + "/" + types + "/" + documentId;
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
        String url = "/" + indices + "/" + types + "/" + documentId + "?fields=ecm:acl";
        return url;
    }
}

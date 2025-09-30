/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.api;

import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * @since 2025.9
 */
public class DirectoryQueryBuilder extends QueryBuilder {

    protected boolean fetchReferences;

    public DirectoryQueryBuilder() {
        super();
        fetchReferences = false;
    }

    public DirectoryQueryBuilder(QueryBuilder other) {
        super(other);
        fetchReferences = other instanceof DirectoryQueryBuilder dirOther && dirOther.fetchReferences();
    }

    public boolean fetchReferences() {
        return fetchReferences;
    }

    public DirectoryQueryBuilder fetchReferences(boolean fetchReferences) {
        this.fetchReferences = fetchReferences;
        return this;
    }
}

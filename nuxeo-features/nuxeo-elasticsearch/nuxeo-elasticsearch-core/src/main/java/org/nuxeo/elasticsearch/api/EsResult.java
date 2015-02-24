/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.elasticsearch.api;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.platform.query.api.Aggregate;

/**
 * @since 6.0
 */
public class EsResult {
    private final DocumentModelList documents;

    private final IterableQueryResult rows;

    private final List<Aggregate> aggregates;

    public EsResult(DocumentModelList documents, List<Aggregate> aggregates) {
        this.documents = documents;
        this.rows = null;
        this.aggregates = aggregates;
    }

    public EsResult(IterableQueryResult rows, List<Aggregate> aggregates) {
        this.documents = null;
        this.rows = rows;
        this.aggregates = aggregates;
    }

    /**
     * Get the list of Nuxeo documents, this is populated when using a SELECT * clause, or when submitting esQuery.
     */
    public DocumentModelList getDocuments() {
        return documents;
    }

    /**
     * Iterator to use when selecting fields: SELECT ecm:uuid ...
     *
     * @since 7.2
     */
    public IterableQueryResult getRows() {
        return rows;
    }

    public List<Aggregate> getAggregates() {
        return aggregates;
    }

}

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
import org.nuxeo.ecm.platform.query.api.Aggregate;

/**
 * @since 5.9.6
 */
public class EsResult {
    private final DocumentModelList documents;
    private final List<Aggregate> aggregates;

    public EsResult(DocumentModelList documents, List<Aggregate> aggregates) {
        this.documents = documents;
        this.aggregates = aggregates;
    }

    public DocumentModelList getDocuments() {
        return documents;
    }

    public List<Aggregate> getAggregates() {
        return aggregates;
    }
}

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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.aggregate;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_DATE_HISTOGRAM;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_DATE_RANGE;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_HISTOGRAM;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_RANGE;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_SIGNIFICANT_TERMS;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TYPE_TERMS;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * @since 5.9.6
 */
final public class AggregateFactory {

    private AggregateFactory() {
    }

    public static AggregateEsBase<? extends Bucket> create(AggregateDefinition def,
            DocumentModel searchDocumentModel) {
        switch (def.getType()) {
        case AGG_TYPE_TERMS:
            return new TermAggregate(def, searchDocumentModel);
        case AGG_TYPE_RANGE:
            return new RangeAggregate(def, searchDocumentModel);
        case AGG_TYPE_DATE_HISTOGRAM:
            return new DateHistogramAggregate(def, searchDocumentModel);
        case AGG_TYPE_SIGNIFICANT_TERMS:
            return new SignificantTermAggregate(def, searchDocumentModel);
        case AGG_TYPE_HISTOGRAM:
            return new HistogramAggregate(def, searchDocumentModel);
        case AGG_TYPE_DATE_RANGE:
            return new DateRangeAggregate(def, searchDocumentModel);
        default:
            throw new IllegalArgumentException("Unknown aggregate type: "
                    + def.getType());
        }

    }
}

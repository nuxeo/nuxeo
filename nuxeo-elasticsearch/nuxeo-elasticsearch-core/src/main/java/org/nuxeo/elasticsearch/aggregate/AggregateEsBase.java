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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.FULLTEXT_FIELD;

import java.util.Collection;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateBase;

/**
 * @since 6.0
 */
public abstract class AggregateEsBase<B extends Bucket> extends
        AggregateBase<B> {

    public AggregateEsBase(AggregateDefinition definition,
            DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    /**
     * Return the Elasticsearch aggregate builder
     */
    public abstract AggregationBuilder getEsAggregate();

    /**
     * Return the Elasticsearch aggregate filter corresponding to the selection
     */
    public abstract FilterBuilder getEsFilter();

    /**
     * Extract the buckets from the Elasticsearch response
     */
    public abstract void parseEsBuckets(
            Collection<? extends MultiBucketsAggregation.Bucket> buckets);

    @Override
    public String getField() {
        String ret = super.getField();
        if (NXQL.ECM_FULLTEXT.equals(ret)) {
            ret = FULLTEXT_FIELD;
        }
        return ret;
    }

    /**
     * Convert embedded Elasticsearch DateTime to joda DateTime
     */
    protected DateTime getDateTime(
            org.elasticsearch.common.joda.time.DateTime date) {
        if (date == null) {
            return null;
        }
        return new DateTime(date.getMillis());
    }

}

/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.query.core;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateQuery;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * @since 5.9.6
 */
public class AggregateImpl implements Aggregate {

    protected final AggregateQuery query;

    protected final List<? extends Bucket> buckets;

    public AggregateImpl(AggregateQuery query, List<? extends Bucket> buckets) {
        if (query == null) {
            throw new IllegalArgumentException("query is null");
        }
        if (buckets == null) {
            throw new IllegalArgumentException("buckets is null");
        }
        this.query = query;
        this.buckets = buckets;
    }

    @Override
    public String getId() {
        return query.getId();
    }

    @Override
    public String getType() {
        return query.getType();
    }

    @Override
    public List<? extends Bucket> getBuckets() {
        return buckets;
    }

    @Override
    public AggregateQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return String.format("AggregateImpl(%s, %s, %s)", getId(), getType(),
                Arrays.toString(buckets.toArray()));
    }
}

/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.api;

/**
 * @since 5.9.6
 */
public class Aggregate {

    protected final AggregateQuery query;

    protected final Bucket[] buckets;

    public Aggregate(AggregateQuery query, Bucket[] buckets) {
        assert (query != null);
        assert (buckets != null);
        this.query = query;
        this.buckets = buckets;
    }

    public String getId() {
        return query.getId();
    }

    public String getType() {
        return query.getType();
    }

    public Bucket[] getBuckets() {
        return buckets;
    }

    public AggregateQuery getQuery() {
        return query;
    }

}

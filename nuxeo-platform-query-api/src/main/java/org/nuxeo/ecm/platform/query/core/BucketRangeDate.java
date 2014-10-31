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

import org.joda.time.DateTime;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * Immutable bucket for date range.
 * @since 6.0
 */
public class BucketRangeDate implements Bucket {

    private final BucketRange range;
    // joda DateTime are immutables
    private final DateTime fromDate;
    private final DateTime toDate;

    public BucketRangeDate(String key, DateTime from, DateTime to, long docCount) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        };
        //fromDate.
        range = new BucketRange(key, from != null ? from.getMillis() : null, to != null ? to.getMillis() : null, docCount);
        this.fromDate = from;
        this.toDate = to;
    }

    @Override
    public String getKey() {
        return range.getKey();
    }

    @Override
    public long getDocCount() {
        return range.getDocCount();
    }

    public Double getFrom() {
        return range.getFrom();
    }
    /**
     * @return null if there are no minimal limit
     */
    public DateTime getFromAsDate() {
        return fromDate;
    }

    public Double getTo() {
        return range.getTo();
    }

    /**
     * @return null if there are no maximal limit
     */
    public DateTime getToAsDate() {
        return toDate;
    }

    @Override
    public String toString() {
            return String.format("BucketRangeDate(%s, %d, %s, %s)", getKey(), getDocCount(), fromDate,
                    toDate);
    }
}

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

import org.nuxeo.ecm.platform.query.api.Bucket;

import java.util.Locale;

/**
 * Immutable bucket for range.
 * @since 6.0
 */
public final class BucketRange implements Bucket {

    private final String key;
    private final long docCount;
    private final Double from;
    private final Double to;

    public BucketRange(String key, Number from, Number to, long docCount) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        };
        this.key = key;
        if (from != null) {
            this.from = from.doubleValue();
        } else {
            this.from = null;
        }
        if (to != null) {
            this.to = to.doubleValue();
        } else {
            this.to = null;
        }
        this.docCount = docCount;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public long getDocCount() {
        return docCount;
    }

    /**
     * @return null if there are no minimal limit
     */
    public Double getFrom() {
        return from;
    }

    /**
     * @return null if there are no max limit
     */
    public Double getTo() {
        return to;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "BucketRange(%s, %d, %.2f, %.2f)", key, docCount, from,
                        to);
    }
}

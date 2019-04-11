/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.ecm.platform.query.api.Bucket;

import java.util.Locale;

/**
 * Immutable bucket for range.
 *
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
        }
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
        return String.format(Locale.ENGLISH, "BucketRange(%s, %d, %.2f, %.2f)", key, docCount, from, to);
    }

}

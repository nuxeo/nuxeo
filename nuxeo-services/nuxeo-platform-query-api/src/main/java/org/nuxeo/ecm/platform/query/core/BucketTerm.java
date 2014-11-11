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

/**
 * Immutable Bucket for terms.
 * @since 5.9.6
 */
public final class BucketTerm implements Bucket {

    private final String key;
    private final long docCount;

    public BucketTerm(String key, long docCount) {
         if (key == null) {
            throw new IllegalArgumentException("key is null");
        };
        this.key = key;
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

    @Override
    public String toString() {
        return String.format("BucketTerm(%s, %d)", key, docCount);
    }
}

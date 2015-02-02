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
package org.nuxeo.ecm.platform.query.api;

import java.util.List;
import java.util.Map;

/**
 * @since 6.0
 */
public interface Aggregate<B extends Bucket> {
    /**
     * The aggregate identifier.
     */
    String getId();

    /**
     * Type of aggregation.
     */
    String getType();

    /**
     * Nuxeo field to aggregate.
     */
    String getField();

    /**
     * Properties of the aggregate.
     */
    Map<String, String> getProperties();

    /**
     * Range definition for aggregate of type range.
     */
    List<AggregateRangeDefinition> getRanges();

    /**
     * Date Range definition for aggregate of type date range.
     */
    List<AggregateRangeDateDefinition> getDateRanges();

    /**
     * The selection filter that is going to be applied to the main query as a post filter.
     */
    List<String> getSelection();

    void setSelection(List<String> selection);

    /**
     * The aggregate results.
     */
    List<B> getBuckets();

    void setBuckets(List<B> buckets);

    Bucket getBucket(final String key);

    boolean hasBucket(final String key);

    void resetSelection();

    /**
     * The regular list of buckets plus buckets with doc count at 0 for selected buckets which are not returned from es
     * post filtering.
     */
    List<Bucket> getExtendedBuckets();
}

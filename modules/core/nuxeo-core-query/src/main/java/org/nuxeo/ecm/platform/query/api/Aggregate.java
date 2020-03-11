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

    /**
     * Gets the field name as defined in Nuxeo.
     * <p>
     * In Nuxeo the separator for a complex type is the `/` character, in a case where our {@link Aggregate} implementation defines a field
     * as `file:content.mime-type`, this method should return `file:content/mime-type`.
     *
     * @since 11.1
     */
    String getXPathField();
}

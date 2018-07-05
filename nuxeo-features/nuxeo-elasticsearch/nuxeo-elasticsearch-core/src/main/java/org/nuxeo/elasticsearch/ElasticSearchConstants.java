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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch;

public final class ElasticSearchConstants {

    public static final String AGG_INCLUDE_PROP = "include";

    public static final String AGG_SIZE_PROP = "size";

    public static final String AGG_MIN_DOC_COUNT_PROP = "minDocCount";

    public static final String AGG_EXCLUDE_PROP = "exclude";

    public static final String AGG_ORDER_PROP = "order";

    public static final String AGG_INTERVAL_PROP = "interval";

    public static final String AGG_EXTENDED_BOUND_MAX_PROP = "extendedBoundsMax";

    public static final String AGG_EXTENDED_BOUND_MIN_PROP = "extendedBoundsMin";

    public static final String AGG_FORMAT_PROP = "format";

    public static final String AGG_TIME_ZONE_PROP = "timeZone";

    public static final String AGG_PRE_ZONE_PROP = "preZone";

    public static final String AGG_ORDER_COUNT_DESC = "count desc";

    public static final String AGG_ORDER_COUNT_ASC = "count asc";

    public static final String AGG_ORDER_TERM_DESC = "term desc";

    public static final String AGG_ORDER_TERM_ASC = "term asc";

    public static final String AGG_ORDER_KEY_DESC = "key desc";

    public static final String AGG_ORDER_KEY_ASC = "key asc";

    public static final String AGG_TYPE_TERMS = "terms";

    public static final String AGG_TYPE_SIGNIFICANT_TERMS = "significant_terms";

    public static final String AGG_TYPE_RANGE = "range";

    public static final String AGG_TYPE_DATE_RANGE = "date_range";

    public static final String AGG_TYPE_HISTOGRAM = "histogram";

    public static final String AGG_TYPE_DATE_HISTOGRAM = "date_histogram";

    public static final String ID_FIELD = "_id";

    public static final String FULLTEXT_FIELD = "all_field";

    /**
     * Elasticsearch type name used to index Nuxeo documents
     */
    public static final String DOC_TYPE = "doc";

    /**
     * Elasticsearch type name used to index Nuxeo audit entries
     */
    public static final String ENTRY_TYPE = "entry";

    /**
     * Elasticsearch type name used for the UID sequencer index
     */
    public static final String SEQ_ID_TYPE = "seqId";

    public static final String ACL_FIELD = "ecm:acl";

    public static final String PATH_FIELD = "ecm:path";

    public static final String CHILDREN_FIELD = "ecm:path.children";

    public static final String BINARYTEXT_FIELD = "ecm:binarytext";

    public static final String ALL_FIELDS = "*";

    public static final String ES_ENABLED_PROPERTY = "elasticsearch.enabled";

    public static final String FETCH_DOC_FROM_ES_PROPERTY = "elasticsearch.fetchDocFromEs";

    public static final String REINDEX_BUCKET_READ_PROPERTY = "elasticsearch.reindex.bucketReadSize";

    public static final String REINDEX_BUCKET_WRITE_PROPERTY = "elasticsearch.reindex.bucketWriteSize";

    public static final String REINDEX_ON_STARTUP_PROPERTY = "elasticsearch.reindex.onStartup";

    public static final String INDEX_BULK_MAX_SIZE_PROPERTY = "elasticsearch.index.bulkMaxSize";

    public static final String DISABLE_AUTO_INDEXING = "disableAutoIndexing";

    public static final String ES_SYNC_INDEXING_FLAG = "ESSyncIndexing";

    public static final String REINDEX_USING_CHILDREN_TRAVERSAL_PROPERTY = "elasticsearch.reindex.useChildrenTraversal";

    /** @since 7.4 */
    public static final String INDEXING_QUEUE_ID = "elasticSearchIndexing";

    public static final String EPOCH_MILLIS_FORMAT = "epoch_millis";

    /** @since 10.2 */
    public static final String ES_SCORE_FIELD = "_score";

    private ElasticSearchConstants() {
    }

}

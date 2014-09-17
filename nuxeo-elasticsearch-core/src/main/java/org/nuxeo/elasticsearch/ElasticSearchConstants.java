/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch;

public class ElasticSearchConstants {
    private ElasticSearchConstants() {
    }

    public static final String AGG_INCLUDE_PROP = "include";
    public static final String AGG_SIZE_PROP = "size";
    public static final String AGG_MIN_DOC_COUNT_PROP = "minDocCount";
    public static final String AGG_EXCLUDE_PROP = "exclude";
    public static final String AGG_ORDER_PROP = "order";
    public static final String AGG_ORDER_COUNT_DESC = "count desc";
    public static final String AGG_ORDER_COUNT_ASC = "count asc";
    public static final String AGG_ORDER_TERM_DESC = "term desc";
    public static final String AGG_ORDER_TERM_ASC = "term asc";

    public static final String AGG_TYPE_TERMS = "terms";
    public static final String AGG_TYPE_SIGNIFICANT_TERMS = "significant_terms";
    public static final String AGG_TYPE_RANGE = "range";
    public static final String AGG_TYPE_DATE_RANGE = "date_range";

    public static final String ID_FIELD = "_id";
    /**
     * Elasticsearch type name used to index Nuxeo documents
     */
    public static final String DOC_TYPE = "doc";

    public static final String ACL_FIELD = "ecm:acl";
    public static final String PATH_FIELD = "ecm:path";
    public static final String CHILDREN_FIELD = "ecm:path.children";
    public static final String BINARYTEXT_FIELD = "ecm:binarytext";
    public static final String ALL_FIELDS = "*";

    public static final String FETCH_DOC_FROM_ES_PROPERTY = "elasticsearch.fetchDocFromEs";
}


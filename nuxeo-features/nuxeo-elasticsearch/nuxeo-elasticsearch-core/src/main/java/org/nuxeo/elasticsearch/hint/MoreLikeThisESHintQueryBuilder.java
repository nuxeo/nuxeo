/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.elasticsearch.hint;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;

import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

/**
 * The implementation of {@link ESHintQueryBuilder} for the <strong>"more_like_this"</strong> Elasticsearch hint
 * operator.
 *
 * @since 11.1
 */
public class MoreLikeThisESHintQueryBuilder implements ESHintQueryBuilder {

    public static final int MORE_LIKE_THIS_MIN_TERM_FREQ = 1;

    public static final int MORE_LIKE_THIS_MIN_DOC_FREQ = 3;

    public static final int MORE_LIKE_THIS_MAX_QUERY_TERMS = 12;

    /**
     * {@inheritDoc}
     * <p>
     * 
     * @return {@link org.elasticsearch.index.query.MoreLikeThisQueryBuilder}
     */
    @Override
    public QueryBuilder make(EsHint hint, String fieldName, Object value) {
        String[] indexFields = hint.getIndexFieldNames();
        String[] fields = indexFields.length > 0 ? indexFields : new String[] { fieldName };
        return QueryBuilders.moreLikeThisQuery(fields, null, getItems(value))
                            .analyzer(hint.analyzer)
                            .minDocFreq(MORE_LIKE_THIS_MIN_DOC_FREQ)
                            .minTermFreq(MORE_LIKE_THIS_MIN_TERM_FREQ)
                            .maxQueryTerms(MORE_LIKE_THIS_MAX_QUERY_TERMS);
    }

    /**
     * Build a single or an array of {@link MoreLikeThisQueryBuilder.Item} according to the value type. Where each
     * {@link MoreLikeThisQueryBuilder.Item} represent a document request
     *
     * @param value represent what we are looking for. Can be <code>String</code> or an array of <code>String</code>
     * @return the items / document requests
     */
    public static MoreLikeThisQueryBuilder.Item[] getItems(Object value) {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        String repo = rm.getDefaultRepository().getName();
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        String esIndex = esa.getIndexNameForRepository(repo);
        Object[] values;
        if (value instanceof Object[]) {
            values = (Object[]) value;
        } else {
            values = new Object[] { value };
        }
        MoreLikeThisQueryBuilder.Item[] ret = new MoreLikeThisQueryBuilder.Item[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new MoreLikeThisQueryBuilder.Item(esIndex, DOC_TYPE, (String) values[i]);
        }
        return ret;
    }
}

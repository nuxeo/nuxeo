/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.elasticsearch.seqgen;

import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.uidgen.AbstractUIDSequencer;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

/**
 * Elasticsearch implementation of {@link UIDSequencer}.
 * <p>
 * Since elasticsearch does not seem to support a notion of native sequence, the implementation uses the auto-increment
 * of the version attribute as described in the <a href=
 * "http://blogs.perl.org/users/clinton_gormley/2011/10/elasticsearchsequence---a-blazing-fast-ticket-server.html"
 * >ElasticSearch::Sequence - a blazing fast ticket server</a> blog post.
 *
 * @since 7.3
 */
public class ESUIDSequencer extends AbstractUIDSequencer {

    private static final Log log = LogFactory.getLog(ESUIDSequencer.class);

    protected ESClient esClient = null;

    protected String indexName;

    @Override
    public void init() {
        if (esClient != null) {
            return;
        }
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        if (!esa.isElasticsearchEnabled()) {
            log.info("Elasticsearch service is disabled");
            return;
        }
        esClient = esa.getClient();
        indexName = esa.getIndexNameForType(ElasticSearchConstants.SEQ_ID_TYPE);
        try {
            boolean indexExists = esClient.indexExists(indexName);
            if (!indexExists) {
                throw new NuxeoException(
                        String.format("Sequencer %s needs an elasticSearchIndex contribution with type %s", getName(),
                                ElasticSearchConstants.SEQ_ID_TYPE));
            }
        } catch (NoSuchElementException | NuxeoException e) {
            dispose();
            throw e;
        }
    }

    @Override
    public void dispose() {
        if (esClient == null) {
            return;
        }
        esClient = null;
        indexName = null;
    }

    @Override
    public void initSequence(String key, long id) {
        String source = "{ \"ts\" : " + System.currentTimeMillis() + "}";
        IndexResponse res = esClient.index(
                new IndexRequest(indexName, ElasticSearchConstants.SEQ_ID_TYPE, key).versionType(VersionType.EXTERNAL)
                        .version(id)
                        .source(source, XContentType.JSON));
    }

    @Override
    public long getNextLong(String sequenceName) {
        String source = "{ \"ts\" : " + System.currentTimeMillis() + "}";
        IndexResponse res = esClient.index(
                new IndexRequest(indexName, ElasticSearchConstants.SEQ_ID_TYPE, sequenceName)
                        .source(source, XContentType.JSON));
        return res.getVersion();
    }

}

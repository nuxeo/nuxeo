/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.elasticsearch.seqgen;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
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
public class ESUIDSequencer implements UIDSequencer {

    public static final String SEQUENCER_CONTRIB = "esSequencer";

    public static final String IDX_NAME = "uidgen";

    public static final String IDX_TYPE = "seqId";

    protected Client esClient = null;

    protected Client getClient() {
        if (esClient == null) {
            ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
            esClient = esa.getClient();
        }
        return esClient;
    }

    @Override
    public void dispose() {
        if (esClient != null) {
            esClient.close();
        }
    }

    @Override
    public int getNext(String sequenceName) {
        String source = "{ \"ts\" : " + System.currentTimeMillis() + "}";
        IndexResponse res = getClient().prepareIndex(IDX_NAME, IDX_TYPE, sequenceName).setSource(source).execute().actionGet();
        return (int) res.getVersion();
    }

    @Override
    public void init() {
        getClient();
    }

}

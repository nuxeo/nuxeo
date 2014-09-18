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
 *     Tiry
 * 
 */
package org.nuxeo.elasticsearch.seqgen;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implements the {@link SequenceGenerator} service on top of Elasticsearch.
 * 
 * Since elasticsearch does not seem to support a notion of native sequence, the
 * implementation uses the auto-increment of the version attribute as described
 * in
 * 
 * http://blogs.perl.org/users/clinton_gormley/2011/10/elasticsearchsequence--
 * -a-blazing-fast-ticket-server.html
 * 
 * @author tiry
 * 
 */
public class ESSequenceGeneratorComponent extends DefaultComponent implements
        SequenceGenerator {

    public static final String IDX_NAME = "sequence";

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
    public long getNextId(String sequenceName) {
        String source = "{ \"ts\" : " + System.currentTimeMillis() + "}";
        IndexResponse res = getClient().prepareIndex(IDX_NAME, IDX_TYPE,
                sequenceName).setSource(source).execute().actionGet();
        return res.getVersion();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (esClient != null) {
            esClient.close();
        }
    }

}

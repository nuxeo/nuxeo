/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Removes documents which are proxies and whose id is contained in the given ids list.
 *
 * @since 10.3
 */
public class RemoveProxyAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "removeProxy";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String QUERY_TEMPLATE = "SELECT ecm:uuid FROM Document WHERE ecm:isProxy=1 AND ecm:uuid IN ('%s')";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(RemoveProxyComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class RemoveProxyComputation extends AbstractBulkComputation {

        public RemoveProxyComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            String query = String.format(QUERY_TEMPLATE, String.join("', '", ids));
            Set<DocumentRef> proxies = new HashSet<>();
            try (IterableQueryResult res = session.queryAndFetch(query, NXQL.NXQL)) {
                for (Map<String, Serializable> map : res) {
                    proxies.add(new IdRef((String) map.get(NXQL.ECM_UUID)));
                }
            }
            session.removeDocuments(proxies.toArray(new DocumentRef[0]));
            session.save();
        }
    }
}

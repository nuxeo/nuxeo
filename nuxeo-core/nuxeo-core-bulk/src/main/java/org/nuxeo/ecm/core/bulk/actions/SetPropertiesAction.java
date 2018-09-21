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

package org.nuxeo.ecm.core.bulk.actions;

import static org.nuxeo.ecm.core.bulk.StreamBulkProcessor.COUNTER_ACTION_NAME;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.lib.stream.computation.Topology.Builder;

/**
 * @since 10.2
 */
public class SetPropertiesAction extends AbstractBulkAction {

    public static final String ACTION_NAME = "setProperties";

    @Override
    protected Builder addComputations(Builder builder, int size, int threshold) {
        return builder.addComputation(() -> new SetPropertyComputation(size, threshold),
                Arrays.asList("i1:" + ACTION_NAME, "o1:" + COUNTER_ACTION_NAME));
    }

    public static class SetPropertyComputation extends AbstractBulkComputation {

        public SetPropertyComputation(int batchSize, int batchThresholdMs) {
            super(ACTION_NAME, batchSize, batchThresholdMs);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            DocumentRef[] docRefs = ids.stream()
                                       .map(IdRef::new)
                                       .collect(Collectors.toList())
                                       .toArray(new DocumentRef[0]);
            DocumentModelList docs = session.getDocuments(docRefs);
            docs.forEach(doc -> properties.forEach(doc::setPropertyValue));
            session.saveDocuments(docs.toArray(new DocumentModel[0]));
        }
    }

}

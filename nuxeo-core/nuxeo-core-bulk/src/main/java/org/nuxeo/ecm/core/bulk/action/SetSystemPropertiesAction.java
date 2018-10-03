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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 10.3
 */
public class SetSystemPropertiesAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "setSystemProperties";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(SetSystemPropertyComputation::new,
                               Arrays.asList("i1:" + ACTION_NAME, "o1:" + STATUS_STREAM))
                       .build();
    }

    public static class SetSystemPropertyComputation extends AbstractBulkComputation {

        public SetSystemPropertyComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            ids.forEach(id -> properties.forEach((k, v) -> session.setDocumentSystemProp(new IdRef(id), k, v)));
            session.save();
        }

    }
}

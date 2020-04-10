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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 10.3
 */
public class SetSystemPropertiesAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "setSystemProperties";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(SetSystemPropertyComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class SetSystemPropertyComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(SetSystemPropertyComputation.class);

        public SetSystemPropertyComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            Collection<DocumentRef> refs = ids.stream().map(IdRef::new).collect(Collectors.toList());
            for (DocumentRef ref : refs) {
                for (Entry<String, Serializable> entry : properties.entrySet()) {
                    try {
                        session.setDocumentSystemProp(ref, entry.getKey(), entry.getValue());
                    } catch (NuxeoException e) {
                        // TODO send to error stream
                        log.warn("Cannot set system property: " + entry.getKey() + " on: " + ref.toString(), e);
                    }
                }
            }
            try {
                session.save();
            } catch (PropertyException e) {
                // TODO send to error stream
                log.warn("Cannot save session", e);
            }
        }
    }
}

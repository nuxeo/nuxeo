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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
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
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class SetSystemPropertyComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(SetSystemPropertyComputation.class);

        public static final String NOTIFY = "param-notify";

        public SetSystemPropertyComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            Map<String, Serializable> actualProperties = new HashMap<>(properties);
            String eventId = (String) actualProperties.remove(NOTIFY);
            Collection<DocumentRef> refs = ids.stream().map(IdRef::new).collect(Collectors.toList());
            Collection<DocumentRef> updatedRefs = new ArrayList<>(refs.size());
            for (DocumentRef ref : refs) {
                for (Entry<String, Serializable> entry : actualProperties.entrySet()) {
                    try {
                        session.setDocumentSystemProp(ref, entry.getKey(), entry.getValue());
                        updatedRefs.add(ref);
                    } catch (NuxeoException e) {
                        // TODO send to error stream
                        log.warn("Cannot set system property: " + entry.getKey() + " on: " + ref.toString(), e);
                    }
                }
            }
            try {
                session.save();
                if (eventId != null && !updatedRefs.isEmpty()) {
                    fireEvent(session, eventId, updatedRefs);
                }
            } catch (PropertyException e) {
                // TODO send to error stream
                log.warn("Cannot save session", e);
            }
        }

        protected void fireEvent(CoreSession session, String eventId, Collection<DocumentRef> refs) {
            EventService eventService = Framework.getService(EventService.class);
            DocumentModelList docs = session.getDocuments(refs.toArray(new DocumentRef[0]));
            docs.forEach(d -> {
                DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), d);
                ctx.setCategory(DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
                ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
                ctx.setProperty(CoreEventConstants.SESSION_ID, session.getSessionId());
                Event event = ctx.newEvent(eventId);
                event.setInline(false);
                event.setImmediate(false);
                eventService.fireEvent(event);
            });
        }

    }
}

/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Thomas Roger <troger@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action processor that fires group updated events for Nuxeo Drive.
 *
 * @since 11.5
 */
public class FireGroupUpdatedEventAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "driveFireGroupUpdatedEvent";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(FireGroupUpdatedEventComputation::new, //
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class FireGroupUpdatedEventComputation extends AbstractBulkComputation {

        public FireGroupUpdatedEventComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            DocumentModelList docs = loadDocuments(session, ids);
            for (DocumentModel doc : docs) {
                EventContext context = new DocumentEventContext(session, session.getPrincipal(), doc);
                Event event = context.newEvent(NuxeoDriveEvents.GROUP_UPDATED);
                Framework.getService(EventService.class).fireEvent(event);
            }
        }
    }
}

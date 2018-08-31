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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public class SetSystemPropertiesAction extends AbstractBulkAction {

    public static final String ACTION_NAME = "setSystemProperties";

    public SetSystemPropertiesAction() {
        super(ACTION_NAME);
    }

    @Override
    protected Computation createComputation(int batchSize, int batchThresholdMs) {
        return new SetSystemPropertyComputation(getActionName(), batchSize, batchThresholdMs);
    }

    public static class SetSystemPropertyComputation extends AbstractBulkComputation {

        public static final String NOTIFY = "param-notify";

        public SetSystemPropertyComputation(String name, int batchSize, int batchThresholdMs) {
            super(name, 1, 1, batchSize, batchThresholdMs);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            Map<String, Serializable> actualProperties = new HashMap<>(properties);
            String eventId = (String) actualProperties.remove(NOTIFY);
            Collection<DocumentRef> refs = ids.stream().map(IdRef::new).collect(Collectors.toList());
            ids.forEach(id -> actualProperties.forEach((k, v) -> session.setDocumentSystemProp(new IdRef(id), k, v)));
            session.save();
            if (eventId != null) {
                fireEvent(session, eventId, refs);
            }
        }

        protected void fireEvent(CoreSession session, String eventId, Collection<DocumentRef> refs) {
            EventService eventService = Framework.getService(EventService.class);
            DocumentModelList docs = session.getDocuments(refs.toArray(new DocumentRef[refs.size()]));
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

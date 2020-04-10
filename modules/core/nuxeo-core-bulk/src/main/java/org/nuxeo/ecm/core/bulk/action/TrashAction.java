/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.REPOSITORY_NAME;
import static org.nuxeo.ecm.core.api.event.DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_TRASHED;
import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_UNTRASHED;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_NAME;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_PARENTID;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.query.sql.NXQL.NXQL;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.trash.TrashService;
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
public class TrashAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "trash";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String PARAM_NAME = "value";

    public static final String PROXY_QUERY_TEMPLATE = "SELECT ecm:uuid FROM Document WHERE ecm:isProxy=1 AND ecm:uuid IN ('%s')";

    public static final String SYSPROP_QUERY_TEMPLATE = "SELECT ecm:uuid, ecm:name, ecm:parentId FROM Document WHERE ecm:isProxy=0 AND ecm:isTrashed=%s AND ecm:uuid IN ('%s')";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(TrashComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class TrashComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(TrashComputation.class);

        public TrashComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            Boolean trashValue = (Boolean) properties.get(PARAM_NAME);
            if (trashValue) {
                removeProxies(session, ids);
            }
            setSystemProperty(session, ids, trashValue);
        }

        protected void removeProxies(CoreSession session, List<String> ids) {
            Set<DocumentRef> proxies = new HashSet<>();
            String query = String.format(PROXY_QUERY_TEMPLATE, String.join("', '", ids));
            try (IterableQueryResult res = session.queryAndFetch(query, NXQL)) {
                for (Map<String, Serializable> map : res) {
                    proxies.add(new IdRef((String) map.get(ECM_UUID)));
                }
            }
            session.removeDocuments(proxies.toArray(new DocumentRef[0]));
            try {
                session.save();
            } catch (PropertyException e) {
                // TODO send to error stream
                log.warn("Cannot save session", e);
            }
        }

        public void setSystemProperty(CoreSession session, List<String> ids, Boolean value) {
            List<DocumentRef> updatedRefs = new ArrayList<>(ids.size());
            String query = String.format(SYSPROP_QUERY_TEMPLATE, value ? "0" : "1", String.join("', '", ids));
            try (IterableQueryResult res = session.queryAndFetch(query, NXQL)) {
                TrashService trashService = Framework.getService(TrashService.class);
                for (Map<String, Serializable> map : res) {
                    DocumentRef ref = new IdRef((String) map.get(ECM_UUID));
                    try {
                        session.setDocumentSystemProp(ref, "isTrashed", value);
                        String docName = (String) map.get(ECM_NAME);
                        if (!value && trashService.isMangledName(docName)) {
                            DocumentRef parentRef = new IdRef((String) map.get(ECM_PARENTID));
                            session.move(ref, parentRef,
                                    trashService.unmangleName(session, parentRef, docName));
                        }
                        updatedRefs.add(ref);
                    } catch (NuxeoException e) {
                        // TODO send to error stream
                        log.warn("Cannot set system property: isTrashed on: " + ref.toString(), e);
                    }
                }
            }
            try {
                session.save();
                if (!updatedRefs.isEmpty()) {
                    fireEvent(session, value ? DOCUMENT_TRASHED : DOCUMENT_UNTRASHED, updatedRefs);
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
                ctx.setProperty(REPOSITORY_NAME, session.getRepositoryName());
                ctx.setCategory(EVENT_DOCUMENT_CATEGORY);
                Event event = ctx.newEvent(eventId);
                event.setImmediate(false);
                event.setInline(false);
                eventService.fireEvent(event);
            });
        }
    }
}

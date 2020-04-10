/*
 * (C) Copyright 2019 (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.security;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk action executed for documents whose retention has expired.
 *
 * @since 11.1
 */
public class RetentionExpiredAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "retentionExpired";

    public static final String ACTION_FULL_NAME = "retention/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(RetentionExpiredComputation::new, //
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class RetentionExpiredComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(RetentionExpiredComputation.class);

        public RetentionExpiredComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            Collection<DocumentRef> refs = ids.stream().map(IdRef::new).collect(Collectors.toList());
            for (DocumentRef ref : refs) {
                // sanity checks
                if (!session.isRecord(ref)) {
                    log.debug("Document: {} is not a record", ref);
                    continue;
                }
                Calendar retainUntil = session.getRetainUntil(ref);
                if (retainUntil == null) {
                    log.debug("Document: {} was not under retention", ref);
                    continue;
                }
                if (isUnderRetention(retainUntil)) {
                    log.debug("Document: {} is still under retention: {}", () -> ref, retainUntil::toInstant);
                    continue;
                }

                try {
                    // reset retention to null
                    session.setRetainUntil(ref, null, null);
                    // send event about retention expired
                    sendEvent(session, ref, retainUntil);
                } catch (NuxeoException e) {
                    // TODO send to error stream
                    log.warn("Cannot reset retention on: {}", ref, e);
                }

            }
            try {
                session.save();
            } catch (PropertyException e) {
                // TODO send to error stream
                log.warn("Cannot save session", e);
            }
        }

        protected static boolean isUnderRetention(Calendar retainUntil) {
            return retainUntil != null && Calendar.getInstance().before(retainUntil);
        }

        protected void sendEvent(CoreSession session, DocumentRef ref, Calendar retainUntil) {
            DocumentModel doc = session.getDocument(ref);
            DocumentEventContext ctx = new DocumentEventContext(session, null, doc);
            ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
            ctx.setProperty(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
            ctx.setProperty(CoreEventConstants.RETAIN_UNTIL, retainUntil);
            ctx.setProperty(DocumentEventContext.COMMENT_PROPERTY_KEY, retainUntil.toInstant().toString());
            Event event = ctx.newEvent(DocumentEventTypes.RETENTION_EXPIRED);
            Framework.getService(EventService.class).fireEvent(event);
        }
    }

}

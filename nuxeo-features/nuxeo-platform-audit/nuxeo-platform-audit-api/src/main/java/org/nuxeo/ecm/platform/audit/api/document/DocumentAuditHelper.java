/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.audit.api.document;

import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit log stores event related to the "live" DocumentModel. This means that when retrieving the Audit Log for a
 * version or a proxy, we must merge part of the "live" document history with the history of the proxy or version. This
 * helper class fetches the additional parameters that must be used to retrieve history of a version or of a proxy.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class DocumentAuditHelper {

    @SuppressWarnings({ "unchecked", "boxing" })
    public static AdditionalDocumentAuditParams getAuditParamsForUUID(String uuid, CoreSession session) {

        IdRef ref = new IdRef(uuid);
        if (!session.exists(ref)) {
            return null;
        }
        DocumentModel doc = session.getDocument(ref);
        if (!doc.isProxy() && !doc.isVersion()) {
            return null;
        }
        SourceDocumentResolver resolver = new SourceDocumentResolver(session, doc);
        resolver.runUnrestricted();
        if (resolver.sourceDocument == null) {
            return null;
        }
        String targetUUID = resolver.sourceDocument.getId();
        // now get from Audit Logs the creation date of
        // the version / proxy
        QueryBuilder builder = new AuditQueryBuilder().addAndPredicate(Predicates.eq(LOG_DOC_UUID, uuid))
                                                      .addAndPredicate(Predicates.eq(LOG_EVENT_ID,
                                                              DocumentEventTypes.DOCUMENT_CREATED));
        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> entries = reader.queryLogs(builder);
        AdditionalDocumentAuditParams result;
        if (entries != null && entries.size() > 0) {
            result = new AdditionalDocumentAuditParams();
            result.maxDate = entries.get(0).getEventDate();
            result.targetUUID = targetUUID;
            result.eventId = entries.get(0).getId();
        } else {
            // we have no entry in audit log to get the maxDate
            // fallback to repository timestamp
            // this code is here only for compatibility so that it works before version events were added to
            // the audit log
            if (doc.getPropertyValue("dc:modified") == null) {
                return null;
            }
            result = new AdditionalDocumentAuditParams();
            Calendar estimatedDate = ((Calendar) doc.getPropertyValue("dc:modified"));

            // We can not directly use the repo timestamp because Audit and VCS can be in separated DB
            // => try to find the matching TS in Audit
            List<String> ids = new ArrayList<>();
            ids.add(targetUUID);
            if (doc.isVersion()) {
                session.getProxies(doc.getRef(), null).stream().map(DocumentModel::getId).forEach(ids::add);
            }
            estimatedDate.add(Calendar.MILLISECOND, -500);

            QueryBuilder dateBuilder = new AuditQueryBuilder();
            dateBuilder.predicates( //
                    Predicates.in(LOG_DOC_UUID, ids), //
                    Predicates.in(LOG_EVENT_ID, DocumentEventTypes.DOCUMENT_CREATED,
                            DocumentEventTypes.DOCUMENT_CHECKEDIN), //
                    Predicates.gte(LOG_EVENT_DATE, estimatedDate.getTime()) //
            );
            dateBuilder.orders(OrderByExprs.asc(LOG_EVENT_ID));
            dateBuilder.offset(0).limit(20);
            List<LogEntry> dateEntries = reader.queryLogs(dateBuilder);
            if (dateEntries.size() > 0) {
                result.targetUUID = targetUUID;
                Calendar maxDate = new GregorianCalendar();
                maxDate.setTime(dateEntries.get(0).getEventDate());
                maxDate.add(Calendar.MILLISECOND, -500);
                result.maxDate = maxDate.getTime();
            } else {
                // no other choice : use the VCS TS
                // results may be truncated in some DB config
                result.targetUUID = targetUUID;
                result.maxDate = ((Calendar) doc.getPropertyValue("dc:modified")).getTime();
            }
        }
        return result;
    }

}

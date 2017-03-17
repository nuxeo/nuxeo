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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
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
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        FilterMapEntry filter = new FilterMapEntry();
        filter.setColumnName("eventId");
        filter.setOperator("=");
        filter.setQueryParameterName("eventId");
        filter.setObject(DocumentEventTypes.DOCUMENT_CREATED);
        Map<String, FilterMapEntry> filters = new HashMap<String, FilterMapEntry>();
        filters.put("eventId", filter);
        List<LogEntry> entries = reader.getLogEntriesFor(uuid, filters, false);
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

            List<LogEntry> dateEntries = runNativeQueryForBackend(session, doc, targetUUID, reader);
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

    /**
     * Fix to generate and run the query based on the backend available for the Audit.
     *
     * @return
     * @since 8.10
     */
    @SuppressWarnings("unchecked")
    protected static List<LogEntry> runNativeQueryForBackend(CoreSession session, DocumentModel doc, String targetUUID,
            AuditReader reader) {
        Calendar estimatedDate = ((Calendar) doc.getPropertyValue("dc:modified"));

        // We can not directly use the repo timestamp because Audit and VCS can be in separated DB
        // => try to find the matching TS in Audit
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("minDate", estimatedDate.getTime());
        estimatedDate.add(Calendar.MILLISECOND, -500);

        StringBuilder queryString = new StringBuilder();
        // Check if the audit uses elasticsearch as backend
        boolean isESBackEnd = reader.getClass().getSimpleName().equals("ESAuditBackend");
        if (isESBackEnd) {
            queryString.append("{\"query\":{\"bool\":{\"must\": { \"match_all\": {} },"
                    + "\"filter\": {\"and\" : [{\"terms\": {\"docUUID\": [");

            // Build the list of ids
            queryString.append("\"" + targetUUID + "\"");
            if (doc.isVersion()) {
                DocumentModelList proxies = session.getProxies(doc.getRef(), null);
                for (DocumentModel proxy : proxies) {
                    queryString.append(",\"" + proxy.getId() + "\"");
                }
            }
            // Add the events
            queryString.append("]}}, {\"terms\": {\"eventId\": [");
            queryString.append("\"" + DocumentEventTypes.DOCUMENT_CREATED + "\"");
            queryString.append(",\"" + DocumentEventTypes.DOCUMENT_CHECKEDIN + "\"");

            // Add the range date
            queryString.append("]}},{\"range\": {\"eventDate\": {\"gte\" : \"${minDate}\"}}}]}}},\"sort\":[{\"eventId\": \"asc\"}]}");
        } else {
            queryString.append("from LogEntry log where log.docUUID in (");
            queryString.append("'" + targetUUID + "'");
            if (doc.isVersion()) {
                DocumentModelList proxies = session.getProxies(doc.getRef(), null);
                for (DocumentModel proxy : proxies) {
                    queryString.append(",'" + proxy.getId() + "'");
                }
            }
            queryString.append(",'" + doc.getId() + "'");
            queryString.append(") AND log.eventId IN (");
            queryString.append("'" + DocumentEventTypes.DOCUMENT_CREATED + "'");
            queryString.append(",'" + DocumentEventTypes.DOCUMENT_CHECKEDIN + "'");
            queryString.append(") AND log.eventDate >= :minDate ");
            queryString.append(" order by log.eventId asc");
        }

        return (List<LogEntry>) reader.nativeQuery(queryString.toString(), params, 0, 20);
    }

}

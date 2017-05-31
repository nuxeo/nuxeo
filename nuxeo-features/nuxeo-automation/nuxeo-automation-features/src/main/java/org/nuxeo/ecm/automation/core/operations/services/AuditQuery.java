/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = AuditQuery.ID, category = Constants.CAT_SERVICES, label = "Query Audit Service", description = "Execute a JPA query against the Audit Service. This is returning a blob with the query result. The result is a serialized JSON array. You can use the context to set query variables but you must prefix using 'audit.query.' the context variable keys that match the ones in the query.", addToStudio = false)
public class AuditQuery {

    public static final String ID = "Audit.Query";

    @Context
    protected AuditReader audit;

    @Context
    protected OperationContext ctx;

    @Param(name = "query", required = true, widget = Constants.W_MULTILINE_TEXT)
    protected String query;

    @Param(name = "pageNo", required = false)
    protected int pageNo = 1;

    @Param(name = "maxResults", required = false)
    protected int maxResults;

    @OperationMethod
    public Blob run() {
        List<LogEntry> result = query();
        JSONArray rows = new JSONArray();
        for (LogEntry entry : result) {
            JSONObject obj = new JSONObject();
            obj.element("eventId", entry.getEventId());
            obj.element("category", entry.getCategory());
            obj.element("eventDate", entry.getEventDate().getTime());
            obj.element("principal", entry.getPrincipalName());
            obj.element("docUUID", entry.getDocUUID());
            obj.element("docType", entry.getDocType());
            obj.element("docPath", entry.getDocPath());
            obj.element("docLifeCycle", entry.getDocLifeCycle());
            obj.element("repoId", entry.getRepositoryId());
            obj.element("comment", entry.getComment());
            // Map<String, ExtendedInfo> info = entry.getExtendedInfos();
            // if (info != null) {
            // info.get
            // }
            rows.add(obj);
        }
        return Blobs.createJSONBlob(rows.toString());
    }

    public List<LogEntry> query() {
        PersistenceProviderFactory pf = Framework.getService(PersistenceProviderFactory.class);
        PersistenceProvider provider = pf.newProvider("nxaudit-logs");
        return provider.run(false, new RunCallback<List<LogEntry>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<LogEntry> runWith(EntityManager em) {
                Query q = em.createQuery(query);
                if (maxResults > 0) {
                    q.setMaxResults(maxResults);
                    q.setFirstResult((pageNo - 1) * maxResults);
                }
                for (Map.Entry<String, Object> entry : ctx.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("audit.query.")) {
                        setQueryParam(q, key.substring("audit.query.".length()), entry.getValue());
                    }
                }
                return q.getResultList();
            }
        });

    }

    protected void setQueryParam(Query q, String key, Object value) {
        if (value instanceof String) {
            String v = (String) value;
            if (v.startsWith("{d ") && v.endsWith("}")) {
                v = v.substring(3, v.length() - 1).trim();
                int i = v.indexOf(' ');
                if (i == -1) {
                    Date date = Date.valueOf(v);
                    q.setParameter(key, date);
                } else {
                    Timestamp ts = Timestamp.valueOf(v);
                    q.setParameter(key, ts);
                }
            } else {
                q.setParameter(key, v);
            }
        } else {
            q.setParameter(key, value);
        }
    }
}

/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = AuditQuery.ID, category = Constants.CAT_SERVICES, label = "Query Audit Service", description = "Execute a JPA query against the Audit Service. This is returning a blob with the query result. The result is a serialized JSON array. You can use the context to set query variables but you must prefix using 'audit.query.' the context variable keys that match the ones in the query.")
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
    public Blob run() throws Exception {
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
            obj.element("comment", entry.getComment());
            // Map<String, ExtendedInfo> info = entry.getExtendedInfos();
            // if (info != null) {
            // info.get
            // }
            rows.add(obj);
        }
        return new StringBlob(rows.toString(), "application/json");
    }

    public List<LogEntry> query() throws Exception {
        PersistenceProviderFactory pf = Framework.getService(PersistenceProviderFactory.class);
        PersistenceProvider provider = pf.newProvider("nxaudit-logs");
        return provider.run(false, new RunCallback<List<LogEntry>>() {
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
                        setQueryParam(q,
                                key.substring("audit.query.".length()),
                                entry.getValue());
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

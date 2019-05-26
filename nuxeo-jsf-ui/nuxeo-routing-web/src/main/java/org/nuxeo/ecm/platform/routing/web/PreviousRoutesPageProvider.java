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
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.web;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link PageProvider} implementation that returns {@link LogEntry} from Audit Service - Used for Route History content
 * view
 *
 * @Since 5.6
 */
public class PreviousRoutesPageProvider extends AbstractPageProvider<LogEntry> implements PageProvider<LogEntry> {

    private static final long serialVersionUID = 1L;

    protected String auditQuery;

    protected Map<String, Object> auditQueryParams;

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String UICOMMENTS_PROPERTY = "generateUIComments";

    public static final String DOC_ID_PROPERTY = "documentID";

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nquery : ").append(auditQuery).append("\nparams : ");
        List<String> pNames = new ArrayList<>(auditQueryParams.keySet());
        Collections.sort(pNames);
        for (String name : pNames) {
            sb.append("\n ");
            sb.append(name);
            sb.append(" : ");
            sb.append(auditQueryParams.get(name).toString());
        }

        return sb.toString();
    }

    protected void preprocessCommentsIfNeeded(List<LogEntry> entries) {
        Serializable preprocess = getProperties().get(UICOMMENTS_PROPERTY);
        CoreSession session = (CoreSession) getProperties().get(CORE_SESSION_PROPERTY);
        if (session != null && preprocess != null && "true".equalsIgnoreCase(preprocess.toString())) {
            CommentProcessorHelper cph = new CommentProcessorHelper(session);
            cph.processComments(entries);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LogEntry> getCurrentPage() {
        AuditReader reader = Framework.getService(AuditReader.class);

        buildAuditQuery(true);
        // TODO: Follow https://jira.nuxeo.com/browse/NXP-17236, native query should use JSON in case of Elasticsearch
        // audit backend
        List<LogEntry> entries = (List<LogEntry>) reader.nativeQuery(auditQuery, auditQueryParams,
                (int) getCurrentPageIndex() + 1, (int) getMinMaxPageSize());
        preprocessCommentsIfNeeded(entries);
        return entries;
    }

    protected String getSortPart() {
        StringBuilder sort = new StringBuilder();
        if (getSortInfos() != null && getSortInfos().size() > 0) {
            sort.append(" ORDER BY ");
        }
        int index = 0;
        for (SortInfo si : getSortInfos()) {
            if (index > 0) {
                sort.append(" , ");
            }
            sort.append(si.getSortColumn());
            if (si.getSortAscending()) {
                sort.append(" ASC ");
            } else {
                sort.append(" DESC ");
            }
            index++;
        }
        return sort.toString();
    }

    protected Object convertParam(Object param) {
        if (param == null) {
            return null;
        }
        // Hibernate does not like Calendar type
        if (param instanceof Calendar) {
            return new Timestamp(((Calendar) param).getTime().getTime());
        }
        return param;
    }

    protected boolean isNonNullParam(Object[] val) {
        if (val == null) {
            return false;
        }
        for (Object v : val) {
            if (v != null) {
                if (v instanceof String) {
                    if (!((String) v).isEmpty()) {
                        return true;
                    }
                } else if (v instanceof String[]) {
                    if (((String[]) v).length > 0) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    protected String getFixedPart() {
        if (getDefinition().getWhereClause() == null) {
            return null;
        } else {
            return getDefinition().getWhereClause().getFixedPart();
        }
    }

    protected boolean allowSimplePattern() {
        return true;
    }

    protected void buildAuditQuery(boolean includeSort) {
        PageProviderDefinition def = getDefinition();
        Object[] params = getParameters();

        if (def.getWhereClause() == null) {
            // Simple Pattern

            if (!allowSimplePattern()) {
                throw new UnsupportedOperationException("This page provider requires a explicit Where Clause");
            }

            String baseQuery = def.getPattern();

            Map<String, Object> qParams = new HashMap<>();
            for (int i = 0; i < params.length; i++) {
                baseQuery = baseQuery.replaceFirst("\\?", ":param" + i);
                qParams.put("param" + i, convertParam(params[i]));
            }

            if (includeSort) {
                baseQuery = baseQuery + getSortPart();
            }

            auditQuery = baseQuery;
            auditQueryParams = qParams;

        } else {
            // Where clause based on DocumentModel

            StringBuilder baseQuery = new StringBuilder("from LogEntry log ");

            // manage fixed part
            String fixedPart = getFixedPart();
            Map<String, Object> qParams = new HashMap<>();
            int idxParam = 0;
            if (fixedPart != null && !fixedPart.isEmpty()) {
                while (fixedPart.indexOf('?') >= 0) {
                    // Hack for handling parameter in fixed part TODO
                    if (getProperties().get(DOC_ID_PROPERTY) != null) {
                        fixedPart = fixedPart.replaceFirst("\\?", getProperties().get(DOC_ID_PROPERTY).toString());
                        // Map properties = new HashMap<String, Serializable>();
                        // properties.put(DOC_ID_PROPERTY, "done");
                    } else {
                        fixedPart = fixedPart.replaceFirst("\\?", ":param" + idxParam);
                        qParams.put("param" + idxParam, convertParam(params[idxParam]));
                        idxParam++;
                    }
                }
                baseQuery.append(" where ");
                baseQuery.append(fixedPart);
            }

            // manages predicates
            DocumentModel searchDocumentModel = getSearchDocumentModel();
            if (searchDocumentModel != null) {
                PredicateDefinition[] predicates = def.getWhereClause().getPredicates();
                int idxPredicate = 0;

                for (PredicateDefinition predicate : predicates) {

                    // extract data from DocumentModel
                    PredicateFieldDefinition[] fieldDef = predicate.getValues();
                    Object[] val = new Object[fieldDef.length];
                    for (int fidx = 0; fidx < fieldDef.length; fidx++) {
                        if (fieldDef[fidx].getXpath() != null) {
                            val[fidx] = searchDocumentModel.getPropertyValue(fieldDef[fidx].getXpath());
                        } else {
                            val[fidx] = searchDocumentModel.getProperty(fieldDef[fidx].getSchema(),
                                    fieldDef[fidx].getName());
                        }
                    }

                    if (!isNonNullParam(val)) {
                        // skip predicate where all values are null
                        continue;
                    }

                    if (idxPredicate > 0 || idxParam > 0) {
                        baseQuery.append(" AND ");
                    } else {
                        baseQuery.append(" where ");
                    }

                    baseQuery.append(predicate.getParameter());
                    baseQuery.append(" ");

                    if (!predicate.getOperator().equalsIgnoreCase("BETWEEN")) {
                        // don't add the between operation for now
                        baseQuery.append(predicate.getOperator());
                    }

                    if (predicate.getOperator().equalsIgnoreCase("IN")) {
                        baseQuery.append(" (");

                        if (val[0] instanceof Iterable<?>) {
                            Iterable<?> vals = (Iterable<?>) val[0];
                            Iterator<?> valueIterator = vals.iterator();

                            while (valueIterator.hasNext()) {
                                Object v = valueIterator.next();
                                qParams.put("param" + idxParam, convertParam(v));
                                baseQuery.append(" :param").append(idxParam++);
                                if (valueIterator.hasNext()) {
                                    baseQuery.append(",");
                                }
                            }
                        } else if (val[0] instanceof Object[]) {
                            Object[] valArray = (Object[]) val[0];
                            for (int i = 0; i < valArray.length; i++) {
                                Object v = valArray[i];
                                qParams.put("param" + idxParam, convertParam(v));
                                baseQuery.append(" :param").append(idxParam++);
                                if (i < valArray.length - 1) {
                                    baseQuery.append(",");
                                }
                            }
                        }
                        baseQuery.append(" ) ");
                    } else if (predicate.getOperator().equalsIgnoreCase("BETWEEN")) {
                        Object startValue = convertParam(val[0]);
                        Object endValue = null;
                        if (val.length > 1) {
                            endValue = convertParam(val[1]);
                        }
                        if (startValue != null && endValue != null) {
                            baseQuery.append(predicate.getOperator()).append(" :param").append(idxParam);
                            qParams.put("param" + idxParam++, startValue);
                            baseQuery.append(" AND :param").append(idxParam);
                            qParams.put("param" + idxParam, endValue);
                        } else if (startValue == null) {
                            baseQuery.append("<=").append(" :param").append(idxParam);
                            qParams.put("param" + idxParam, endValue);
                        } else if (endValue == null) {
                            baseQuery.append(">=").append(" :param").append(idxParam);
                            qParams.put("param" + idxParam, startValue);
                        }
                        idxParam++;
                    } else {
                        baseQuery.append(" :param").append(idxParam);
                        qParams.put("param" + idxParam, convertParam(val[0]));
                        idxParam++;
                    }

                    idxPredicate++;
                }
            }

            if (includeSort) {
                baseQuery.append(getSortPart());
            }

            auditQuery = baseQuery.toString();
            auditQueryParams = qParams;
        }
    }

    @Override
    public void refresh() {
        setCurrentPageOffset(0);
        super.refresh();
    }

    @SuppressWarnings("unchecked")
    @Override
    public long getResultsCount() {
        buildAuditQuery(false);
        AuditReader reader = Framework.getService(AuditReader.class);
        // TODO: Follow https://jira.nuxeo.com/browse/NXP-17236, native query should use JSON in case of Elasticsearch
        // audit backend, yet currently there doesn't seem to be any way to do such a count in this case
        List<Long> res = (List<Long>) reader.nativeQuery("select count(log.id) " + auditQuery, auditQueryParams, 1, 20);
        resultsCount = res.get(0).longValue();
        return resultsCount;
    }

}

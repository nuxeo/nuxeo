/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.audit.api;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link PageProvider} implementation that returns {@link LogEntry} from Audit
 * Service
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class AuditPageProvider extends AbstractPageProvider<LogEntry> implements
        PageProvider<LogEntry> {

    private static final long serialVersionUID = 1L;

    protected long currentPageIndex = 1;

    protected String auditQuery;

    protected Map<String, Object> auditQueryParams;

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("\nquery : " + auditQuery);
        sb.append("\nparams : ");
        List<String> pNames = new ArrayList<String>(auditQueryParams.keySet());
        Collections.sort(pNames);
        for (String name : pNames) {
            sb.append("\n ");
            sb.append(name);
            sb.append(" : ");
            sb.append(auditQueryParams.get(name).toString());
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LogEntry> getCurrentPage() {
        AuditReader reader;
        try {
            reader = Framework.getService(AuditReader.class);
        } catch (Exception e) {
            return null;
        }

        buildAuditQuery(true);
        return (List<LogEntry>) reader.nativeQuery(auditQuery,
                auditQueryParams, (int) getCurrentPageIndex(),
                (int) getPageSize());

    }

    protected String getSortPart() {
        StringBuffer sort = new StringBuffer();
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
        if (param==null) {
            return null;
        }
        // Hibernate does not like Calendar type
        if (param instanceof Calendar) {
            return new Timestamp(((Calendar) param).getTime().getTime());
            //return ((Calendar) param).getTime();
        }
        return param;
    }

    protected boolean isNonNullParam(Object[] val) {
        if (val==null) {
            return false;
        }
        for (Object v : val) {
            if (v!=null) {
                return true;
            }
        }
        return false;
    }

    protected void buildAuditQuery(boolean includeSort) {
        PageProviderDefinition def = getDefinition();
        Object[] params = getParameters();

        if (def.getWhereClause() == null) {
            // Simple Pattern

            String baseQuery = def.getPattern();

            Map<String, Object> qParams = new HashMap<String, Object>();
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

            StringBuilder baseQuery = new StringBuilder(
                    "from LogEntry log where ");

            // manage fixed part
            String fixedPart = def.getWhereClause().getFixedPart();
            Map<String, Object> qParams = new HashMap<String, Object>();
            int idxParam = 0;
            if (fixedPart != null && !fixedPart.isEmpty()) {
                while (fixedPart.indexOf("?") > 0) {
                    fixedPart = fixedPart.replaceFirst("\\?", ":param"
                            + idxParam);
                    qParams.put("param" + idxParam,
                            convertParam(params[idxParam]));
                    idxParam++;
                }
                baseQuery.append(fixedPart);
            }

            // manages predicates
            DocumentModel searchDocumentModel = getSearchDocumentModel();
            if (searchDocumentModel == null) {
                throw new ClientRuntimeException(String.format(
                        "Cannot build query of provider '%s': "
                                + "no search document model is set", getName()));
            }
            PredicateDefinition[] predicates = def.getWhereClause().getPredicates();
            int idxPredicate = 0;

            for (PredicateDefinition predicate : predicates) {

                // extract data from DocumentModel
                Object val[];
                try {
                    PredicateFieldDefinition[] fieldDef = predicate.getValues();
                    val = new Object[fieldDef.length];

                    for (int fidx = 0; fidx < fieldDef.length; fidx++) {
                        if (fieldDef[fidx].getXpath() != null) {
                            val[fidx] = searchDocumentModel.getPropertyValue(fieldDef[fidx].getXpath());
                        } else {
                            val[fidx] = searchDocumentModel.getProperty(
                                    fieldDef[fidx].getSchema(),
                                    fieldDef[fidx].getName());
                        }
                    }
                } catch (Exception e) {
                    throw new ClientRuntimeException(e);
                }

                if (!isNonNullParam(val)) {
                    // skip predicate where all values are null
                    continue;
                }

                if (idxPredicate > 0 || idxParam > 0) {
                    baseQuery.append(" AND ");
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
                            baseQuery.append(" :param" + idxParam);
                            idxParam++;
                            if (valueIterator.hasNext()) {
                                baseQuery.append(",");
                            }
                        }
                    } else if (val[0] instanceof Object[]) {
                        Object[] valArray = (Object[]) val[0];
                        for (int i = 0; i < valArray.length; i++) {
                            Object v = valArray[i];
                            qParams.put("param" + idxParam, convertParam(v));
                            baseQuery.append(" :param" + idxParam);
                            idxParam++;
                            if (i < valArray.length - 1) {
                                baseQuery.append(",");
                            }
                        }
                    }
                    baseQuery.append(" ) ");
                }
                else if (predicate.getOperator().equalsIgnoreCase("BETWEEN")) {
                    Object startValue = convertParam(val[0]);
                    Object endValue = null;
                    if (val.length>1) {
                        endValue = convertParam(val[1]);
                    }

                    if (startValue!=null && endValue!=null) {
                        baseQuery.append(predicate.getOperator());
                        baseQuery.append(" :param" + idxParam);
                        qParams.put("param" + idxParam, startValue);
                        idxParam++;
                        baseQuery.append(" AND :param" + idxParam);
                        qParams.put("param" + idxParam, endValue);
                    } else if (startValue==null) {
                        baseQuery.append("<=");
                        baseQuery.append(" :param" + idxParam);
                        qParams.put("param" + idxParam, endValue);
                    } else if (endValue==null) {
                        baseQuery.append(">=");
                        baseQuery.append(" :param" + idxParam);
                        qParams.put("param" + idxParam, startValue);
                    }
                    idxParam++;
                } else {
                    baseQuery.append(" :param" + idxParam);
                    qParams.put("param" + idxParam, convertParam(val[0]));
                    idxParam++;
                }

                idxPredicate++;
            }

            if (includeSort) {
                baseQuery.append(getSortPart());
            }

            auditQuery = baseQuery.toString();
            auditQueryParams = qParams;
        }
    }

    @Override
    public long getCurrentPageIndex() {
        return currentPageIndex;
    }

    @Override
    public void nextPage() {
        currentPageIndex += 1;
    }

    @Override
    public void previousPage() {
        currentPageIndex -= 1;
        if (currentPageIndex < 1) {
            currentPageIndex = 1;
        }
    }

    public void refresh() {
        super.refresh();
        currentPageIndex=1;
    }

    @Override
    public void firstPage() {
        currentPageIndex = 1;
    }

    @Override
    public List<LogEntry> setCurrentPage(long page) {
        currentPageIndex = page;
        return getCurrentPage();
    }

    @Override
    public long getNumberOfPages() {
        if (this.pageSize == 0L) {
            return 1L;
        }
        return (int) (1L + (getResultsCount() - 1L) / this.pageSize);
    }

    @SuppressWarnings("unchecked")
    @Override
    public long getResultsCount() {
        buildAuditQuery(false);

        AuditReader reader;
        try {
            reader = Framework.getService(AuditReader.class);
        } catch (Exception e) {
            return 0;
        }

        List<Long> res = (List<Long>) reader.nativeQuery(
                "select count(log.id) " + auditQuery, auditQueryParams, 1, 20);
        resultsCount = ((Long) res.get(0)).longValue();

        return this.resultsCount;
    }
}

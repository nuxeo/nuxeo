/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.querymodel.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.runtime.model.RuntimeContext;

@XObject(value = "queryModel")
public class QueryModelDescriptor {

    private static final Log log = LogFactory.getLog(QueryModelDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("@docType")
    protected String docType;

    protected String pattern;

    @XNode("pattern")
    public void setPattern(String pattern) {
        // remove new lines and following spaces
        if(pattern != null) {
          this.pattern = pattern.replaceAll("\r?\n\\s*", " ");
        }
    }

    @XNode("whereClause")
    protected WhereClauseDescriptor whereClause;

    @XNode("sortColumn/field")
    protected FieldDescriptor sortColumnField;

    @XNode("sortAscending/field")
    protected FieldDescriptor sortAscendingField;

    @XNode("batchSize/field")
    protected FieldDescriptor batchSize;

    @XNode("batchLength/field")
    protected FieldDescriptor batchLength;

    @XNodeList(value = "facet-filter/facet", type = ArrayList.class, componentType = FacetDescriptor.class)
    public List<FacetDescriptor> filterFacets;

    @XNode("sortable@defaultSortColumn")
    protected String defaultSortColumn;

    public FieldDescriptor getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(FieldDescriptor batchSize) {
        this.batchSize = batchSize;
    }

    public FieldDescriptor getBatchLength() {
        return batchLength;
    }

    public void setBatchLength(FieldDescriptor batchLength) {
        this.batchLength = batchLength;
    }

    public String getDefaultSortColumn() {
        return defaultSortColumn;
    }

    public void setDefaultSortColumn(String defaultSortColumn) {
        this.defaultSortColumn = defaultSortColumn;
    }

    public Boolean getDefaultSortAscending() {
        return defaultSortAscending;
    }

    public void setDefaultSortAscending(Boolean defaultSortAscending) {
        this.defaultSortAscending = defaultSortAscending;
    }

    public Boolean getSortable() {
        return sortable;
    }

    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    public String getPattern() {
        return pattern;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSortColumnField(FieldDescriptor sortColumnField) {
        this.sortColumnField = sortColumnField;
    }

    public void setSortAscendingField(FieldDescriptor sortAscendingField) {
        this.sortAscendingField = sortAscendingField;
    }

    @XNode("sortable@defaultSortAscending")
    protected Boolean defaultSortAscending = true;

    /**
     * used for stateless qm a sortable qm is one that does not have an ORDER BY
     * clause, so that a query with sortInfo can append them
     *
     * @deprecated - do not use an ORDER BY clause in the pattern, use
     *             defaultSortColumn and defaultSortAscending
     */
    @Deprecated
    @XNode("sortable@value")
    Boolean sortable = false;

    @XNode("max")
    // TODO tie page length to a field and use this one as default for BBB
    protected Integer max;

    // no-arg constructor
    public QueryModelDescriptor() {
    }

    public QueryModelDescriptor(String name, String docType, String pattern,
            List<FacetDescriptor> filterFacets,
            WhereClauseDescriptor whereClause) {
        this.name = name;
        this.docType = docType;
        this.pattern = pattern;
        this.filterFacets = filterFacets;
        this.whereClause = whereClause;
    }

    public QueryModelDescriptor(String name, String docType, Integer max,
            WhereClauseDescriptor whereClause, FieldDescriptor sortColumnField,
            FieldDescriptor sortAscendingField) {
        this.name = name;
        this.docType = docType;
        this.max = max;
        this.whereClause = whereClause;
        this.sortColumnField = sortColumnField;
        this.sortAscendingField = sortAscendingField;
    }

    public boolean isStateless() {
        return pattern != null;
    }

    public boolean isStateful() {
        return docType != null;
    }

    public String getDocType() {
        return docType;
    }

    public String getQuery(DocumentModel model) throws ClientException {
        return getQuery(model, null);
    }

    public SortInfo getDefaultSortInfo(DocumentModel model) {
        if (isStateful()) {
            if (sortColumnField == null || sortAscendingField == null) {
                return null;
            }
            String sortColumn = sortColumnField.getPlainStringValue(model);
            Boolean sortAscendingObj = sortAscendingField.getBooleanValue(model);
            boolean sortAscending = Boolean.TRUE.equals(sortAscendingObj);
            if (sortColumn == null) {
                return null;
            } else {
                return new SortInfo(sortColumn, sortAscending);
            }
        } else {
            if (defaultSortColumn == null) {
                return null;
            } else {
                return new SortInfo(defaultSortColumn, defaultSortAscending);
            }
        }
    }

    public String getQuery(DocumentModel model, SortInfo sortInfo)
            throws ClientException {
        if (!isStateful()) {
            throw new ClientException(name + " is not stateful");
        }
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM Document");
        if (whereClause != null) {
            queryBuilder.append(whereClause.getQueryElement(model));
        }

        if (sortInfo != null) {
            String sortColumn = sortInfo.getSortColumn();
            boolean sortAscending = sortInfo.getSortAscending();
            queryBuilder.append(" ORDER BY ");
            queryBuilder.append(sortColumn);
            if (!sortAscending) {
                queryBuilder.append(" DESC");
            }
        }
        // TODO: add support for the Batching clause syntax once it's
        // implemented in the core
        String res = queryBuilder.toString();
        log.debug(res);
        return res;
    }

    public String getQuery(Object[] params) throws ClientException {
        return getQuery(params, null);
    }

    /**
     * Return the string literal in a form ready to embed in an NXQL statement.
     * TODO remove this once we work on org.nuxeo.core, v 1.4
     *
     * @param s
     * @return
     */
    // TODO remove this once we work on org.nuxeo.core, v 1.4
    public static String prepareStringLiteral(String s) {
        return "'" + s.replaceAll("'", "\\\\'") + "'";
    }

    private static void appendQuotedStringList(StringBuilder queryBuilder,
            List<?> listParam) {
        queryBuilder.append('(');
        List<String> quotedParam = new ArrayList<String>(listParam.size());
        for (Object param : listParam) {
            quotedParam.add(prepareStringLiteral(param.toString()));
        }
        queryBuilder.append(StringUtils.join(quotedParam, ", "));
        queryBuilder.append(')');
    }

    public String getQuery(Object[] params, SortInfo sortInfo)
            throws ClientException {
        if (!isStateless()) {
            throw new ClientException(name + " is not stateless");
        }
        StringBuilder queryBuilder;
        if (params == null) {
            queryBuilder = new StringBuilder(pattern + ' ');
        } else {
            // XXX: the core should provide an escaping scheme by default
            // similar to the JDBC PreparedStatement class

            // XXX: the + " " is a workaround for the buggy implementation of
            // the split function in case the pattern ends with '?'
            String[] queryStrList = (pattern + ' ').split("\\?");
            queryBuilder = new StringBuilder(queryStrList[0]);
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String[]) {
                    appendQuotedStringList(queryBuilder,
                            Arrays.asList((String[]) params[i]));
                } else if (params[i] instanceof List) {
                    appendQuotedStringList(queryBuilder,
                            (List<?>) params[i]);
                } else if (params[i] instanceof Boolean) {
                    boolean b = ((Boolean) params[i]).booleanValue();
                    queryBuilder.append(b ? 1 : 0);
                } else if (params[i] instanceof Number) {
                    queryBuilder.append(params[i]);
                } else if (params[i] instanceof Literal) {
                    queryBuilder.append(params[i].toString());
                } else {
                    String queryParam = params[i].toString();
                    // this will escape everything as if it where a string
                    // use a literal if you want to do your own custom stuff
                    // TODO replug escaper from SQLQueryParser
                    queryBuilder.append(prepareStringLiteral(queryParam));
                }
                queryBuilder.append(queryStrList[i + 1]);
            }
        }
        if (sortInfo != null) {
            String sortColumn = sortInfo.getSortColumn();
            boolean sortAscending = sortInfo.getSortAscending();
            queryBuilder.append("ORDER BY ").append(sortColumn).append(' ').append(
                    sortAscending ? "" : "DESC");
        }
        return queryBuilder.toString().trim();
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public String getName() {
        return name;
    }

    public Filter getFilter() {
        if (filterFacets == null || filterFacets.isEmpty()) {
            return null;
        }
        List<String> requiredFacets = new ArrayList<String>();
        List<String> excludedFacets = new ArrayList<String>();
        for (FacetDescriptor descriptor : filterFacets) {
            if (descriptor.isRequired()) {
                requiredFacets.add(descriptor.getName());
            } else {
                excludedFacets.add(descriptor.getName());
            }
        }
        return new FacetFilter(requiredFacets, excludedFacets);
    }

    public WhereClauseDescriptor getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(WhereClauseDescriptor whereClause) {
        this.whereClause = whereClause;
    }

    public FieldDescriptor getSortColumnField() {
        return sortColumnField;
    }

    public FieldDescriptor getSortAscendingField() {
        return sortAscendingField;
    }

    public boolean isSortable() {
        if (isStateful()) {
            return sortColumnField != null && sortAscendingField != null;
        } else {
            return true;
        }
    }

    /**
     * Init the escaper object for stateful query models.
     * <p>
     * This is meant to be called at extension point contribution registration
     * time.
     * </p>
     *
     * @param context surrounding context, used to load the correct class.
     */
    public void initEscaper(RuntimeContext context) {
        whereClause.initEscaper(context);
    }

}

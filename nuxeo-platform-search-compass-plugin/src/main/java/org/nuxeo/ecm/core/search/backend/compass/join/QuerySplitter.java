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

package org.nuxeo.ecm.core.search.backend.compass.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.search.NXSearch;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;

/**
 * This
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class QuerySplitter {

    private static FromClause matchAllFromClause;

    private static SelectClause selectAllClause;

    private final SearchServiceInternals service;

    private Predicate predicate;

    private final FromClause from;

    private final SelectClause select;

    private OrderByClause orderBy;

    private final SQLQuery query;

    protected final List<SubQuery> subQueries;

    // resource type -> pairs (main field, sub field)
    protected final Map<String, String[]> joins;

    private SplitQuery splitQuery;

    public QuerySplitter(SQLQuery query) {
        if (matchAllFromClause == null) { // un pour tous, tous pour un
            matchAllFromClause = new FromClause();
            matchAllFromClause.add(TypeConstants.DOCUMENT);

            // empty select means all
            selectAllClause = new SelectClause();
        }
        service = (SearchServiceInternals) NXSearch.getSearchService();

        this.query = query;
        WhereClause where = query.getWhereClause();
        from = query.getFromClause();
        select = query.getSelectClause();

        if (where != null) {
            predicate = where.predicate;
        }
        subQueries = new LinkedList<SubQuery>();
        joins = new HashMap<String, String[]>();
    }

    public static boolean isJoin(Predicate p) {
        return p.lvalue instanceof Reference
                && p.rvalue instanceof Reference;
    }

    public SplitQuery split() throws QueryException {

        // Ensure idempotency
        if (splitQuery != null) {
            return splitQuery;
        }

        if (predicate == null) { // empty where clause
            return new SplitQuery(query, null);
        }

        while (predicate != null) {
            extractSubQuery();
        }

        // single out main query and put join info.
        List<SubQuery> joinedSubQueries = new ArrayList<SubQuery>(
                subQueries.size() - 1);


        SQLQuery mainQuery = null;
        for (SubQuery subQuery : subQueries) {
            String type = subQuery.getResourceType();
            if (isMainType(type)) {
                mainQuery = new SQLQuery(select, from,
                        subQuery.getQuery().getWhereClause(), orderBy);
                continue;
            }
           String[] joinInfo = joins.get(subQuery.getResourceName());
           if (joinInfo == null) {
               continue;
           }
           subQuery.setJoinInfo(joinInfo[0], joinInfo[1]);
           joinedSubQueries.add(subQuery);
        }

        splitQuery = new SplitQuery(mainQuery, joinedSubQueries);
        return splitQuery;
    }

    /**
     * Extract a sub query from the query
     *
     * <p>Implementation heavily relies on the shape of queries produced by
     * current parser: n-fold AND operator is implemented as a totally
     * unbalanced tree (long leg on the right).
     * </p>
     *
     * @return the extracted query
     * @throws QueryException
     */
    protected void extractSubQuery() throws QueryException {
        if (! Operator.AND.equals(predicate.operator)) {
            // Goes directly in main clause
            String[] resourceTypeName = resourceTypeOf(predicate);
            subQueries.add(new SubQuery(
                             new SQLQuery(
                                     selectAllClause,
                                     computeFromClause(resourceTypeName[0]),
                                     new WhereClause(predicate)),
                             resourceTypeName[0], resourceTypeName[1]));
            predicate = null;
            return;
        }

        String resourceType = null;
        String resourceName = null;

        Predicate extracted = null;
        while (Operator.AND.equals(predicate.operator)) {
            Predicate current = (Predicate) predicate.rvalue;
            Predicate next = (Predicate) predicate.lvalue;

            if  (isJoin(current)) {
                if (resourceType == null) {
                    throw new QueryException(
                            "join predicate appears before sub query " +
                            "resource type has been infered");
                }
                if (!Operator.EQ.equals(current.operator)) {
                    throw new QueryException(
                            "Operator in join clause must be '='");
                }

                String leftField = ((Reference) current.lvalue).name;
                String rightField = ((Reference) current.rvalue).name;
                String[] leftTypeAndName = resourceTypeOf(leftField);
                String[] rightTypeAndName = resourceTypeOf(rightField);

                String joinSubName;
                String mainJoinField;
                String subJoinField;
                if (isMainType(rightTypeAndName[0])) {
                    subJoinField = leftField;
                    joinSubName = leftTypeAndName[1];
                    mainJoinField = rightField;
                } else if (isMainType(leftTypeAndName[0])) {
                    subJoinField = rightField;
                    joinSubName = rightTypeAndName[1];
                    mainJoinField = leftField;
                } else {
                    throw new QueryException("Can't join between two non " +
                            "document resources");
                }
                predicate = next;
                subQueries.add(new SubQuery(
                                new SQLQuery(selectAllClause,
                                     computeFromClause(resourceType),
                                     new WhereClause(extracted)),
                                resourceType, resourceName));
                String[] joinInfo = {mainJoinField, subJoinField};
                joins.put(joinSubName, joinInfo);
                return;
            }

            if (resourceType == null) {
                String[] tn = resourceTypeOf(current);
                resourceType = tn[0];
                resourceName = tn[1];
            }

            if (extracted == null) {
                extracted = current;
            } else {
                extracted = new Predicate(current, Operator.AND, extracted);
            }
            predicate = next;
        }
    }

    /**
    * Writes from clauses sub queries.
    *
    * TODO Relies on some coincidental behaviour in backend
    */
    private FromClause computeFromClause(String resourceType) {
        // TODO Auto-generated method stub
        if (isMainType(resourceType)) {
            return from;
        }
        return matchAllFromClause; // TODO here
    }

    // TODO To be updated once the service doesn't lookup confs on prefix
    private String[] resourceTypeOf(String field) throws QueryException {
        String[] splitField = field.split(":");
        if (splitField.length != 2) {
            throw new QueryException("Invalid field name " + field);
        }

        IndexableResourceConf conf =
            service.getIndexableResourceConfByPrefix(splitField[0], false);
        if (conf == null) {
            throw new QueryException("Couldn't find resource conf for " + field);
        }

        String[] res = new String[2];
        res[0] = conf.getType();
        res[1] = conf.getName();
        return res;
    }

    /** Infers the resource type and name of given predicate.
     *
     * <p>Assumes that the predicate
     * if homogeneous, which is right within the scope of NXP 1348
     * </p>
     * @param predicate
     * @return
     * @throws QueryException
     */
    private String[] resourceTypeOf(Predicate predicate) throws QueryException {
        // top-level is enough to tell
        Operand left = predicate.lvalue;
        if (left instanceof Reference) {
            return resourceTypeOf( ((Reference) left).name);
        }
        Operand right = predicate.lvalue;
        if (right instanceof Reference) {
            return resourceTypeOf( ((Reference) right).name);
        }

        // recurse
        if (left instanceof Predicate) {
            String[] under = resourceTypeOf((Predicate)left);
            if (under != null) {
                return under;
            }
        }
        if (right instanceof Predicate) {
            String[] under = resourceTypeOf((Predicate)right);
            if (under != null) {
                return under;
            }
        }

        // don't know, tell upstairs
        return null;
    }

    static boolean isMainType(String resourceType) {
        return ResourceType.SCHEMA.equals(resourceType) ||
            ResourceType.DOC_BUILTINS.equals(resourceType);
    }

}

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

package org.nuxeo.ecm.core.search.backend.compass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.compass.core.CompassQuery;
import org.compass.core.CompassQueryBuilder;
import org.compass.core.CompassSession;
import org.compass.core.CompassQuery.SortDirection;
import org.compass.core.CompassQueryBuilder.CompassBooleanQueryBuilder;
import org.compass.core.CompassQueryBuilder.CompassQueryStringBuilder;
import org.compass.core.lucene.util.LuceneHelper;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.security.SecurityFiltering;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.ecm.core.search.backend.compass.lucene.MatchBeforeQuery;

/**
 * One shot object to convert incoming NXQL into CompassQuery.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class QueryConverter {

    private static final String PER_PROP_ESCAPE = "[\\:]";

    private final CompassSession session;

    private final SearchServiceInternals searchService;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(CompassBackend.class);

    public QueryConverter(CompassSession session,
            SearchServiceInternals searchService) {
        this.session = session;
        this.searchService = searchService;
    }

    private static void addIfNotNull(List<CompassQuery> l, CompassQuery query) {
        if (query != null) {
            l.add(query);
        }
    }

    public CompassQuery toCompassQuery(SQLQuery query, SearchPrincipal principal)
            throws QueryException {
        SelectClause select = query.getSelectClause();
        // TODO Not yet supported select clauses
        if (select.distinct || !select.elements.isEmpty()) {
            throw new QueryException("Not supported SELECT clause in query "
                    + query.toString());
        }

        List<CompassQuery> subQueries = new ArrayList<CompassQuery>();
        addIfNotNull(subQueries, fromClause(query.getFromClause()));
        addIfNotNull(subQueries, whereClause(query.getWhereClause()));
        addIfNotNull(subQueries, makeSecurityQuery(principal));

        CompassQuery cQuery;
        CompassQueryBuilder builder = session.queryBuilder();
        if (subQueries.isEmpty()) {
            cQuery = builder.matchAll();
        } else if (subQueries.size() == 1) {
            cQuery = subQueries.get(0);
        } else {
            CompassBooleanQueryBuilder bbuilder = builder.bool();
            for (CompassQuery sQuery : subQueries) {
                bbuilder.addMust(sQuery);
            }
            cQuery = bbuilder.toQuery();
        }

        // ORDER BY
        OrderByClause order = query.getOrderByClause();
        if (order != null) {
            SortDirection direction = order.isDescendent ? SortDirection.REVERSE
                    : SortDirection.AUTO;
            for (Reference elt : order.elements) {
                String orderField = elt.name;
                IndexableResourceDataConf propConf = searchService.getIndexableDataConfFor(orderField);

                if (propConf != null) {
                    if (propConf.isSortable()) {
                        orderField += Util.SORTABLE_FIELD_SUFFIX;
                    } else if (propConf.getIndexingType().toLowerCase().equals(
                            "text")) {
                        throw new QueryException(String.format(
                                "'%s' is a text field that has "
                                        + "not been declared as sortable",
                                orderField));
                    }
                }
                cQuery.addSort(orderField, direction);
            }
        }
        return cQuery;
    }

    private CompassQuery whereClause(WhereClause clause) throws QueryException {
        if (clause == null) {
            return null;
        }
        return wherePredicate(clause.predicate);
    }

    public CompassQuery fromClause(FromClause from) throws QueryException {
        // First step: using document type as virtual table
        if (from.count() != 1) {
            throw new QueryException("Not supported FROM clause in query "
                    + from.toString());
        }
        String docType = from.get(0);
        if (docType.equals(TypeConstants.DOCUMENT)) {
            return null;
        }

        Set<String> docTypes = searchService.getDocumentTypeNamesExtending(docType);
        if (docTypes == null) {
            throw new QueryException("Unknown core document type: " + docType);
        }

        CompassBooleanQueryBuilder bbuilder = session.queryBuilder().bool();
        for (String t : searchService.getDocumentTypeNamesExtending(docType)) {
            bbuilder.addShould(LuceneHelper.createCompassQuery(session,
                    new TermQuery(new Term(
                            BuiltinDocumentFields.FIELD_DOC_TYPE, t))));
        }
        return bbuilder.toQuery();
    }

    /**
     * Direct query string for experts.
     *
     * @param query
     * @param principal
     * @return The wrapped query, intersected with security query
     * @throws QueryException if the list of permissions to query cannot be fetched
     */
    public CompassQuery toCompassQuery(String query, SearchPrincipal principal) throws QueryException {
        CompassQueryBuilder builder = session.queryBuilder();

        CompassQuery sQuery = builder.queryString(query).toQuery();
        if (principal == null) {
            return sQuery;
        }

        CompassBooleanQueryBuilder bbuilder = session.queryBuilder().bool();
        bbuilder.addMust(sQuery);
        bbuilder.addMust(makeSecurityQuery(principal));
        return bbuilder.toQuery();
    }

    /**
     * Makes a security query for the given principal, on the default security
     * indexing field with the default list of relevant permissions.
     *
     * @param principal
     * @return the security query, wrapped as a CompassQuery
     * @throws QueryException if the list of permissions to query cannot be fetched
     */
    public CompassQuery makeSecurityQuery(SearchPrincipal principal) throws QueryException {
        try {
            return makeSecurityQuery(principal, SecurityFiltering.getBrowsePermissionList(),
                    BuiltinDocumentFields.FIELD_ACP_INDEXED);
        } catch (Throwable t) {
            throw new QueryException(t);
        }
    }

    /**
     * Builds a query for given principal, to check the given list of perms in
     * given indexing field.
     * <p>
     * This is translated in a {@Link MatchBeforeQuery} to require a positive
     * occurence of one of the principal's security tokens (name or group),
     * paired with one of the relevant permissions before any negative one.
     * <p>
     * Of course the field has to have been constructed accordingly.
     *
     * @param principal the principal to check
     * @param perms the list of relevant permissions (atomic or group)
     * @param field the security indexing field
     * @return the security query, wrapped as a CompassQuery or null
     */
    public CompassQuery makeSecurityQuery(SearchPrincipal principal,
            List<String> perms, String field) {

        // If no search principal or a system user then no security
        // restrictions.
        if (principal == null || principal.isSystemUser()) {
            return null;
        }

        // building just once the list of all security tokens
        String[] groups = principal.getGroups();
        List<String> tokens = new ArrayList<String>(groups.length + 1);
        tokens.add(principal.getName());
        tokens.addAll(Arrays.asList(groups));

        int pairsNb = (groups.length + 1) * perms.size();
        String[] required = new String[pairsNb];
        String[] excluded = new String[pairsNb];

        String sep = SecurityFiltering.SEPARATOR;
        int i = 0;
        for (String perm : perms) {
            // TODO escape separator
            for (String token : tokens) {
                required[i] = '+' + token + sep + perm;
                excluded[i++] = '-' + token + sep + perm;
            }
        }

        return LuceneHelper.createCompassQuery(session, new MatchBeforeQuery(
                field, Arrays.asList(required), Arrays.asList(excluded)));
    }

    /**
     * Takes a terminal (leaf) where clause and produces the corresponding
     * CompassQuery.
     * <p>
     * Must not depend on searchEngine to stay unit testable without it.
     *
     * @param op The operator
     * @param right Right hand side of the clause
     * @param analyzer if necessary
     * @param type the indexing type. Case insensitive.
     * @return a compass Query or null to mean it's logically equivalent to a
     *         MatchAllDocQuery
     * @throws QueryException
     */
    public CompassQuery atomicWhereClause(Operator op, String name,
            Operand right, String analyzer, String type) throws QueryException {
        if (type != null) {
            type = type.toLowerCase();
        }
        CompassQueryBuilder builder = session.queryBuilder();

        // factorized of type resolution
        boolean isDate = "date".equals(type); // quicker than instanceof
        boolean isString = !isDate && (right instanceof StringLiteral);

        if (BuiltinDocumentFields.FIELD_FULLTEXT.equals(name)) {
            FulltextFieldDescriptor desc = searchService.getFullTextDescriptorByName(name);
            if (desc != null) { // e.g, in some unit tests
                analyzer = desc.getAnalyzer();
            }
            name = Util.COMPASS_FULLTEXT;
            type = "text"; // Would be later overriden if special conf
        }

        if (analyzer == null) {
            analyzer = "default"; // Will be used for text fields only
        }

        // Special cases for facets
        if (BuiltinDocumentFields.FIELD_DOC_FACETS.equals(name)) {
            if (!op.equals(Operator.EQ) && !op.equals(Operator.IN)) {
                throw new QueryException(String.format(
                        "Can't query on %s with operator %s", name, op));
            }
            Set<String> docTypes;
            if (right instanceof StringLiteral) {
                docTypes = searchService.getDocumentTypeNamesForFacet(((StringLiteral) right).value);
            } else if (right instanceof LiteralList) {
                List<String> facets = new LinkedList<String>();
                for (Literal lit : (LiteralList) right) {
                    facets.add(((StringLiteral) lit).value);
                }
                docTypes = searchService.getDocumentTypeNamesForFacet(facets);
            } else {
                throw new QueryException("Wrong operand for query on " + name);
            }
            if (docTypes == null) { // Maybe a bit harsh
                throw new QueryException("No document types correspond "
                        + "to specified facets");
            }
            LiteralList newRight = new LiteralList();
            for (String docType : docTypes) {
                newRight.add(new StringLiteral(docType));
            }
            return atomicWhereClause(Operator.IN,
                    BuiltinDocumentFields.FIELD_DOC_TYPE, newRight, null, null);
        }

        Object rightOb = null;
        if (isDate) {
            if (right instanceof DateLiteral) {

                DateLiteral literal = (DateLiteral) right;
                DateTime date = literal.value;

                if (literal.onlyDate) {
                    if (op.equals(Operator.EQ)) {
                        return betweenQuery(name, type, date, date.plusDays(1),
                                true, false);
                    }
                    if (op.equals(Operator.LTEQ)) {
                        op = Operator.LT;
                        date = date.plusDays(1);
                    } else if (op.equals(Operator.GT)) {
                        op = Operator.GTEQ;
                        date = date.plusDays(1);
                    } else {
                        // Remaining valid operators
                        // (nothing to be done for them)
                        if (!op.equals(Operator.GTEQ)
                                && !op.equals(Operator.LT)) {
                            throw new QueryException(
                                    "Unsupported binary operator for DATE :"
                                            + op.toString());
                        }
                    }
                }
                rightOb = date.toGregorianCalendar();
            } else {
                // XXX Clearly not the best way to handle empty date query
                // string
                if (((StringLiteral) right).value.equals("")) {
                    rightOb = Util.NULL_MARKER;
                } else {
                    throw new QueryException("Date literal was expected. Got "
                            + ((StringLiteral) right).value + " instead");
                }
            }
        }
        if (isString) {
            rightOb = ((StringLiteral) right).value;
        }

        if ("boolean".equals(type)) {
            long val = ((IntegerLiteral) right).value;
            if (val == 0L) {
                rightOb = false;
            } else if (val == 1L) {
                rightOb = true;
            } else {
                throw new QueryException(
                        "A boolean field can be queried on 0 and 1 only");
            }
        } else if (right instanceof IntegerLiteral) {
            if ("int".equals(type) || "long".equals(type)) {
                rightOb = ((IntegerLiteral) right).value;
            } else {
                rightOb = ((IntegerLiteral) right).value;
            }
        }

        // STARTSWITH for paths boils down to a EQ
        // thanks to special indexing treatment
        // and the way EQ itself behaves
        if (op.equals(Operator.STARTSWITH)) {
            if (name.equals(BuiltinDocumentFields.FIELD_DOC_PATH)
                    || type.equals("path")) {
                String path = (String) rightOb;
                if (path.length() == 0 || "/".equals(path)) {
                    return null;
                }
                // TODO in Nuxeo, all indexed paths turn out to start with a
                // slash. So queries without a leading slash won't return
                // anything. What to do ? WARN log ? automatic add ?
                op = Operator.EQ;
            } else if (type.equals("text") || type.equals("keyword")) {
                // TODO apply Lucene escape
                String value = (String) rightOb + '*';
                return compassQueryString(name, value, analyzer);
            }
        }
        if (op.equals(Operator.EQ)) {
            // try and feed converters

            if (!isString) {
                try {
                    return builder.term(name, rightOb);
                } catch (NullPointerException e) {
                    log.error("Failed building query on property " + name);
                    e.printStackTrace();
                    throw new QueryException(e.getMessage(), e);
                }
            }

            // String handling
            // TODO this should not depend on backend: mutualize at
            // least at service level
            String value = Util.escapeSpecialMarkers((String) rightOb);
            if (name.equals(BuiltinDocumentFields.FIELD_DOC_REF)
                    || name.equals(BuiltinDocumentFields.FIELD_DOC_PARENT_REF)) {
                value = 'i' + value; // will match id refs only
            }

            if ("text".equals(type)) {
                return builder.queryString(
                        String.format("%s:(\"%s\")", name.replaceAll(
                                PER_PROP_ESCAPE, "\\\\$0"), value.replaceAll(
                                PER_PROP_ESCAPE, "\\\\$0"))).setAnalyzer(
                        analyzer).toQuery();
            }

            // Using plain old Lucene term by default
            TermQuery lQuery = new TermQuery(new Term(name, value));
            return LuceneHelper.createCompassQuery(session, lQuery);
        }

        if (op.equals(Operator.LIKE)) {
            if (!isString) {
                throw new QueryException(
                        String.format(
                                "LIKE not supported for %s (only on string based fields)",
                                name));
            }
            return compassQueryString(name, (String) rightOb, analyzer);
        }

        if (op.equals(Operator.IN)) {
            // TODO only lists of String Literal are actually supported
            LiteralList value = (LiteralList) right;
            BooleanQuery lQuery = new BooleanQuery();
            for (Literal l : value) {
                String lv = Util.escapeSpecialMarkers(((StringLiteral) l).value);
                lQuery.add(new BooleanClause(new TermQuery(new Term(name, lv)),
                        BooleanClause.Occur.SHOULD));
            }
            return LuceneHelper.createCompassQuery(session, lQuery);
        }

        if (op.equals(Operator.GT)) {
            return builder.gt(name, rightOb);
        }
        if (op.equals(Operator.GTEQ)) {
            return builder.ge(name, rightOb);
        }
        if (op.equals(Operator.LT)) {
            return builder.lt(name, rightOb);
        }
        if (op.equals(Operator.LTEQ)) {
            return builder.le(name, rightOb);
        }

        throw new QueryException("Unsupported WHERE operator " + op.toString());
    }

    private CompassQuery compassQueryString(String name, String value,
            String analyzer) {
        value = Util.escapeSpecialMarkers(value);
        CompassQueryStringBuilder sBuilder = session.queryBuilder().queryString(
                String.format("%s:(%s)", name.replaceAll(PER_PROP_ESCAPE,
                        "\\\\$0"), value.replaceAll(PER_PROP_ESCAPE, "\\\\$0")));
        if (analyzer != null) {
            sBuilder.setAnalyzer(analyzer);
        }
        return sBuilder.toQuery();
    }

    /**
     * Creates a BETWEEN Query.
     * <p>
     * Dates have to be DateTime instances.
     *
     * @param name the field name
     * @param type must be all lower-case
     * @param left
     * @param right
     * @param leftInclusive
     * @param rightInclusive
     *
     * @return
     */
    private CompassQuery betweenQuery(String name, String type, Object left,
            Object right, boolean leftInclusive, boolean rightInclusive) {

        // Some conversions before feeding Compass converters.
        // Note: can't use a converter for this because on indexing
        // we have a different class (this is bad)
        if ("date".equals(type)) {
            left = ((DateTime) left).toGregorianCalendar();
            right = ((DateTime) right).toGregorianCalendar();
        }
        CompassQueryBuilder builder = session.queryBuilder();

        if (leftInclusive == rightInclusive) {
            return builder.between(name, left, right, leftInclusive);
        }

        CompassBooleanQueryBuilder bbuilder = builder.bool();
        if (leftInclusive) {
            bbuilder.addMust(builder.ge(name, left));
        } else {
            bbuilder.addMust(builder.gt(name, left));
        }
        if (rightInclusive) {
            bbuilder.addMust(builder.le(name, right));
        } else {
            bbuilder.addMust(builder.lt(name, right));
        }
        return bbuilder.toQuery();
    }

    /**
     * Transforms a where predicate into a {@link CompassQuery}. Returns null
     * if the predicate matches everything.
     *
     * @param predicate
     * @return the resulting CompassQuery or null
     * @throws QueryException
     */
    private CompassQuery wherePredicate(Predicate predicate)
            throws QueryException {
        Operator op = predicate.operator;
        Operator notOp = null;

        // Negative operators
        if (op.equals(Operator.NOTLIKE)) {
            notOp = Operator.LIKE;
        } else if (op.equals(Operator.NOTEQ)) {
            notOp = Operator.EQ;
        } else if (op.equals(Operator.NOTIN)) {
            notOp = Operator.IN;
        } else if (op.equals(Operator.NOTBETWEEN)) {
            notOp = Operator.BETWEEN;
        } else if (op.equals(Operator.NOT)) { // generic NOT
                return negateQuery(wherePredicate((Predicate) predicate.lvalue));
        }
        if (notOp != null) {
            return negateQuery(wherePredicate(new Predicate(predicate.lvalue,
                    notOp, predicate.rvalue)));
        }

        CompassQueryBuilder builder = session.queryBuilder();

        // Other boolean queries
        if (op.equals(Operator.AND) || op.equals(Operator.OR)) {
            // treat subqueries, extracting the NOT information
            Predicate leftPred = (Predicate) predicate.lvalue;
            Predicate rightPred = (Predicate) predicate.rvalue;
            Boolean leftnot = leftPred.operator.equals(Operator.NOT);
            Boolean rightnot = rightPred.operator.equals(Operator.NOT);
            if (leftnot) {
                leftPred = (Predicate) leftPred.lvalue;
            }
            if (rightnot) {
                rightPred = (Predicate) rightPred.lvalue;
            }
            CompassQuery left = wherePredicate(leftPred);
            CompassQuery right = wherePredicate(rightPred);

            // insert subqueries
            CompassBooleanQueryBuilder bbuilder = builder.bool();
            boolean empty = true;
            if (op.equals(Operator.AND)) {
                if (leftnot) {
                    if (right == null || left == null) {
                        // AND NOT (all) -> same as NOT (all) (no match)
                        return negateQuery(left);
                    }
                    bbuilder.addMustNot(left);
                    empty = false;
                } else {
                    if (left != null) {
                        bbuilder.addMust(left);
                        empty = false;
                    }
                }

                if (rightnot) {
                    if (right == null || left == null) {
                        // same as above (factorize this)
                        return negateQuery(right);
                    }
                    bbuilder.addMustNot(right);
                    empty = false;
                } else {
                    if (right != null) {
                        bbuilder.addMust(right);
                        empty = false;
                    }
                }
            } else { // op is OR
                if (leftnot && rightnot) {
                    // NOT A OR NOT B = NOT (A AND B)
                    // probably better performance-wise
                    if (left == null && right == null) {
                        return negateQuery(null);
                    }
                    if (left != null) {
                        bbuilder.addMust(left);
                    }
                    if (right != null) {
                        bbuilder.addMust(right);
                    }
                    return negateQuery(bbuilder.toQuery());
                }
                if (leftnot) {
                    left = negateQuery(left);
                }
                if (rightnot) {
                    right = negateQuery(right);
                }
                if (left == null || right == null) {
                    return null;
                }
                bbuilder.addShould(left);
                bbuilder.addShould(right);
                empty = false;
            }
            if (empty) {
                return null;
            }

            return bbuilder.toQuery();
        }

        // default to atomic where clause
        String name;

        if (predicate.lvalue instanceof Reference) {
            name = ((Reference) predicate.lvalue).name;
        } else { // e.g STARTSWITH !
            name = ((StringLiteral) predicate.lvalue).value;
        }
        IndexableResourceDataConf propConf = searchService.getIndexableDataConfFor(name);

        if (op.equals(Operator.BETWEEN)) {
            String type;
            if (propConf == null) {
                type = null;
            } else {
                type = propConf.getIndexingType();
            }
            LiteralList operands = (LiteralList) predicate.rvalue;
            return trivalentWhereClause(op, name, operands.get(0),
                    operands.get(1), type);
        }

        if (propConf != null) {
            return atomicWhereClause(op, name, predicate.rvalue,
                    propConf.getIndexingAnalyzer(), propConf.getIndexingType());
        } else {
            // Allow direct search on, e.g, builtins by making
            // unknown fields go through
            return atomicWhereClause(op, name, predicate.rvalue, null, null);
        }
    }

    private CompassQuery negateQuery(CompassQuery query) {
        CompassQueryBuilder builder = session.queryBuilder();
        CompassBooleanQueryBuilder bbuilder = builder.bool();
        bbuilder.addMust(builder.matchAll());
        bbuilder.addMustNot(query);
        return bbuilder.toQuery();
    }

    private CompassQuery trivalentWhereClause(Operator op, String name,
            Literal first, Literal second, String type) throws QueryException {

        if (type != null) {
            type = type.toLowerCase();
        }

        if (!op.equals(Operator.BETWEEN)) {
            throw new QueryException(String.format("Operator %s not supported",
                    op.toString()));
        }

        if (!first.getClass().equals(second.getClass())) {
            throw new QueryException(
                    "Operands must be of same type for BETWEEN");
        }

        if ("date".equals(type)) {
            DateLiteral dl1 = (DateLiteral) first;
            DateLiteral dl2 = (DateLiteral) second;
            if (dl2.onlyDate) {
                // accept up to next day at 0:00
                return betweenQuery(name, type, dl1.value,
                        dl2.value.plusDays(1), true, false);
            }
            return betweenQuery(name, type, dl1.value, dl2.value, true, true);
        }

        if (first instanceof StringLiteral) {
            return betweenQuery(name, type, ((StringLiteral) first).value,
                    ((StringLiteral) second).value, true, true);
        }

        throw new QueryException(String.format(
                "Unsuported BETWEEN statement: %s, %s", first.toString(),
                second.toString()));
    }

}

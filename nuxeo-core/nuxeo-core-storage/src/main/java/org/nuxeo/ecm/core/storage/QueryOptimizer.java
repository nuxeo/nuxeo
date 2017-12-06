/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collector;

import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.FromList;
import org.nuxeo.ecm.core.query.sql.model.IdentityQueryTransformer;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Generic optimizer for a NXQL query.
 *
 * @since 5.9.4
 */
public abstract class QueryOptimizer {

    public static final String TYPE_ROOT = "Root";

    public static final String TYPE_DOCUMENT = "Document";

    public static final String TYPE_RELATION = "Relation";

    protected static final int CORR_BASE = 100_000;

    protected FacetFilter facetFilter;

    protected final SchemaManager schemaManager;

    protected final Set<String> neverPerInstanceMixins;

    protected int correlationCounter;

    /** Do we match only relations? */
    protected boolean onlyRelations;

    // group by prefix, keeping order
    protected static Collector<Expression, ?, Map<String, List<Expression>>> GROUPING_BY_EXPR_PREFIX = groupingBy(
            QueryOptimizer::getExpressionPrefix, LinkedHashMap::new, toList());

    public QueryOptimizer() {
        // schemaManager may be null in unit tests
        schemaManager = Framework.getService(SchemaManager.class);
        Set<String> facets = schemaManager == null ? Collections.emptySet()
                : schemaManager.getNoPerDocumentQueryFacets();
        neverPerInstanceMixins = new HashSet<>(facets);
    }

    public QueryOptimizer withFacetFilter(FacetFilter facetFilter) {
        this.facetFilter = facetFilter;
        return this;
    }

    /**
     * Optimizes a query to provide a WHERE clause containing facet filters, primary and mixin types. In addition, the
     * top-level AND clauses are analyzed to provide prefix info.
     */
    public SQLQuery optimize(SQLQuery query) {
        // rewrite some uncorrelated wildcards and add NOT NULL clauses for projection ones
        query = addWildcardNotNullClauses(query);

        List<Expression> clauses = new ArrayList<>();
        addFacetFilters(clauses, facetFilter);
        addTypes(clauses, query.from);
        addWhere(clauses, query.where);
        simplifyTypes(clauses);
        MultiExpression multiExpression = MultiExpression.fromExpressionList(Operator.AND, clauses);

        // collect information about common reference prefixes (stored on Reference and Expression)
        new ReferencePrefixAnalyzer().visitMultiExpression(multiExpression);

        Expression whereExpr;
        PrefixInfo info = (PrefixInfo) multiExpression.getInfo();
        if (!info.prefix.isEmpty()) {
            // all references have a common prefix
            whereExpr = multiExpression;
        } else {
            // do grouping by common prefix
            Map<String, List<Expression>> grouped = clauses.stream().collect(GROUPING_BY_EXPR_PREFIX);
            Map<String, Expression> groupedExpressions = new LinkedHashMap<>();
            for (Entry<String, List<Expression>> en : grouped.entrySet()) {
                String prefix = en.getKey();
                List<Expression> list = en.getValue();
                groupedExpressions.put(prefix, makeSingleAndExpression(prefix, list));
            }

            // potentially reorganize into nested grouping
            reorganizeGroupedExpressions(groupedExpressions);

            List<Expression> expressions = new ArrayList<>(groupedExpressions.values());
            whereExpr = makeSingleAndExpression("", expressions);
        }

        return query.withWhereExpression(whereExpr);
    }

    /**
     * Makes a single AND expression from several expressions known to have a common prefix.
     */
    public static Expression makeSingleAndExpression(String prefix, List<Expression> exprs) {
        if (exprs.size() == 1) {
            return exprs.get(0);
        } else {
            int count = prefix.isEmpty() ? 0 : exprs.stream().mapToInt(QueryOptimizer::getExpressionCount).sum();
            Expression e = MultiExpression.fromExpressionList(Operator.AND, exprs);
            e.setInfo(new PrefixInfo(prefix, count));
            return e;
        }
    }

    protected static String getExpressionPrefix(Expression expr) {
        PrefixInfo info = (PrefixInfo) expr.getInfo();
        return info == null ? "" : info.prefix;
    }

    protected static int getExpressionCount(Expression expr) {
        PrefixInfo info = (PrefixInfo) expr.getInfo();
        return info == null ? 0 : info.count;
    }

    /**
     * Reorganizes the grouped expressions in order to have 2-level nesting in case a group is a prefix of another.
     *
     * @since 9.3
     */
    public static void reorganizeGroupedExpressions(Map<String, Expression> groupedExpressions) {
        if (groupedExpressions.size() > 1) {
            List<String> keys = new ArrayList<>(groupedExpressions.keySet());
            List<String> withPrefix = new ArrayList<>();
            String prefix = findPrefix(keys, withPrefix);
            if (prefix != null) {
                // first part, the expression corresponding to the prefix
                Expression first = groupedExpressions.remove(prefix);

                // second part, all those that had that prefix
                List<Expression> exprs = new ArrayList<>();
                for (String k : withPrefix) {
                    exprs.add(groupedExpressions.remove(k));
                }
                String secondPrefix;
                if (withPrefix.size() == 1) {
                    secondPrefix = withPrefix.get(0);
                } else {
                    throw new QueryParseException("Too complex correlated wildcards in query: " + groupedExpressions);
                }
                Expression second = makeSingleAndExpression(secondPrefix, exprs);

                // finally bring them all together
                Expression expr = makeSingleAndExpression(prefix, Arrays.asList(first, second));
                groupedExpressions.put(prefix, expr);
            }
        }
    }

    /**
     * Finds a non-empty prefix in the strings.
     * <p>
     * If a prefix is found, the other strings having it as a prefix are collected in {@code withPrefix}, and the prefix
     * and the found strings are moved from the input {@code string}.
     * <p>
     * {@code strings} and {@code withPrefix} must both be ArrayLists as they will be mutated and queried by index.
     *
     * @param strings the input strings
     * @param withPrefix (return value) the strings that have the found prefix as a prefix
     * @return the prefix if found, or null if not
     * @since 9.3
     */
    public static String findPrefix(List<String> strings, List<String> withPrefix) {
        // naive algorithm as list size is very small
        String prefix = null;
        ALL: //
        for (int i = 0; i < strings.size(); i++) {
            String candidate = strings.get(i);
            if (candidate.isEmpty()) {
                continue;
            }
            for (int j = 0; j < strings.size(); j++) {
                if (i == j) {
                    continue;
                }
                String s = strings.get(j);
                if (s.isEmpty()) {
                    continue;
                }
                if (s.startsWith(candidate + '/')) {
                    prefix = candidate;
                    break ALL;
                }
            }
        }
        if (prefix != null) {
            for (Iterator<String> it = strings.iterator(); it.hasNext();) {
                String s = it.next();
                if (s.isEmpty()) {
                    continue;
                }
                if (s.equals(prefix)) {
                    it.remove(); // will be returned as method result
                    continue;
                }
                if (s.startsWith(prefix + '/')) {
                    it.remove(); // will be returned in withPrefix
                    withPrefix.add(s);
                }
            }
        }
        return prefix;
    }

    protected void addFacetFilters(List<Expression> clauses, FacetFilter facetFilter) {
        if (facetFilter == null) {
            return;
        }
        for (String mixin : facetFilter.required) {
            // every facet is required, not just any of them,
            // so do them one by one
            Expression expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE), Operator.EQ, new StringLiteral(mixin));
            clauses.add(expr);
        }
        if (!facetFilter.excluded.isEmpty()) {
            LiteralList list = new LiteralList();
            for (String mixin : facetFilter.excluded) {
                list.add(new StringLiteral(mixin));
            }
            Expression expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE), Operator.NOTIN, list);
            clauses.add(expr);
        }
    }

    // methods using SchemaManager easily overrideable for easier testing

    protected Set<String> getDocumentTypeNamesForFacet(String mixin) {
        Set<String> types = schemaManager.getDocumentTypeNamesForFacet(mixin);
        if (types == null) {
            // unknown mixin
            types = Collections.emptySet();
        }
        return types;
    }

    protected Set<String> getDocumentTypeNamesExtending(String typeName) {
        Set<String> types = schemaManager.getDocumentTypeNamesExtending(typeName);
        if (types == null) {
            throw new RuntimeException("Unknown type: " + typeName);
        }
        return types;
    }

    protected boolean isTypeRelation(String typeName) {
        do {
            if (TYPE_RELATION.equals(typeName)) {
                return true;
            }
            Type t = schemaManager.getDocumentType(typeName);
            if (t != null) {
                t = t.getSuperType();
            }
            typeName = t == null ? null : t.getName();
        } while (typeName != null);
        return false;
    }

    /**
     * Finds all the types to take into account (all concrete types being a subtype of the passed types) based on the
     * FROM list.
     * <p>
     * Adds them as a ecm:primaryType match in the toplevel operands.
     */
    protected void addTypes(List<Expression> clauses, FromClause node) {
        onlyRelations = true;
        Set<String> fromTypes = new HashSet<>();
        FromList elements = node.elements;
        for (String typeName : elements.values()) {
            if (TYPE_DOCUMENT.equalsIgnoreCase(typeName)) {
                typeName = TYPE_DOCUMENT;
            }
            fromTypes.addAll(getDocumentTypeNamesExtending(typeName));
            boolean isRelation = isTypeRelation(typeName);
            onlyRelations = onlyRelations && isRelation;
        }
        fromTypes.remove(TYPE_ROOT);
        LiteralList list = new LiteralList();
        for (String type : fromTypes) {
            list.add(new StringLiteral(type));
        }
        clauses.add(new Expression(new Reference(NXQL.ECM_PRIMARYTYPE), Operator.IN, list));
    }

    /**
     * Adds a flattened version of all toplevel ANDed WHERE clauses.
     */
    protected void addWhere(List<Expression> clauses, WhereClause where) {
        if (where != null) {
            addWhere(clauses, where.predicate);
        }
    }

    protected void addWhere(List<Expression> clauses, Expression expr) {
        if (expr.operator == Operator.AND && expr.lvalue instanceof Expression && expr.rvalue instanceof Expression) {
            addWhere(clauses, (Expression) expr.lvalue);
            addWhere(clauses, (Expression) expr.rvalue);
        } else if (expr.operator == Operator.AND && expr instanceof MultiExpression) {
            List<Operand> remainingOperands = new ArrayList<>();
            for (Operand oper : ((MultiExpression) expr).values) {
                if (oper instanceof Expression) {
                    addWhere(clauses, (Expression) oper);
                } else {
                    remainingOperands.add(oper);
                }
            }
            if (!remainingOperands.isEmpty()) {
                clauses.add(new MultiExpression(Operator.AND, remainingOperands));
            }
        } else {
            clauses.add(expr);
        }
    }

    /**
     * Simplify ecm:primaryType positive references, and non-per-instance mixin types.
     */
    protected void simplifyTypes(List<Expression> clauses) {
        Set<String> primaryTypes = null; // if defined, required
        for (Iterator<Expression> it = clauses.iterator(); it.hasNext();) {
            // whenever we don't know how to optimize the expression,
            // we just continue the loop
            Expression expr = it.next();
            if (!(expr.lvalue instanceof Reference)) {
                continue;
            }
            String name = ((Reference) expr.lvalue).name;
            Operator op = expr.operator;
            Operand rvalue = expr.rvalue;
            if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                if (op != Operator.EQ && op != Operator.IN) {
                    continue;
                }
                Set<String> set;
                if (op == Operator.EQ) {
                    if (!(rvalue instanceof StringLiteral)) {
                        continue;
                    }
                    String primaryType = ((StringLiteral) rvalue).value;
                    set = new HashSet<>(Collections.singleton(primaryType));
                } else { // Operator.IN
                    if (!(rvalue instanceof LiteralList)) {
                        continue;
                    }
                    set = getStringLiterals((LiteralList) rvalue);
                }
                if (primaryTypes == null) {
                    primaryTypes = set;
                } else {
                    primaryTypes.retainAll(set);
                }
                it.remove(); // expression simplified into primaryTypes set
            } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                if (op != Operator.EQ && op != Operator.NOTEQ) {
                    continue;
                }
                if (!(rvalue instanceof StringLiteral)) {
                    continue;
                }
                String mixin = ((StringLiteral) rvalue).value;
                if (!neverPerInstanceMixins.contains(mixin)) {
                    // mixin per instance -> primary type checks not enough
                    continue;
                }
                Set<String> set = getDocumentTypeNamesForFacet(mixin);
                if (primaryTypes == null) {
                    if (op == Operator.EQ) {
                        primaryTypes = new HashSet<>(set); // copy
                    } else {
                        continue; // unknown positive, no optimization
                    }
                } else {
                    if (op == Operator.EQ) {
                        primaryTypes.retainAll(set);
                    } else {
                        primaryTypes.removeAll(set);
                    }
                }
                it.remove(); // expression simplified into primaryTypes set
            }
        }
        // readd the simplified primary types constraints
        if (primaryTypes != null) {
            if (primaryTypes.isEmpty()) {
                // TODO better removal
                primaryTypes.add("__NOSUCHTYPE__");
            }
            Expression expr;
            if (primaryTypes.size() == 1) {
                String pt = primaryTypes.iterator().next();
                expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE), Operator.EQ, new StringLiteral(pt));
            } else { // primaryTypes.size() > 1
                LiteralList list = new LiteralList();
                for (String pt : primaryTypes) {
                    list.add(new StringLiteral(pt));
                }
                expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE), Operator.IN, list);
            }
            clauses.add(expr);
        }
    }

    protected static Set<String> getStringLiterals(LiteralList list) {
        Set<String> set = new HashSet<>();
        for (Literal literal : list) {
            if (!(literal instanceof StringLiteral)) {
                throw new RuntimeException("requires string literals");
            }
            set.add(((StringLiteral) literal).value);
        }
        return set;
    }

    /**
     * If we have a query like {@code SELECT dc:subjects/* FROM ...} then we must make sure we don't match documents
     * that don't have a {@code dc:subjects} at all, as this would make the evaluator return one {@code null} value for
     * each due to its semantics of doing the equivalent of LEFT JOINs.
     * <p>
     * To prevent this, we add a clause {@code ... AND dc:subjects/* IS NOT NULL}.
     * <p>
     * For correlated wildcards this is enough, but for uncorrelated wildcards we must avoid adding extra JOINs, so we
     * must artificially correlated them. This requires rewriting the query with correlated wildcards instead of
     * uncorrelated ones.
     */
    protected SQLQuery addWildcardNotNullClauses(SQLQuery query) {
        ProjectionWildcardsFinder finder = new ProjectionWildcardsFinder();
        finder.visitQuery(query);

        // find wildcards in the projection
        Set<String> wildcards = finder.projectionWildcards;
        Set<String> uncorrelatedWildcards = finder.uncorrelatedProjectionWildcards;
        if (!uncorrelatedWildcards.isEmpty()) {
            // rename uncorrelated wildcards to unique correlation names
            Map<String, String> map = new HashMap<>(uncorrelatedWildcards.size());
            for (String name : uncorrelatedWildcards) {
                String newName = name + (CORR_BASE + correlationCounter++);
                map.put(name, newName);
                wildcards.remove(name);
                wildcards.add(newName);
            }
            // rename them in the whole query
            query = new ProjectionReferenceRenamer(map).transform(query);
        }

        // add IS NOT NULL clauses for all projection wildcards (now all correlated)
        if (!wildcards.isEmpty()) {
            query = addIsNotNullClauses(query, wildcards);
        }

        return query;
    }

    protected SQLQuery addIsNotNullClauses(SQLQuery query, Collection<String> names) {
        List<Operand> values = names.stream()
                                    .map(name -> new Expression(new Reference(name), Operator.ISNOTNULL, null))
                                    .collect(toList());
        Expression expr = new Expression(query.where.predicate, Operator.AND,
                new MultiExpression(Operator.AND, values));
        return query.withWhereExpression(expr);
    }

    protected static class ProjectionWildcardsFinder extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        protected final Set<String> projectionWildcards = new HashSet<>();

        protected final Set<String> uncorrelatedProjectionWildcards = new HashSet<>();

        protected boolean inProjection;

        protected boolean inOrderBy;

        @Override
        public void visitSelectClause(SelectClause node) {
            inProjection = true;
            super.visitSelectClause(node);
            inProjection = false;
        }

        @Override
        public void visitOrderByClause(OrderByClause node) {
            inOrderBy = true;
            super.visitOrderByClause(node);
            inOrderBy = false;
        }

        @Override
        public void visitReference(Reference ref) {
            if (inProjection) {
                String name = ref.name;
                if (name.endsWith("*")) {
                    projectionWildcards.add(name);
                    if (name.endsWith("/*")) {
                        uncorrelatedProjectionWildcards.add(name);
                    }
                }
            }
        }
    }

    /**
     * Renames references if they are in the projection part.
     */
    protected static class ProjectionReferenceRenamer extends IdentityQueryTransformer {

        protected final Map<String, String> map;

        protected boolean inProjection;

        public ProjectionReferenceRenamer(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public SelectClause transform(SelectClause node) {
            inProjection = true;
            node = super.transform(node);
            inProjection = false;
            return node;
        }

        @Override
        public Reference transform(Reference node) {
            if (!inProjection) {
                return node;
            }
            String name = node.name;
            String newName = map.getOrDefault(name, name);
            Reference newReference = new Reference(newName, node.cast, node.esHint);
            if (newReference.originalName == null) {
                newReference.originalName = name;
            }
            newReference.info = node.info;
            return newReference;
        }
    }

    /**
     * Info about a prefix: the prefix, and how many times it was encountered.
     *
     * @since 9.3
     */
    public static class PrefixInfo {

        public static final PrefixInfo EMPTY = new PrefixInfo("", 0);

        public final String prefix;

        public final int count;

        public PrefixInfo(String prefix, int count) {
            this.prefix = prefix;
            this.count = count;
        }
    }

    /**
     * Analyzes references to compute common prefix info in order to later factor them in a parent expression.
     *
     * @since 9.3
     */
    public class ReferencePrefixAnalyzer extends DefaultQueryVisitor {

        private static final long serialVersionUID = 1L;

        @Override
        public void visitReference(Reference node) {
            super.visitReference(node);
            processReference(node);
        }

        @Override
        public void visitMultiExpression(MultiExpression node) {
            super.visitMultiExpression(node);
            processExpression(node, node.values);
        }

        @Override
        public void visitExpression(Expression node) {
            super.visitExpression(node);
            processExpression(node, Arrays.asList(node.lvalue, node.rvalue));
        }

        protected void processReference(Reference node) {
            String prefix = getCorrelatedWildcardPrefix(node.name);
            int count = prefix.isEmpty() ? 0 : 1;
            node.setInfo(new PrefixInfo(prefix, count));
        }

        protected void processExpression(Expression node, List<Operand> operands) {
            PrefixInfo commonInfo = null;
            for (Operand operand : operands) {
                // find longest prefix for the operand
                PrefixInfo info;
                if (operand instanceof Reference) {
                    Reference reference = (Reference) operand;
                    info = (PrefixInfo) reference.getInfo();
                } else if (operand instanceof Expression) {
                    Expression expression = (Expression) operand;
                    info = (PrefixInfo) expression.getInfo();
                } else {
                    info = null;
                }
                if (info != null) {
                    if (commonInfo == null) {
                        commonInfo = info;
                    } else if (commonInfo.prefix.equals(info.prefix)) {
                        commonInfo = new PrefixInfo(commonInfo.prefix, commonInfo.count + info.count);
                    } else {
                        commonInfo = PrefixInfo.EMPTY;
                    }
                }
            }
            node.setInfo(commonInfo);
        }
    }

    /**
     * Gets the prefix to use for this reference name (NXQL) if it contains a correlated wildcard.
     * <p>
     * The prefix is used to group together sets of expression that all use references with the same prefix.
     *
     * @param name the reference name (NXQL)
     * @return the prefix, or an empty string if there is no correlated wildcard
     * @since 9.3
     */
    public abstract String getCorrelatedWildcardPrefix(String name);

}

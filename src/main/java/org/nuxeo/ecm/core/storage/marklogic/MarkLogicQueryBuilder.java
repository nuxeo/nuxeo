/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.storage.marklogic.MarkLogicHelper.ElementType;
import org.nuxeo.runtime.api.Framework;

import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawQueryDefinition;
import com.marklogic.client.query.RawStructuredQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;

/**
 * Query builder for a MarkLogic query from an {@link Expression}.
 *
 * @since 8.3
 */
class MarkLogicQueryBuilder {

    private static final Long ZERO = 0L;

    private static final Long ONE = 1L;

    protected final SchemaManager schemaManager;

    // non-canonical index syntax, for replaceAll
    private final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+|\\*|\\*\\d+)\\]" // index in brackets
    );

    /** Splits foo/*1/bar into foo/*1, bar with the last bar part optional */
    protected final static Pattern WILDCARD_SPLIT = Pattern.compile("(.*/\\*\\d+)(?:/(.*))?");

    private final QueryManager queryManager;

    private final StructuredQueryBuilder sqb;

    private final Expression expression;

    private final SelectClause selectClause;

    private final OrderByClause orderByClause;

    private final PathResolver pathResolver;

    private final boolean fulltextSearchDisabled;

    private final boolean distinctDocuments;

    private Boolean projectionHasWildcard;

    public MarkLogicQueryBuilder(QueryManager queryManager, Expression expression, SelectClause selectClause,
            OrderByClause orderByClause, PathResolver pathResolver, boolean fulltextSearchDisabled,
            boolean distinctDocuments) {
        this.schemaManager = Framework.getLocalService(SchemaManager.class);
        this.queryManager = queryManager;
        this.sqb = queryManager.newStructuredQueryBuilder();
        this.expression = expression;
        this.selectClause = selectClause;
        this.orderByClause = orderByClause;
        this.pathResolver = pathResolver;
        this.fulltextSearchDisabled = fulltextSearchDisabled;
        this.distinctDocuments = distinctDocuments;
    }

    public boolean doManualProjection() {
        // Don't do manual projection if there are no projection wildcards, as this brings no new
        // information and is costly. The only difference is several identical rows instead of one.
        return !distinctDocuments && hasProjectionWildcard();
    }

    private boolean hasProjectionWildcard() {
        if (projectionHasWildcard == null) {
            projectionHasWildcard = false;
            for (int i = 0; i < selectClause.elements.size(); i++) {
                Operand op = selectClause.elements.get(i);
                if (!(op instanceof Reference)) {
                    throw new QueryParseException("Projection not supported: " + op);
                }
                if (walkReference(op).hasWildcard()) {
                    projectionHasWildcard = true;
                    break;
                }
            }
        }
        return projectionHasWildcard;
    }

    public RawQueryDefinition buildQuery() {
        RawStructuredQueryDefinition query = sqb.build(walkExpression(expression).build(sqb));
        String options = buildOptions();
        String comboQuery = "<search xmlns=\"http://marklogic.com/appservices/search\">" //
                + query.toString() //
                + options //
                + "</search>";
        return queryManager.newRawCombinedQueryDefinition(new StringHandle(comboQuery));
    }

    private String buildOptions() {
        return "<options xmlns=\"http://marklogic.com/appservices/search\">" //
                + buildProjections() //
                + "</options>";
    }

    private String buildProjections() {
        if (doManualProjection()) {
            return "";
        }
        StringBuilder extract = new StringBuilder("<extract-document-data selected=\"include-with-ancestors\">");
        for (int i = 0; i < selectClause.elements.size(); i++) {
            Operand op = selectClause.elements.get(i);
            if (!(op instanceof Reference)) {
                throw new QueryParseException("Projection not supported: " + op);
            }
            FieldInfo fieldInfo = walkReference((Reference) op);
            extract.append("<extract-path>");
            extract.append(MarkLogicHelper.DOCUMENT_ROOT_PATH)
                   .append('/')
                   .append(MarkLogicHelper.serializeKey(fieldInfo.getFullField()));
            extract.append("</extract-path>");
            // TODO check fulltext score case (from mongodb)
        }
        extract.append("</extract-document-data>");
        return extract.toString();
    }

    private QueryBuilder walkExpression(Expression expression) {
        Operator op = expression.operator;
        Operand lvalue = expression.lvalue;
        Operand rvalue = expression.rvalue;
        Reference ref = lvalue instanceof Reference ? (Reference) lvalue : null;
        String name = ref != null ? ref.name : null;
        // TODO handle ref and date cast

        if (op == Operator.STARTSWITH) {
             return walkStartsWith(lvalue, rvalue);
        } else if (NXQL.ECM_PATH.equals(name)) {
             return walkEcmPath(op, rvalue);
            // } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT) && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
            // walkEcmFulltext(name, op, rvalue);
        } else if (op == Operator.SUM) {
            throw new UnsupportedOperationException("SUM");
        } else if (op == Operator.SUB) {
            throw new UnsupportedOperationException("SUB");
        } else if (op == Operator.MUL) {
            throw new UnsupportedOperationException("MUL");
        } else if (op == Operator.DIV) {
            throw new UnsupportedOperationException("DIV");
        } else if (op == Operator.LT) {
            return walkLt(lvalue, rvalue);
        } else if (op == Operator.GT) {
            return walkGt(lvalue, rvalue);
        } else if (op == Operator.EQ) {
            return walkEq(lvalue, rvalue, true);
        } else if (op == Operator.NOTEQ) {
            return walkEq(lvalue, rvalue, false);
        } else if (op == Operator.LTEQ) {
            return walkLtEq(lvalue, rvalue);
        } else if (op == Operator.GTEQ) {
            return walkGtEq(lvalue, rvalue);
        } else if (op == Operator.AND) {
            if (expression instanceof MultiExpression) {
                return walkMultiExpression((MultiExpression) expression);
            } else {
                return walkAnd(lvalue, rvalue);
            }
        } else if (op == Operator.NOT) {
            return walkNot(lvalue);
        } else if (op == Operator.OR) {
            return walkOr(lvalue, rvalue);
        } else if (op == Operator.LIKE) {
            return walkLike(lvalue, rvalue, true, false);
        } else if (op == Operator.ILIKE) {
            return walkLike(lvalue, rvalue, true, true);
        } else if (op == Operator.NOTLIKE) {
            return walkLike(lvalue, rvalue, false, false);
        } else if (op == Operator.NOTILIKE) {
            return walkLike(lvalue, rvalue, false, true);
        } else if (op == Operator.IN) {
            return walkIn(lvalue, rvalue, true);
        } else if (op == Operator.NOTIN) {
            return walkIn(lvalue, rvalue, false);
        } else if (op == Operator.ISNULL) {
            return walkNull(lvalue, true);
        } else if (op == Operator.ISNOTNULL) {
            return walkNull(lvalue, false);
        } else if (op == Operator.BETWEEN) {
            // walkBetween(lvalue, rvalue, true);
        } else if (op == Operator.NOTBETWEEN) {
            // walkBetween(lvalue, rvalue, false);
        }
        throw new QueryParseException("Unknown operator: " + op);
    }

    public QueryBuilder walkStartsWith(Operand lvalue, Operand rvalue) {
        if (!(lvalue instanceof Reference)) {
            throw new QueryParseException("Invalid STARTSWITH query, left hand side must be a property: " + lvalue);
        }
        String name = ((Reference) lvalue).name;
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(
                    "Invalid STARTSWITH query, right hand side must be a literal path: " + rvalue);
        }
        String path = ((StringLiteral) rvalue).value;
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (NXQL.ECM_PATH.equals(name)) {
            return walkStartsWithPath(path);
        } else {
            return walkStartsWithNonPath(lvalue, path);
        }
    }

    protected QueryBuilder walkStartsWithPath(String path) {
        // resolve path
        String ancestorId = pathResolver.getIdForPath(path);
        if (ancestorId == null) {
            // no such path
            // TODO XXX do better
            return walkNull(new Reference(NXQL.ECM_UUID), true);
        }
        return walkEq(new Reference(ExpressionEvaluator.NXQL_ECM_ANCESTOR_IDS), new StringLiteral(ancestorId), true);
    }

    protected QueryBuilder walkStartsWithNonPath(Operand lvalue, String path) {
        // Rebuild an OR expression, it means :
        // - build an equal Operand
        // - build a like Operand starting after the '/'
        Expression equalOperand = new Expression(lvalue, Operator.EQ, new StringLiteral(path));
        Expression likeOperand = new Expression(lvalue, Operator.LIKE, new StringLiteral(path + "/%"));
        return walkOr(equalOperand, likeOperand);
    }

    private QueryBuilder walkEcmPath(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_PATH + " requires = or <> operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_PATH + " requires literal path as right argument");
        }
        String path = ((StringLiteral) rvalue).value;
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String id = pathResolver.getIdForPath(path);
        if (id == null) {
            // no such path
            // TODO XXX do better
            return walkNull(new Reference(NXQL.ECM_UUID), true);
        }
        return walkEq(new Reference(NXQL.ECM_UUID), new StringLiteral(id), op == Operator.EQ);
    }

    private QueryBuilder walkNot(Operand lvalue) {
        QueryBuilder query = walkOperandAsExpression(lvalue);
        query.not();
        return query;
    }

    private QueryBuilder walkEq(Operand lvalue, Operand rvalue, boolean equals) {
        FieldInfo leftInfo = walkReference(lvalue);
        if (leftInfo.isMixinTypes()) {
            if (!(rvalue instanceof StringLiteral)) {
                throw new QueryParseException("Invalid EQ rhs: " + rvalue);
            }
            // TODO walk mixin types.
        }
        Literal convertedLiteral = convertIfBoolean(leftInfo, (Literal) rvalue);
        // If the literal is null it could be :
        // - test for non existence of boolean field
        // - a platform issue
        if (convertedLiteral == null) {
            return walkNull(lvalue, equals);
        }
        return getQueryBuilder(leftInfo, name -> new EqualQueryBuilder(name, convertedLiteral, equals));
    }

    private QueryBuilder walkLt(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo, name -> new RangeQueryBuilder(name, StructuredQueryBuilder.Operator.LT,
                (Literal) rvalue));
    }

    private QueryBuilder walkGt(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo, name -> new RangeQueryBuilder(name, StructuredQueryBuilder.Operator.GT,
                (Literal) rvalue));
    }

    private QueryBuilder walkLtEq(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo, name -> new RangeQueryBuilder(name, StructuredQueryBuilder.Operator.LE,
                (Literal) rvalue));
    }

    private QueryBuilder walkGtEq(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo, name -> new RangeQueryBuilder(name, StructuredQueryBuilder.Operator.GE,
                (Literal) rvalue));
    }

    private QueryBuilder walkMultiExpression(MultiExpression expression) {
        return walkAnd(expression.values);
    }

    private QueryBuilder walkAnd(Operand lvalue, Operand rvalue) {
        return walkAnd(Arrays.asList(lvalue, rvalue));
    }

    private QueryBuilder walkAnd(List<Operand> values) {
        List<QueryBuilder> children = walkOperandAsExpression(values);
        // Check wildcards in children in order to perform correlated constraints
        Map<String, List<QueryBuilder>> propBaseToBuilders = new LinkedHashMap<>();
        for (Iterator<QueryBuilder> it = children.iterator(); it.hasNext();) {
            QueryBuilder child = it.next();
            if (child instanceof CorrelatedContainerQueryBuilder) {
                CorrelatedContainerQueryBuilder queryBuilder = (CorrelatedContainerQueryBuilder) child;
                String correlatedPath = queryBuilder.getPath();
                // Store object for this key
                List<QueryBuilder> propBaseBuilders = propBaseToBuilders.get(correlatedPath);
                if (propBaseBuilders == null) {
                    propBaseToBuilders.put(correlatedPath, propBaseBuilders = new LinkedList<>());
                }
                propBaseBuilders.add(queryBuilder.getChild());
                it.remove();
            }
        }
        for (Entry<String, List<QueryBuilder>> entry : propBaseToBuilders.entrySet()) {
            String correlatedPath = entry.getKey();
            List<QueryBuilder> propBaseBuilders = entry.getValue();
            // Build the composition query builder
            QueryBuilder queryBuilder;
            if (propBaseBuilders.size() == 1) {
                queryBuilder = propBaseBuilders.get(0);
            } else {
                queryBuilder = new CompositionQueryBuilder(propBaseBuilders, true);
            }
            // Build upper container
            children.add(new CorrelatedContainerQueryBuilder(correlatedPath, queryBuilder));
        }
        if (children.size() == 1) {
            return children.get(0);
        }
        return new CompositionQueryBuilder(children, true);
    }

    private QueryBuilder walkOr(Operand lvalue, Operand rvalue) {
        return walkOr(Arrays.asList(lvalue, rvalue));
    }

    private QueryBuilder walkOr(List<Operand> values) {
        List<QueryBuilder> children = walkOperandAsExpression(values);
        if (children.size() == 1) {
            return children.get(0);
        }
        return new CompositionQueryBuilder(children, false);
    }

    private QueryBuilder walkLike(Operand lvalue, Operand rvalue, boolean positive, boolean caseInsensitive) {
        FieldInfo leftInfo = walkReference(lvalue);
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException("Invalid LIKE/ILIKE, right hand side must be a string: " + rvalue);
        }
        return getQueryBuilder(leftInfo, name -> new LikeQueryBuilder(name, (StringLiteral) rvalue, positive,
                caseInsensitive));
    }

    private QueryBuilder walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        if (!(rvalue instanceof LiteralList)) {
            throw new QueryParseException("Invalid IN, right hand side must be a list: " + rvalue);
        }
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo, name -> new InQueryBuilder(name, (LiteralList) rvalue, positive));
    }

    private QueryBuilder walkNull(Operand lvalue, boolean isNull) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo, name -> new IsNullQueryBuilder(name, isNull));
    }

    /**
     * Method used to walk on a list of {@link Expression} typed as {@link Operand}.
     */
    private List<QueryBuilder> walkOperandAsExpression(List<Operand> operands) {
        return operands.stream().map(this::walkOperandAsExpression).collect(Collectors.toList());
    }

    /**
     * Method used to walk on an {@link Expression} typed as {@link Operand}.
     */
    private QueryBuilder walkOperandAsExpression(Operand operand) {
        if (!(operand instanceof Expression)) {
            throw new IllegalArgumentException("Operand " + operand + "is not an Expression.");
        }
        return walkExpression((Expression) operand);
    }

    private FieldInfo walkReference(Operand value) {
        if (!(value instanceof Reference)) {
            throw new QueryParseException("Invalid query, left hand side must be a property: " + value);
        }
        return walkReference((Reference) value);
    }

    private FieldInfo walkReference(Reference reference) {
        String name = reference.name;
        String prop = canonicalXPath(name);
        String[] parts = prop.split("/");
        if (prop.startsWith(NXQL.ECM_PREFIX)) {
            if (prop.startsWith(NXQL.ECM_ACL + "/")) {
                 return parseACP(prop, parts);
            }
            String field = DBSSession.convToInternal(prop);
            return new FieldInfo(prop, field);
        }

        // Copied from Mongo

        String first = parts[0];
        Field field = schemaManager.getField(first);
        if (field == null) {
            if (first.indexOf(':') > -1) {
                throw new QueryParseException("No such property: " + name);
            }
            // check without prefix
            // TODO precompute this in SchemaManagerImpl
            for (Schema schema : schemaManager.getSchemas()) {
                if (!StringUtils.isBlank(schema.getNamespace().prefix)) {
                    // schema with prefix, do not consider as candidate
                    continue;
                }
                if (schema != null) {
                    field = schema.getField(first);
                    if (field != null) {
                        break;
                    }
                }
            }
            if (field == null) {
                throw new QueryParseException("No such property: " + name);
            }
        }
        Type type = field.getType();
        // canonical name
        parts[0] = field.getName().getPrefixedName();
        // are there wildcards or list indexes?
        boolean firstPart = true;
        for (String part : parts) {
            if (NumberUtils.isDigits(part)) {
                // explicit list index
                type = ((ListType) type).getFieldType();
            } else if (!part.startsWith("*")) {
                // complex sub-property
                if (!firstPart) {
                    // we already computed the type of the first part
                    field = ((ComplexType) type).getField(part);
                    if (field == null) {
                        throw new QueryParseException("No such property: " + name);
                    }
                    type = field.getType();
                }
            } else {
                // wildcard
                type = ((ListType) type).getFieldType();
            }
            firstPart = false;
        }
        String fullField = String.join("/", parts);
        return new FieldInfo(prop, fullField, type, false);
    }

    protected FieldInfo parseACP(String prop, String[] parts) {
        if (parts.length != 3) {
            throw new QueryParseException("No such property: " + prop);
        }
        String wildcard = parts[1];
        if (NumberUtils.isDigits(wildcard)) {
            throw new QueryParseException("Cannot use explicit index in ACLs: " + prop);
        }
        String last = parts[2];
        String fullField;
        if (NXQL.ECM_ACL_NAME.equals(last)) {
            fullField = KEY_ACP + "/*/" + KEY_ACL_NAME;
            // TODO remember wildcard correlation
        } else {
            String fieldLast = DBSSession.convToInternalAce(last);
            if (fieldLast == null) {
                throw new QueryParseException("No such property: " + prop);
            }
            fullField = KEY_ACP + "/*/" + KEY_ACL + '/' + wildcard + '/' + fieldLast;
        }
        Type type = DBSSession.getType(last);
        return new FieldInfo(prop, fullField, type, false);
    }

    /**
     * Canonicalizes a Nuxeo-xpath. Replaces {@code a/foo[123]/b} with {@code a/123/b} A star or a star followed by
     * digits can be used instead of just the digits as well.
     *
     * @param xpath the xpath
     * @return the canonicalized xpath.
     */
    private String canonicalXPath(String xpath) {
        while (xpath.length() > 0 && xpath.charAt(0) == '/') {
            xpath = xpath.substring(1);
        }
        if (xpath.indexOf('[') == -1) {
            return xpath;
        } else {
            return NON_CANON_INDEX.matcher(xpath).replaceAll("$1");
        }
    }

    public Literal convertIfBoolean(FieldInfo fieldInfo, Literal literal) {
        if (fieldInfo.type instanceof BooleanType && literal instanceof IntegerLiteral) {
            long value = ((IntegerLiteral) literal).value;
            if (ZERO.equals(value)) {
                literal = fieldInfo.isTrueOrNullBoolean ? null : new BooleanLiteral(false);
            } else if (ONE.equals(value)) {
                literal = new BooleanLiteral(true);
            } else {
                throw new QueryParseException("Invalid boolean: " + value);
            }
        }
        return literal;
    }

    private QueryBuilder getQueryBuilder(FieldInfo fieldInfo, Function<String, QueryBuilder> constraintBuilder) {
        Matcher m = WILDCARD_SPLIT.matcher(fieldInfo.fullField);
        if (m.matches()) {
            String correlatedFieldPart = m.group(1);
            String fieldSuffix = m.group(2);
            if (fieldSuffix == null) {
                fieldSuffix = MarkLogicHelper.ARRAY_ITEM_KEY;
            }
            return new CorrelatedContainerQueryBuilder(correlatedFieldPart, constraintBuilder.apply(fieldSuffix));
        }
        String path = fieldInfo.fullField;
        // Handle the list type case - if it's not present in path
        if (fieldInfo.type != null && fieldInfo.type.isListType() && !fieldInfo.fullField.endsWith("*")) {
            path += '/' + MarkLogicHelper.ARRAY_ITEM_KEY;
        }
        return constraintBuilder.apply(path);
    }

    private class FieldInfo {

        /**
         * NXQL property.
         */
        private final String prop;

        /**
         * MarkLogic field including wildcards.
         */
        protected final String fullField;

        protected final Type type;

        /**
         * Boolean system properties only use TRUE or NULL, not FALSE, so queries must be updated accordingly.
         */
        protected final boolean isTrueOrNullBoolean;

        /**
         * Constructor for a simple field.
         */
        public FieldInfo(String prop, String field) {
            this(prop, field, DBSSession.getType(field), true);
        }

        public FieldInfo(String prop, String fullField, Type type, boolean isTrueOrNullBoolean) {
            this.prop = prop;
            this.fullField = fullField;
            this.type = type;
            this.isTrueOrNullBoolean = isTrueOrNullBoolean;
        }

        public String getFullField() {
            return fullField;
        }

        public boolean isBoolean() {
            return type instanceof BooleanType;
        }

        public boolean isMixinTypes() {
            return fullField.equals(DBSDocument.KEY_MIXIN_TYPES);
        }

        public boolean hasWildcard() {
            return fullField.contains("*");
        }

    }

    private static class CorrelatedContainerQueryBuilder extends AbstractNamedQueryBuilder {

        private final QueryBuilder child;

        public CorrelatedContainerQueryBuilder(String path, QueryBuilder child) {
            super(path);
            this.child = child;
        }

        @Override
        protected StructuredQueryDefinition build(StructuredQueryBuilder sqb, String name) {
            if (!name.startsWith("*")) {
                throw new QueryParseException("A correlated query builder might finish by a wildcard, path=" + path);
            }
            return sqb.containerQuery(sqb.element(MarkLogicHelper.ARRAY_ITEM_KEY), child.build(sqb));
        }

        @Override
        public void not() {
            child.not();
        }

        public QueryBuilder getChild() {
            return child;
        }

    }

    private static class EqualQueryBuilder extends AbstractNamedQueryBuilder {

        private final Literal literal;

        private boolean equal;

        public EqualQueryBuilder(String path, Literal literal, boolean equal) {
            super(path);
            this.literal = literal;
            this.equal = equal;
        }

        @Override
        public StructuredQueryDefinition build(StructuredQueryBuilder sqb) {
            StructuredQueryDefinition query = super.build(sqb);
            if (equal) {
                return query;
            }
            return sqb.not(query);
        }

        @Override
        protected StructuredQueryDefinition build(StructuredQueryBuilder sqb, String name) {
            // Handle the wildcard case here because semantic is different for <>
            String serializedName = serializeName(name);
            String serializedValue = MarkLogicStateSerializer.serializeValue(getLiteralValue(literal));
            return sqb.value(sqb.element(serializedName), serializedValue);
        }

        @Override
        public void not() {
            equal = !equal;
        }

    }

    private static class LikeQueryBuilder extends AbstractNamedQueryBuilder {

        private static final String CASE_INSENSITIVE = "case-insensitive";

        private static final String PUNCTUATION_SENSITIVE = "punctuation-sensitive";

        private static final String WHITESPACE_SENSITIVE = "whitespace-sensitive";

        private static final String WILDCARDED = "wildcarded";

        private static final String[] BASIC_OPTIONS = new String[] { PUNCTUATION_SENSITIVE, WILDCARDED,
                WHITESPACE_SENSITIVE };

        private final StringLiteral literal;

        private boolean positive;

        private final boolean caseInsensitive;

        public LikeQueryBuilder(String path, StringLiteral literal, boolean positive, boolean caseInsensitive) {
            super(path);
            this.literal = literal;
            this.positive = positive;
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        public StructuredQueryDefinition build(StructuredQueryBuilder sqb) {
            StructuredQueryDefinition query = super.build(sqb);
            if (positive) {
                return query;
            }
            return sqb.not(query);
        }

        @Override
        protected StructuredQueryDefinition build(StructuredQueryBuilder sqb, String name) {
            String serializedName = serializeName(name);
            String serializedValue = likeToMarkLogicWildcard(literal.value);
            String[] options = BASIC_OPTIONS;
            if (caseInsensitive) {
                options = Arrays.copyOf(options, options.length + 1);
                options[options.length - 1] = CASE_INSENSITIVE;
            }
            return sqb.value(sqb.element(serializedName), null, options, 1.0, serializedValue);
        }

        @Override
        public void not() {
            positive = !positive;
        }

        private String likeToMarkLogicWildcard(String like) {
            StringBuilder mlValue = new StringBuilder();
            char[] chars = like.toCharArray();
            boolean escape = false;
            for (char c : chars) {
                boolean escapeNext = false;
                switch (c) {
                case '%':
                    if (escape) {
                        mlValue.append(c);
                    } else {
                        mlValue.append("*");
                    }
                    break;
                case '_':
                    if (escape) {
                        mlValue.append(c);
                    } else {
                        mlValue.append("?");
                    }
                    break;
                case '\\':
                    if (escape) {
                        mlValue.append("\\");
                    } else {
                        escapeNext = true;
                    }
                    break;
                case '*':
                case '?':
                    mlValue.append("\\").append(c);
                    break;
                default:
                    mlValue.append(c);
                    break;
                }
                escape = escapeNext;
            }
            return mlValue.toString();
        }

    }

    private static class RangeQueryBuilder extends AbstractNamedQueryBuilder {

        private StructuredQueryBuilder.Operator operator;

        private final Literal literal;

        public RangeQueryBuilder(String path, StructuredQueryBuilder.Operator operator, Literal literal) {
            super(path);
            this.operator = operator;
            this.literal = literal;
        }

        @Override
        protected StructuredQueryDefinition build(StructuredQueryBuilder sqb, String name) {
            String serializedName = serializeName(name);
            Object value = getLiteralValue(literal);
            String valueType = ElementType.getType(value.getClass()).getKey();
            String serializedValue = MarkLogicStateSerializer.serializeValue(value);
            return sqb.range(sqb.element(serializedName), valueType, operator, serializedValue);
        }

        @Override
        public void not() {
            if (operator == StructuredQueryBuilder.Operator.LT) {
                operator = StructuredQueryBuilder.Operator.GE;
            } else if (operator == StructuredQueryBuilder.Operator.GT) {
                operator = StructuredQueryBuilder.Operator.LE;
            } else if (operator == StructuredQueryBuilder.Operator.LE) {
                operator = StructuredQueryBuilder.Operator.GT;
            } else if (operator == StructuredQueryBuilder.Operator.GE) {
                operator = StructuredQueryBuilder.Operator.LT;
            }
        }

    }

    private static class InQueryBuilder extends AbstractNamedQueryBuilder {

        private final LiteralList literals;

        private boolean in;

        public InQueryBuilder(String path, LiteralList literals, boolean in) {
            super(path);
            this.literals = literals;
            this.in = in;
        }

        @Override
        public StructuredQueryDefinition build(StructuredQueryBuilder sqb) {
            StructuredQueryDefinition query = super.build(sqb);
            if (in) {
                return query;
            }
            return sqb.not(query);
        }

        @Override
        protected StructuredQueryDefinition build(StructuredQueryBuilder sqb, String name) {
            String serializedName = serializeName(name);
            String[] serializedValues = literals.stream()
                                                .map(this::getLiteralValue)
                                                .map(MarkLogicStateSerializer::serializeValue)
                                                .toArray(String[]::new);
            return sqb.value(sqb.element(serializedName), serializedValues);
        }

        @Override
        public void not() {
            in = !in;
        }

    }

    private static class IsNullQueryBuilder extends AbstractNamedQueryBuilder {

        private boolean isNull;

        public IsNullQueryBuilder(String path, boolean isNull) {
            super(path);
            this.isNull = isNull;
        }

        @Override
        public StructuredQueryDefinition build(StructuredQueryBuilder sqb) {
            StructuredQueryDefinition query = super.build(sqb);
            if (isNull) {
                return sqb.not(query);
            }
            return query;
        }

        @Override
        protected StructuredQueryDefinition build(StructuredQueryBuilder sqb, String name) {
            String serializedName = serializeName(name);
            return sqb.containerQuery(sqb.element(serializedName), sqb.and());
        }

        @Override
        public void not() {
            isNull = !isNull;
        }
    }

    private static class CompositionQueryBuilder implements QueryBuilder {

        private final List<QueryBuilder> children;

        private boolean and;

        public CompositionQueryBuilder(List<QueryBuilder> children, boolean and) {
            this.children = children;
            this.and = and;
        }

        @Override
        public StructuredQueryDefinition build(StructuredQueryBuilder sqb) {
            if (children.size() == 1) {
                return children.get(0).build(sqb);
            }
            StructuredQueryDefinition[] childrenQueries = children.stream()
                                                                  .map(child -> child.build(sqb))
                                                                  .toArray(StructuredQueryDefinition[]::new);
            if (and) {
                return sqb.and(childrenQueries);
            }
            return sqb.or(childrenQueries);
        }

        @Override
        public void not() {
            and = !and;
            children.forEach(QueryBuilder::not);
        }

    }

    private static abstract class AbstractNamedQueryBuilder implements QueryBuilder {

        protected final String path;

        public AbstractNamedQueryBuilder(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        @Override
        public StructuredQueryDefinition build(StructuredQueryBuilder sqb) {
            String[] parts = path.split("/");
            StructuredQueryDefinition query = build(sqb, parts[parts.length - 1]);
            for (int i = parts.length - 2; i >= 0; i--) {
                query = sqb.containerQuery(sqb.element(serializeName(parts[i])), query);
            }
            return query;
        }

        protected abstract StructuredQueryDefinition build(StructuredQueryBuilder sqb, String name);

    }

    private interface QueryBuilder {

        StructuredQueryDefinition build(StructuredQueryBuilder sqb);

        void not();

        default Object getLiteralValue(Literal literal) {
            Object result;
            if (literal instanceof BooleanLiteral) {
                result = ((BooleanLiteral) literal).value;
            } else if (literal instanceof DateLiteral) {
                result = ((DateLiteral) literal).value;
            } else if (literal instanceof DoubleLiteral) {
                result = ((DoubleLiteral) literal).value;
            } else if (literal instanceof IntegerLiteral) {
                result = ((IntegerLiteral) literal).value;
            } else if (literal instanceof StringLiteral) {
                result = ((StringLiteral) literal).value;
            } else {
                throw new QueryParseException("Unknown literal: " + literal);
            }
            return result;
        }

        default String serializeName(String name) {
            return name.startsWith("*") ? MarkLogicHelper.ARRAY_ITEM_KEY : MarkLogicHelper.serializeKey(name);
        }

    }

}

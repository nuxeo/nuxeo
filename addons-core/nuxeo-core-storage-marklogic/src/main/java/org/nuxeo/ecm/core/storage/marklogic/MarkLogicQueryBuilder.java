/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IN_MIGRATION;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDICATED_PROPERTY;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SCORE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MIXIN_TYPES;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
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
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.storage.marklogic.MarkLogicHelper.ElementType;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

/**
 * Query builder for a MarkLogic query from an {@link Expression}.
 *
 * @since 8.3
 */
class MarkLogicQueryBuilder {

    private static final Long ZERO = 0L;

    private static final Long ONE = 1L;

    private static final String DATE_CAST = "DATE";

    public static final RangeElementIndexPredicate PRIMARY_TYPE_RANGE_INDEX_PREDICATE = new RangeElementIndexPredicate(
            KEY_PRIMARY_TYPE, "string");

    protected final MarkLogicSchemaManager schemaManager;

    protected List<String> documentTypes;

    // non-canonical index syntax, for replaceAll
    private final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+|\\*|\\*\\d+)\\]" // index in brackets
    );

    /** Splits foo/*1/bar into foo/*1, bar with the last bar part optional */
    protected final static Pattern WILDCARD_SPLIT = Pattern.compile("(.*/\\*\\d+)(?:/(.*))?");

    private final Expression expression;

    private final SelectClause selectClause;

    private final OrderByClause orderByClause;

    private final Set<String> principals;

    private final PathResolver pathResolver;

    public boolean hasFulltext;

    private final boolean fulltextSearchDisabled;

    private final boolean distinctDocuments;

    private final List<MarkLogicRangeElementIndexDescriptor> rangeElementIndexes;

    private Boolean projectionHasWildcard;

    public MarkLogicQueryBuilder(DBSExpressionEvaluator evaluator, OrderByClause orderByClause,
            boolean distinctDocuments, List<MarkLogicRangeElementIndexDescriptor> rangeElementIndexes) {
        this.schemaManager = new MarkLogicSchemaManager();
        this.expression = evaluator.getExpression();
        this.selectClause = evaluator.getSelectClause();
        this.orderByClause = orderByClause;
        this.principals = evaluator.principals;
        this.pathResolver = evaluator.pathResolver;
        this.fulltextSearchDisabled = evaluator.fulltextSearchDisabled;
        this.distinctDocuments = distinctDocuments;
        this.rangeElementIndexes = rangeElementIndexes;
    }

    public boolean doManualProjection() {
        // Don't do manual projection if there are no projection wildcards, as this brings no new
        // information and is costly. The only difference is several identical rows instead of one.
        return !distinctDocuments && hasProjectionWildcard();
    }

    private boolean hasProjectionWildcard() {
        if (projectionHasWildcard == null) {
            projectionHasWildcard = false;
            for (Operand op : selectClause.getSelectList().values()) {
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

    public MarkLogicQuery buildQuery() {
        Expression expression = this.expression;
        if (principals != null) {
            // Add principals to expression
            LiteralList principalLiterals = principals.stream().map(StringLiteral::new).collect(
                    Collectors.toCollection(LiteralList::new));
            Expression principalsExpression = new Expression(new Reference(ExpressionEvaluator.NXQL_ECM_READ_ACL),
                    Operator.IN, principalLiterals);
            // Build final AND expression
            expression = new Expression(expression, Operator.AND, principalsExpression);
        }
        return new MarkLogicQuery(walkExpression(expression).build());
    }

    private QueryBuilder walkExpression(Expression expression) {
        Operator op = expression.operator;
        Operand lvalue = expression.lvalue;
        Operand rvalue = expression.rvalue;
        Reference ref = lvalue instanceof Reference ? (Reference) lvalue : null;
        String name = ref != null ? ref.name : null;
        String cast = ref != null ? ref.cast : null;
        if (DATE_CAST.equals(cast)) {
            checkDateLiteralForCast(op, rvalue, name);
        }

        if (op == Operator.STARTSWITH) {
            return walkStartsWith(lvalue, rvalue);
        } else if (NXQL.ECM_PATH.equals(name)) {
            return walkEcmPath(op, rvalue);
        } else if (NXQL.ECM_ISTRASHED.equals(name)) {
            return walkIsTrashed(op, rvalue);
        } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT) && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
            return walkEcmFulltext(name, op, rvalue);
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
            return walkBetween(lvalue, rvalue, true);
        } else if (op == Operator.NOTBETWEEN) {
            return walkBetween(lvalue, rvalue, false);
        }
        throw new QueryParseException("Unknown operator: " + op);
    }

    private void checkDateLiteralForCast(Operator op, Operand value, String name) {
        if (op == Operator.BETWEEN || op == Operator.NOTBETWEEN) {
            LiteralList l = (LiteralList) value;
            checkDateLiteralForCast(l.get(0), name);
            checkDateLiteralForCast(l.get(1), name);
        } else {
            checkDateLiteralForCast(value, name);
        }
    }

    private void checkDateLiteralForCast(Operand value, String name) {
        if (value instanceof DateLiteral && !((DateLiteral) value).onlyDate) {
            throw new QueryParseException("DATE() cast must be used with DATE literal, not TIMESTAMP: " + name);
        }
    }

    private QueryBuilder walkStartsWith(Operand lvalue, Operand rvalue) {
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

    private QueryBuilder walkStartsWithPath(String path) {
        // resolve path
        String ancestorId = pathResolver.getIdForPath(path);
        if (ancestorId == null) {
            // no such path
            // TODO XXX do better
            return walkNull(new Reference(NXQL.ECM_UUID), true);
        }
        return walkEq(new Reference(ExpressionEvaluator.NXQL_ECM_ANCESTOR_IDS), new StringLiteral(ancestorId), true);
    }

    private QueryBuilder walkStartsWithNonPath(Operand lvalue, String path) {
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

    protected QueryBuilder walkEcmFulltext(String lvalue, Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.LIKE) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires = or LIKE operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires literal string as right argument");
        }
        if (fulltextSearchDisabled) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        if (lvalue.equals(NXQL.ECM_FULLTEXT)) {
            hasFulltext = true;
            return getMarkLogicFulltextQuery((StringLiteral) rvalue);
        }
        // TODO implement fulltext on explicit field
        return null;
    }

    protected QueryBuilder walkIsTrashed(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ISTRASHED + " requires = or <> operator");
        }
        TrashService trashService = Framework.getService(TrashService.class);
        if (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            return walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
        } else if (trashService.hasFeature(TRASHED_STATE_IN_MIGRATION)) {
            QueryBuilder lifeCycleTrashed = walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
            QueryBuilder propertyTrashed = walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue,
                    new BooleanLiteral(true));
            return new CompositionQueryBuilder(new ArrayList<>(Arrays.asList(lifeCycleTrashed, propertyTrashed)),
                    false);
        } else if (trashService.hasFeature(TRASHED_STATE_IS_DEDICATED_PROPERTY)) {
            return walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue, new BooleanLiteral(true));
        } else {
            throw new UnsupportedOperationException("TrashService is in an unknown state");
        }
    }

    protected QueryBuilder walkIsTrashed(Reference ref, Operator op, Operand initialRvalue, Literal deletedRvalue) {
        long v;
        if (!(initialRvalue instanceof IntegerLiteral)
                || ((v = ((IntegerLiteral) initialRvalue).value) != 0 && v != 1)) {
            throw new QueryParseException(NXQL.ECM_ISTRASHED + " requires literal 0 or 1 as right argument");
        }
        boolean equalsDeleted = op == Operator.EQ ^ v == 0;
        return walkEq(ref, deletedRvalue, equalsDeleted);
    }

    protected QueryBuilder getMarkLogicFulltextQuery(StringLiteral rvalue) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(rvalue.value);
        if (ft == null) {
            // TODO handle when fulltext query is null
            return null;
        }
        return translateFulltext(ft);
    }

    /**
     * Transforms the NXQL fulltext syntax into MarkLogic queries.
     */
    protected QueryBuilder translateFulltext(FulltextQuery ft) {
        QueryBuilder query;
        if (ft.op == Op.OR) {
            List<String> words = new ArrayList<>(ft.terms.size());
            List<QueryBuilder> queries = new ArrayList<>(ft.terms.size());
            for (FulltextQuery term : ft.terms) {
                if (term.op == Op.WORD) {
                    words.add(term.word.toLowerCase());
                } else {
                    queries.add(translateFulltext(term));
                }
            }
            if (!words.isEmpty()) {
                queries.add(0, new FulltextQueryBuilder(words.toArray(new String[0])));
            }
            if (queries.size() > 1) {
                query = new CompositionQueryBuilder(queries, false);
            } else {
                query = queries.get(0);
            }
        } else if (ft.op == Op.AND) {
            List<QueryBuilder> queries = ft.terms.stream().map(this::translateFulltext).collect(Collectors.toList());
            query = new CompositionQueryBuilder(queries, true);
        } else {
            query = new FulltextQueryBuilder(ft.word.toLowerCase());
            if (ft.op == Op.NOTWORD) {
                query.not();
            }
        }
        return query;
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
            return walkMixinTypes(Collections.singletonList((StringLiteral) rvalue), equals);
        }
        Literal convertedLiteral = convertIfBoolean(leftInfo, (Literal) rvalue);
        // If the literal is null it could be :
        // - test for non existence of boolean field
        // - a platform issue
        if (convertedLiteral == null) {
            return walkNull(lvalue, equals);
        } else {
            // Get MarkLogic type - not null as convertedLiteral is not null
            String markLogicType = getMarkLogicType(convertedLiteral);
            if (convertedLiteral instanceof DateLiteral && ((DateLiteral) convertedLiteral).onlyDate) {
                // As it's a date we add a NXQL regexp as suffix in order to match days with a like query
                String date = convertedLiteral.asString() + "T__:__:__.___";
                return getQueryBuilder(leftInfo,
                        name -> new LikeQueryBuilder(name, new StringLiteral(date), true, false));
            } else if (rangeElementIndexes.stream().anyMatch(
                    new RangeElementIndexPredicate(leftInfo.queriedElement, markLogicType))) {
                // For NOT EQ we need a different semantic for arrays as Operator.NE will match documents having any
                // item not equal to literal which is different than matching documents having all items not equal to
                // literal
                // So, if we query an array, we want to keep the EQ operator and rely on a not-query
                boolean isList = leftInfo.type != null && leftInfo.type.isListType();
                RangeQueryBuilder.Operator operator = equals || isList ? RangeQueryBuilder.Operator.EQ
                        : RangeQueryBuilder.Operator.NE;
                return getQueryBuilder(leftInfo,
                        name -> new RangeQueryBuilder(name, operator, convertedLiteral, !equals && isList));
            }
        }
        return getQueryBuilder(leftInfo, name -> new EqualQueryBuilder(name, convertedLiteral, equals));
    }

    private String getMarkLogicType(Literal literal) {
        ElementType markLogicType;
        if (literal instanceof BooleanLiteral) {
            markLogicType = ElementType.BOOLEAN;
        } else if (literal instanceof DateLiteral) {
            markLogicType = ElementType.CALENDAR;
        } else if (literal instanceof DoubleLiteral) {
            markLogicType = ElementType.DOUBLE;
        } else if (literal instanceof IntegerLiteral) {
            markLogicType = ElementType.LONG;
        } else if (literal instanceof StringLiteral) {
            markLogicType = ElementType.STRING;
        } else {
            throw new QueryParseException("Unsupported literal type=" + literal.getClass());
        }
        return markLogicType.getWithoutNamespace();
    }

    /**
     * Matches the mixin types against a list of values.
     * <p>
     * Used for:
     * <ul>
     * <li>ecm:mixinTypes = 'Foo'
     * <li>ecm:mixinTypes != 'Foo'
     * <li>ecm:mixinTypes IN ('Foo', 'Bar')
     * <li>ecm:mixinTypes NOT IN ('Foo', 'Bar')
     * </ul>
     * <p>
     * ecm:mixinTypes IN ('Foo', 'Bar')
     *
     * <pre>
     * { "$or" : [ { "ecm:primaryType" : { "$in" : [ ... types with Foo or Bar ...]}} ,
     *             { "ecm:mixinTypes" : { "$in" : [ "Foo" , "Bar]}}]}
     * </pre>
     *
     * ecm:mixinTypes NOT IN ('Foo', 'Bar')
     * <p>
     *
     * <pre>
     * { "$and" : [ { "ecm:primaryType" : { "$in" : [ ... types without Foo nor Bar ...]}} ,
     *              { "ecm:mixinTypes" : { "$nin" : [ "Foo" , "Bar]}}]}
     * </pre>
     */
    private QueryBuilder walkMixinTypes(List<Literal> mixins, boolean include) {
        Set<String> mixinDocumentTypes = mixins.stream()
                                               .map(Literal::asString)
                                               .flatMap(mixin -> schemaManager.getMixinDocumentTypes(mixin).stream())
                                               .collect(Collectors.toSet());

        Collector<Literal, LiteralList, LiteralList> literalListCollector = Collector.of(LiteralList::new,
                LiteralList::add, (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
        /*
         * Primary types that match.
         */
        Set<String> primaryTypes;
        if (include) {
            primaryTypes = new HashSet<>(mixinDocumentTypes);
        } else {
            primaryTypes = new HashSet<>(getDocumentTypes());
            primaryTypes.removeAll(mixinDocumentTypes);
        }
        LiteralList matchPrimaryTypes = primaryTypes.stream().map(StringLiteral::new).collect(literalListCollector);
        /*
         * Instance mixins that match.
         */
        LiteralList matchMixinTypes = mixins.stream()
                                            .filter(mixin -> !isNeverPerInstanceMixin(mixin))
                                            .distinct()
                                            .collect(literalListCollector);
        /*
         * MarkLogic query generation.
         */
        // match on primary type
        QueryBuilder primaryQuery;
        FieldInfo primaryTypeInfo = walkReference(NXQL.ECM_PRIMARYTYPE);
        if (primaryTypes.size() == 1 && rangeElementIndexes.stream().anyMatch(PRIMARY_TYPE_RANGE_INDEX_PREDICATE)) {
            primaryQuery = getQueryBuilder(primaryTypeInfo,
                    name -> new RangeQueryBuilder(name, RangeQueryBuilder.Operator.EQ, matchPrimaryTypes.get(0)));
        } else {
            primaryQuery = getQueryBuilder(primaryTypeInfo, name -> new InQueryBuilder(name, matchPrimaryTypes, true));
        }
        // match on mixin types
        // $in/$nin with an array matches if any/no element of the array matches
        QueryBuilder mixinQuery = getQueryBuilder(walkReference(NXQL.ECM_MIXINTYPE),
                name -> new InQueryBuilder(name, matchMixinTypes, include));
        // and/or between those
        return new CompositionQueryBuilder(Arrays.asList(primaryQuery, mixinQuery), !include);
    }

    private List<String> getDocumentTypes() {
        if (documentTypes == null) {
            documentTypes = schemaManager.getDocumentTypes();
        }
        return documentTypes;
    }

    private boolean isNeverPerInstanceMixin(Literal mixin) {
        return schemaManager.getNoPerDocumentQueryFacets().contains(mixin.asString());
    }

    private QueryBuilder walkLt(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo,
                name -> new RangeQueryBuilder(name, RangeQueryBuilder.Operator.LT, (Literal) rvalue));
    }

    private QueryBuilder walkGt(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo,
                name -> new RangeQueryBuilder(name, RangeQueryBuilder.Operator.GT, (Literal) rvalue));
    }

    private QueryBuilder walkLtEq(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo,
                name -> new RangeQueryBuilder(name, RangeQueryBuilder.Operator.LE, (Literal) rvalue));
    }

    private QueryBuilder walkGtEq(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        return getQueryBuilder(leftInfo,
                name -> new RangeQueryBuilder(name, RangeQueryBuilder.Operator.GE, (Literal) rvalue));
    }

    private QueryBuilder walkBetween(Operand lvalue, Operand rvalue, boolean positive) {
        LiteralList literals = (LiteralList) rvalue;
        Literal left = literals.get(0);
        Literal right = literals.get(1);
        // Rebuild an AND operator for left <= lvalue <= right
        Expression gteExpression = new Expression(lvalue, Operator.GTEQ, left);
        Expression lteExpression = new Expression(lvalue, Operator.LTEQ, right);
        QueryBuilder andBuilder = walkAnd(gteExpression, lteExpression);
        if (!positive) {
            andBuilder.not();
        }
        return andBuilder;
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
        Map<String, String> propBaseKeyToFieldBase = new HashMap<>();
        for (Iterator<QueryBuilder> it = children.iterator(); it.hasNext();) {
            QueryBuilder child = it.next();
            if (child instanceof CorrelatedContainerQueryBuilder) {
                CorrelatedContainerQueryBuilder queryBuilder = (CorrelatedContainerQueryBuilder) child;
                String correlatedPath = queryBuilder.getCorrelatedPath();
                propBaseKeyToFieldBase.putIfAbsent(correlatedPath, queryBuilder.getPath());
                // Store object for this key
                List<QueryBuilder> propBaseBuilders = propBaseToBuilders.computeIfAbsent(correlatedPath,
                        key -> new LinkedList<>());
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
            String path = propBaseKeyToFieldBase.get(correlatedPath);
            children.add(new CorrelatedContainerQueryBuilder(path, correlatedPath, queryBuilder));
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
        return getQueryBuilder(leftInfo,
                name -> new LikeQueryBuilder(name, (StringLiteral) rvalue, positive, caseInsensitive));
    }

    private QueryBuilder walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        if (!(rvalue instanceof LiteralList)) {
            throw new QueryParseException("Invalid IN, right hand side must be a list: " + rvalue);
        }
        FieldInfo leftInfo = walkReference(lvalue);
        if (leftInfo.isMixinTypes()) {
            return walkMixinTypes((LiteralList) rvalue, positive);
        }
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
        FieldInfo fieldInfo = walkReference(reference.name);
        if (DATE_CAST.equals(reference.cast)) {
            Type type = fieldInfo.type;
            if (!(type instanceof DateType
                    || (type instanceof ListType && ((ListType) type).getFieldType() instanceof DateType))) {
                throw new QueryParseException("Cannot cast to " + reference.cast + ": " + reference.name);
            }
        }
        return fieldInfo;
    }

    private FieldInfo walkReference(String name) {
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
        Field field = schemaManager.computeField(name, first);
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

    private FieldInfo parseACP(String prop, String[] parts) {
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
                fieldSuffix = fieldInfo.queryField.substring(fieldInfo.queryField.lastIndexOf('/') + 1);
            }
            String path = fieldInfo.queryField.substring(0, fieldInfo.queryField.indexOf('/' + fieldSuffix));
            return new CorrelatedContainerQueryBuilder(path, correlatedFieldPart, constraintBuilder.apply(fieldSuffix));
        }
        String path = fieldInfo.queryField;
        // Handle the list type case - if it's not present in path
        if (fieldInfo.type != null && fieldInfo.type.isListType() && !fieldInfo.fullField.endsWith("*")) {
            path += '/' + MarkLogicHelper.buildItemNameFromPath(path);
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

        /**
         * Queried element used to know if there's a range element index on it (match against the MarkLogic repository
         * configuration), for example:
         * <ul>
         * <li>foo/bar -> bar</li>
         * <li>foo/bar/* -> bar</li>
         * </ul>
         *
         * @since 8.10
         */
        protected final String queriedElement;

        /**
         * MarkLogic field without widlcards (replaced by the corresponding name in MarkLogic)
         */
        protected final String queryField;

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
            List<String> fields = new ArrayList<>();
            String previous = null;
            String queriedElement = null;
            for (String element : fullField.split("/")) {
                if (element.startsWith("*")) {
                    if (previous == null) {
                        throw new QueryParseException("Invalid query, property can't starts by '*'");
                    }
                    fields.add(previous + MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX);
                } else {
                    fields.add(element);
                    queriedElement = element;
                }
                previous = element;
            }
            this.queriedElement = queriedElement;
            this.queryField = String.join("/", fields);
            this.type = type;
            this.isTrueOrNullBoolean = isTrueOrNullBoolean;
        }

        public boolean isBoolean() {
            return type instanceof BooleanType;
        }

        public boolean isMixinTypes() {
            return fullField.equals(KEY_MIXIN_TYPES);
        }

        public boolean hasWildcard() {
            return fullField.contains("*");
        }

    }

    private static class CorrelatedContainerQueryBuilder extends AbstractNamedQueryBuilder {

        private final String correlatedPath;

        private final QueryBuilder child;

        public CorrelatedContainerQueryBuilder(String path, String correlatedPath, QueryBuilder child) {
            super(path);
            this.correlatedPath = correlatedPath;
            this.child = child;
        }

        @Override
        protected String build(String name) {
            if (!correlatedPath.matches("^.*\\*\\d$")) {
                throw new QueryParseException("A correlated query builder might finish by a wildcard, path=" + path);
            }
            return String.format("cts:element-query(fn:QName(\"\", \"%s\"), %s)", serializeName(name), child.build());
        }

        @Override
        public void not() {
            child.not();
        }

        public String getCorrelatedPath() {
            return correlatedPath;
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
        public String build() {
            String query = super.build();
            if (equal) {
                return query;
            }
            return "cts:not-query(" + query + ")";
        }

        @Override
        protected String build(String name) {
            String serializedName = serializeName(name);
            String serializedValue = serializeValue(getLiteralValue(literal));
            return String.format("cts:element-value-query(fn:QName(\"\", \"%s\"), \"%s\", (\"exact\"))", serializedName,
                    serializedValue);
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

        private static final String BASIC_OPTIONS = String.format("(\"%s\",\"%s\",\"%s\")", PUNCTUATION_SENSITIVE,
                WILDCARDED, WHITESPACE_SENSITIVE);

        private static final String INSENSITIVE_OPTIONS = String.format("(\"%s\",\"%s\",\"%s\",\"%s\")",
                PUNCTUATION_SENSITIVE, WILDCARDED, WHITESPACE_SENSITIVE, CASE_INSENSITIVE);

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
        public String build() {
            String query = super.build();
            if (positive) {
                return query;
            }
            return "cts:not-query(" + query + ")";
        }

        @Override
        protected String build(String name) {
            String serializedName = serializeName(name);
            String serializedValue = likeToMarkLogicWildcard(literal.value);
            String options = BASIC_OPTIONS;
            if (caseInsensitive) {
                options = INSENSITIVE_OPTIONS;
            }
            return String.format("cts:element-value-query(fn:QName(\"\", \"%s\"), \"%s\", %s)", serializedName,
                    serializedValue, options);
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
            return serializeValue(mlValue.toString());
        }

    }

    private static class FulltextQueryBuilder implements QueryBuilder {

        private final String[] values;

        private boolean not;

        public FulltextQueryBuilder(String value) {
            this.values = new String[] { value };
        }

        public FulltextQueryBuilder(String[] values) {
            this.values = values;
        }

        @Override
        public String build() {
            String text = Arrays.stream(values).map(this::serializeValue).map(value -> '"' + value + '"').collect(
                    Collectors.joining(",", "(", ")"));
            String query = "cts:word-query(" + text + ", (\"case-insensitive\",\"diacritic-sensitive\","
                    + "\"punctuation-sensitive\",\"whitespace-sensitive\",\"stemmed\"))";
            if (not) {
                return "cts:not-query(" + query + ")";
            }
            return query;
        }

        @Override
        public void not() {
            not = !not;
        }

    }

    private static class RangeQueryBuilder extends AbstractNamedQueryBuilder {

        public enum Operator {

            LT("<"), LE("<="), GE(">="), GT(">"), EQ("="), NE("!=");

            private final String markLogicOperator;

            Operator(String markLogicOperator) {
                this.markLogicOperator = markLogicOperator;
            }

            public String getMarkLogicOperator() {
                return markLogicOperator;
            }

        }

        private Operator operator;

        private boolean notList;

        private final Literal literal;

        public RangeQueryBuilder(String path, Operator operator, Literal literal) {
            super(path);
            this.operator = operator;
            this.literal = literal;
        }

        public RangeQueryBuilder(String path, Operator operator, Literal literal, boolean notList) {
            this(path, operator, literal);
            this.notList = notList;
        }

        @Override
        public String build() {
            String query = super.build();
            if (notList) {
                return "cts:not-query(" + query + ")";
            }
            return query;
        }

        @Override
        protected String build(String name) {
            String serializedName = serializeName(name);
            Object value = getLiteralValue(literal);
            String valueType = ElementType.getType(value).get();
            String serializedValue = serializeValue(value);
            return String.format("cts:element-range-query(fn:QName(\"\",\"%s\"),\"%s\",%s(\"%s\"))", serializedName,
                    operator.getMarkLogicOperator(), valueType, serializedValue);
        }

        @Override
        public void not() {
            // Handle array semantic, for example ecm:ancestorIds <> "..."
            if (path.endsWith(MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX)) {
                notList = !notList;
            } else {
                if (operator == Operator.LT) {
                    operator = Operator.GE;
                } else if (operator == Operator.GT) {
                    operator = Operator.LE;
                } else if (operator == Operator.LE) {
                    operator = Operator.GT;
                } else if (operator == Operator.GE) {
                    operator = Operator.LT;
                } else if (operator == Operator.EQ) {
                    operator = Operator.NE;
                } else if (operator == Operator.NE) {
                    operator = Operator.EQ;
                }
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
        public String build() {
            String query = super.build();
            if (in) {
                return query;
            }
            return "cts:not-query(" + query + ")";
        }

        @Override
        protected String build(String name) {
            String serializedName = serializeName(name);
            String serializedValues = literals.stream()
                                              .map(this::getLiteralValue)
                                              .map(this::serializeValue)
                                              .map(s -> "\"" + s + "\"")
                                              .collect(Collectors.joining(",", "(", ")"));
            return String.format("cts:element-value-query(fn:QName(\"\", \"%s\"), %s, (\"exact\"))", serializedName,
                    serializedValues);
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
        public String build() {
            String query = super.build();
            if (isNull) {
                return "cts:not-query(" + query + ")";
            }
            return query;
        }

        @Override
        protected String build(String name) {
            String serializedName = serializeName(name);
            return String.format("cts:element-query(fn:QName(\"\", \"%s\"), cts:and-query(()))", serializedName);
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
        public String build() {
            if (children.size() == 1) {
                return children.get(0).build();
            }
            String childrenQueries = children.stream()
                                             .map(QueryBuilder::build)
                                             .collect(Collectors.joining(",", "(", ")"));
            if (and) {
                return String.format("cts:and-query(%s)", childrenQueries);
            }
            return String.format("cts:or-query(%s)", childrenQueries);
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
        public String build() {
            String[] parts = path.split("/");
            String query = build(parts[parts.length - 1]);
            for (int i = parts.length - 2; i >= 0; i--) {
                query = String.format("cts:element-query(fn:QName(\"\", \"%s\"),%s)", serializeName(parts[i]), query);
            }
            return query;
        }

        protected abstract String build(String name);

    }

    private interface QueryBuilder {

        String build();

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
            return MarkLogicHelper.serializeKey(name);
        }

        default String serializeValue(Object value) {
            return StringEscapeUtils.escapeXml10(MarkLogicStateSerializer.serializeValue(value));
        }

    }

    public class MarkLogicQuery {

        private final String ctsQuery;

        public boolean sortOnFulltextScore;

        public MarkLogicQuery(String ctsQuery) {
            this.ctsQuery = ctsQuery;
        }

        public String getSearchQuery() {
            String searchQuery = createSearchQuery();
            return addProjections(searchQuery);
        }

        public String getSearchQuery(int limit, int offset) {
            String searchQuery = createSearchQuery();
            if (limit != 0) {
                searchQuery = String.format("%s[%s to %s]", searchQuery, offset + 1, offset + limit);
            }
            return addProjections(searchQuery);
        }

        public String getCountQuery() {
            return String.format("fn:count(%s)", createSearchQuery());
        }

        public String getCountQuery(int countUpTo) {
            return String.format("fn:count(%s,%s)", createSearchQuery(), countUpTo);
        }

        private String createSearchQuery() {
            if (orderByClause == null) {
                return "cts:search(fn:doc()," + ctsQuery + ')';
            }
            return "cts:search(fn:doc()," + ctsQuery + ",(" + getOrderBy() + "))";
        }

        private String getOrderBy() {
            sortOnFulltextScore = false;
            Map<String, String> rangeElementTypes = rangeElementIndexes.stream().collect(
                    Collectors.toMap(d -> d.element, d -> d.type));
            Set<String> elements = new HashSet<>(orderByClause.elements.size());
            StringBuilder orderBy = new StringBuilder();
            for (OrderByExpr ob : orderByClause.elements) {
                String element = walkReference(ob.reference).queriedElement;
                boolean desc = ob.isDescending;
                if (!elements.contains(element)) {
                    if (!elements.isEmpty()) {
                        orderBy.append(',');
                    }
                    if (KEY_FULLTEXT_SCORE.equals(ob.reference.name)) {
                        sortOnFulltextScore = true;
                        orderBy.append("cts:score-order(\"");
                        orderBy.append(desc ? "descending" : "ascending");
                        orderBy.append("\")");
                    } else {
                        orderBy.append("cts:index-order(cts:element-reference(fn:QName(\"\", \"");
                        orderBy.append(MarkLogicHelper.serializeKey(element)).append("\"),");
                        orderBy.append("(\"type=").append(rangeElementTypes.get(element)).append("\")),\"");
                        orderBy.append(desc ? "descending" : "ascending");
                        orderBy.append("\")");
                    }
                }
                elements.add(element);
            }
            return orderBy.toString();
        }

        private String addProjections(String searchQuery) {
            boolean projectionOnFulltextScore = false;
            String query = searchQuery;
            if (!doManualProjection()) {
                StringBuilder fields = new StringBuilder();
                Set<String> elements = new HashSet<>();
                for (Operand op : selectClause.getSelectList().values()) {
                    if (!(op instanceof Reference)) {
                        throw new QueryParseException("Projection not supported: " + op);
                    }
                    String name = ((Reference) op).name;
                    projectionOnFulltextScore = projectionOnFulltextScore || KEY_FULLTEXT_SCORE.equals(name);
                    if (!elements.contains(name)) {
                        appendProjection(fields, name);
                        elements.add(name);
                    }
                }
                if (projectionOnFulltextScore || sortOnFulltextScore) {
                    if (!hasFulltext) {
                        throw new QueryParseException(
                                NXQL.ECM_FULLTEXT_SCORE + " cannot be used without " + NXQL.ECM_FULLTEXT);
                    }
                }
                query = "import module namespace extract = 'http://nuxeo.com/extract' at '/ext/nuxeo/extract.xqy';\n"
                        + "let $paths := (" + fields.toString() + ")let $namespaces := ()\n" + "for $i in " + query
                        + " return extract:extract-nodes($i, $paths, $namespaces)";
            }
            return query;
        }

        private void appendProjection(StringBuilder fields, String name) {
            FieldInfo fieldInfo = walkReference(name);
            if (fields.length() > 0) {
                fields.append(',').append('\n');
            }
            fields.append('"')
                  .append(MarkLogicHelper.DOCUMENT_ROOT_PATH)
                  .append('/')
                  .append(MarkLogicHelper.serializeKey(fieldInfo.queryField))
                  .append('"');
        }

    }

}

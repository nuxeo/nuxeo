/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.mongodb;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IN_MIGRATION;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDICATED_PROPERTY;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.FACETED_TAG;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.FACETED_TAG_LABEL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SCORE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_MINOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_UID_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_UID_MINOR_VERSION;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_META;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_TEXT_SCORE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bson.Document;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.Function;
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
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;
import org.nuxeo.ecm.core.storage.QueryOptimizer.PrefixInfo;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.mongodb.QueryOperators;

/**
 * Query builder for a MongoDB query from an {@link Expression}.
 *
 * @since 5.9.4
 */
public class MongoDBQueryBuilder {

    public static final Long LONG_ZERO = Long.valueOf(0);

    public static final Long LONG_ONE = Long.valueOf(1);

    public static final Double ONE = Double.valueOf(1);

    public static final Double MINUS_ONE = Double.valueOf(-1);

    protected static final String DATE_CAST = "DATE";

    protected static final String LIKE_ANCHORED_PROP = "nuxeo.mongodb.like.anchored";

    protected final AtomicInteger counter = new AtomicInteger();

    protected final SchemaManager schemaManager;

    protected final MongoDBConverter converter;

    protected final String idKey;

    protected List<String> documentTypes;

    protected final Expression expression;

    protected final SelectClause selectClause;

    protected final OrderByClause orderByClause;

    protected final PathResolver pathResolver;

    public boolean hasFulltext;

    public boolean sortOnFulltextScore;

    protected Document query;

    protected Document orderBy;

    protected Document projection;

    protected Map<String, String> propertyKeys;

    boolean projectionHasWildcard;

    private boolean fulltextSearchDisabled;

    /**
     * Prefix to remove for $elemMatch (including final dot), or {@code null} if there's no current prefix to remove.
     */
    protected String elemMatchPrefix;

    protected boolean likeAnchored;

    public MongoDBQueryBuilder(MongoDBRepository repository, Expression expression, SelectClause selectClause,
            OrderByClause orderByClause, PathResolver pathResolver, boolean fulltextSearchDisabled) {
        schemaManager = Framework.getService(SchemaManager.class);
        converter = repository.converter;
        idKey = repository.idKey;
        this.expression = expression;
        this.selectClause = selectClause;
        this.orderByClause = orderByClause;
        this.pathResolver = pathResolver;
        this.fulltextSearchDisabled = fulltextSearchDisabled;
        this.propertyKeys = new HashMap<>();
        likeAnchored = !Framework.getService(ConfigurationService.class).isBooleanPropertyFalse(LIKE_ANCHORED_PROP);
    }

    public void walk() {
        query = walkExpression(expression); // computes hasFulltext
        walkOrderBy(); // computes sortOnFulltextScore
        walkProjection(); // needs hasFulltext and sortOnFulltextScore
    }

    public Document getQuery() {
        return query;
    }

    public Document getOrderBy() {
        return orderBy;
    }

    public Document getProjection() {
        return projection;
    }

    public boolean hasProjectionWildcard() {
        return projectionHasWildcard;
    }

    protected void walkOrderBy() {
        sortOnFulltextScore = false;
        if (orderByClause == null) {
            orderBy = null;
        } else {
            orderBy = new Document();
            for (OrderByExpr ob : orderByClause.elements) {
                Reference ref = ob.reference;
                boolean desc = ob.isDescending;
                String field = walkReference(ref).queryField;
                if (!orderBy.containsKey(field)) {
                    Object value;
                    if (KEY_FULLTEXT_SCORE.equals(field)) {
                        if (!desc) {
                            throw new QueryParseException("Cannot sort by " + NXQL.ECM_FULLTEXT_SCORE + " ascending");
                        }
                        sortOnFulltextScore = true;
                        value = new Document(MONGODB_META, MONGODB_TEXT_SCORE);
                    } else {
                        value = desc ? MINUS_ONE : ONE;
                    }
                    orderBy.put(field, value);
                }
            }
            if (sortOnFulltextScore && orderBy.size() > 1) {
                throw new QueryParseException("Cannot sort by " + NXQL.ECM_FULLTEXT_SCORE + " and other criteria");
            }
        }
    }

    protected void walkProjection() {
        projection = new Document();
        boolean projectionOnFulltextScore = false;
        for (Operand op : selectClause.getSelectList().values()) {
            if (!(op instanceof Reference)) {
                throw new QueryParseException("Projection not supported: " + op);
            }
            FieldInfo fieldInfo = walkReference((Reference) op);
            String propertyField = fieldInfo.prop;
            if (!propertyField.equals(NXQL.ECM_UUID) //
                    && !propertyField.equals(fieldInfo.projectionField) //
                    && !propertyField.contains("/")) {
                propertyKeys.put(fieldInfo.projectionField, propertyField);
            }
            projection.put(fieldInfo.projectionField, ONE);
            if (propertyField.contains("*")) {
                projectionHasWildcard = true;
            }
            if (fieldInfo.projectionField.equals(KEY_FULLTEXT_SCORE)) {
                projectionOnFulltextScore = true;
            }
        }
        if (projectionOnFulltextScore || sortOnFulltextScore) {
            if (!hasFulltext) {
                throw new QueryParseException(NXQL.ECM_FULLTEXT_SCORE + " cannot be used without " + NXQL.ECM_FULLTEXT);
            }
            projection.put(KEY_FULLTEXT_SCORE, new Document(MONGODB_META, MONGODB_TEXT_SCORE));
        }
    }

    public Document walkExpression(Expression expr) {
        Operator op = expr.operator;
        Operand lvalue = expr.lvalue;
        Operand rvalue = expr.rvalue;
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
        } else if (NXQL.ECM_ANCESTORID.equals(name)) {
            return walkAncestorId(op, rvalue);
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
            return walkEq(lvalue, rvalue);
        } else if (op == Operator.NOTEQ) {
            return walkNotEq(lvalue, rvalue);
        } else if (op == Operator.LTEQ) {
            return walkLtEq(lvalue, rvalue);
        } else if (op == Operator.GTEQ) {
            return walkGtEq(lvalue, rvalue);
        } else if (op == Operator.AND) {
            if (expr instanceof MultiExpression) {
                return walkAndMultiExpression((MultiExpression) expr);
            } else {
                return walkAnd(expr);
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
            return walkIsNull(lvalue);
        } else if (op == Operator.ISNOTNULL) {
            return walkIsNotNull(lvalue);
        } else if (op == Operator.BETWEEN) {
            return walkBetween(lvalue, rvalue, true);
        } else if (op == Operator.NOTBETWEEN) {
            return walkBetween(lvalue, rvalue, false);
        } else {
            throw new QueryParseException("Unknown operator: " + op);
        }
    }

    protected void checkDateLiteralForCast(Operator op, Operand value, String name) {
        if (op == Operator.BETWEEN || op == Operator.NOTBETWEEN) {
            LiteralList l = (LiteralList) value;
            checkDateLiteralForCast(l.get(0), name);
            checkDateLiteralForCast(l.get(1), name);
        } else {
            checkDateLiteralForCast(value, name);
        }
    }

    protected void checkDateLiteralForCast(Operand value, String name) {
        if (value instanceof DateLiteral && !((DateLiteral) value).onlyDate) {
            throw new QueryParseException("DATE() cast must be used with DATE literal, not TIMESTAMP: " + name);
        }
    }

    protected Document walkEcmPath(Operator op, Operand rvalue) {
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
            return new Document(MONGODB_ID, "__nosuchid__");
        }
        if (op == Operator.EQ) {
            return new Document(idKey, id);
        } else {
            return new Document(idKey, new Document(QueryOperators.NE, id));
        }
    }

    protected Document walkAncestorId(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires = or <> operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires literal id as right argument");
        }
        String ancestorId = ((StringLiteral) rvalue).value;
        if (op == Operator.EQ) {
            return new Document(DBSDocument.KEY_ANCESTOR_IDS, ancestorId);
        } else {
            return new Document(DBSDocument.KEY_ANCESTOR_IDS, new Document(QueryOperators.NE, ancestorId));
        }
    }

    protected Document walkEcmFulltext(String name, Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.LIKE) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires = or LIKE operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires literal string as right argument");
        }
        if (fulltextSearchDisabled) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        String fulltextQuery = ((StringLiteral) rvalue).value;
        if (name.equals(NXQL.ECM_FULLTEXT)) {
            // standard fulltext query
            hasFulltext = true;
            String ft = getMongoDBFulltextQuery(fulltextQuery);
            if (ft == null) {
                // empty query, matches nothing
                return new Document(MONGODB_ID, "__nosuchid__");
            }
            Document textSearch = new Document();
            textSearch.put(QueryOperators.SEARCH, ft);
            // TODO language?
            return new Document(QueryOperators.TEXT, textSearch);
        } else {
            // secondary index match with explicit field
            // do a regexp on the field
            if (name.charAt(NXQL.ECM_FULLTEXT.length()) != '.') {
                throw new QueryParseException(name + " has incorrect syntax" + " for a secondary fulltext index");
            }
            String prop = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
            String ft = fulltextQuery.replace(" ", "%");
            rvalue = new StringLiteral(ft);
            return walkLike(new Reference(prop), rvalue, true, true);
        }
    }

    protected Document walkIsTrashed(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ISTRASHED + " requires = or <> operator");
        }
        TrashService trashService = Framework.getService(TrashService.class);
        if (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            return walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
        } else if (trashService.hasFeature(TRASHED_STATE_IN_MIGRATION)) {
            Document lifeCycleTrashed = walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
            Document propertyTrashed = walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue,
                    new BooleanLiteral(true));
            return new Document(QueryOperators.OR, new ArrayList<>(Arrays.asList(lifeCycleTrashed, propertyTrashed)));
        } else if (trashService.hasFeature(TRASHED_STATE_IS_DEDICATED_PROPERTY)) {
            return walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue, new BooleanLiteral(true));
        } else {
            throw new UnsupportedOperationException("TrashService is in an unknown state");
        }
    }

    protected Document walkIsTrashed(Reference ref, Operator op, Operand initialRvalue, Literal deletedRvalue) {
        long v;
        if (!(initialRvalue instanceof IntegerLiteral)
                || ((v = ((IntegerLiteral) initialRvalue).value) != 0 && v != 1)) {
            throw new QueryParseException(NXQL.ECM_ISTRASHED + " requires literal 0 or 1 as right argument");
        }
        boolean equalsDeleted = op == Operator.EQ ^ v == 0;
        if (equalsDeleted) {
            return walkEq(ref, deletedRvalue);
        } else {
            return walkNotEq(ref, deletedRvalue);
        }
    }

    // public static for tests
    public static String getMongoDBFulltextQuery(String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        if (ft == null) {
            return null;
        }
        // translate into MongoDB syntax
        return translateFulltext(ft, false);
    }

    /**
     * Transforms the NXQL fulltext syntax into MongoDB syntax.
     * <p>
     * The MongoDB fulltext query syntax is badly documented, but is actually the following:
     * <ul>
     * <li>a term is a word,
     * <li>a phrase is a set of spaced-separated words enclosed in double quotes,
     * <li>negation is done by prepending a -,
     * <li>the query is a space-separated set of terms, negated terms, phrases, or negated phrases.
     * <li>all the words of non-negated phrases are also added to the terms.
     * </ul>
     * <p>
     * The matching algorithm is (excluding stemming and stop words):
     * <ul>
     * <li>filter out documents with the negative terms, the negative phrases, or missing the phrases,
     * <li>then if any term is present in the document then it's a match.
     * </ul>
     */
    protected static String translateFulltext(FulltextQuery ft, boolean and) {
        List<String> buf = new ArrayList<>();
        translateFulltext(ft, buf, and);
        return StringUtils.join(buf, ' ');
    }

    protected static void translateFulltext(FulltextQuery ft, List<String> buf, boolean and) {
        if (ft.op == Op.OR) {
            for (FulltextQuery term : ft.terms) {
                // don't quote words for OR
                translateFulltext(term, buf, false);
            }
        } else if (ft.op == Op.AND) {
            for (FulltextQuery term : ft.terms) {
                // quote words for AND
                translateFulltext(term, buf, true);
            }
        } else {
            String neg;
            if (ft.op == Op.NOTWORD) {
                neg = "-";
            } else { // Op.WORD
                neg = "";
            }
            String word = ft.word.toLowerCase();
            if (ft.isPhrase() || and) {
                buf.add(neg + '"' + word + '"');
            } else {
                buf.add(neg + word);
            }
        }
    }

    public Document walkNot(Operand value) {
        Object val = walkOperand(value);
        Object not = pushDownNot(val);
        if (!(not instanceof Document)) {
            throw new QueryParseException("Cannot do NOT on: " + val);
        }
        return (Document) not;
    }

    protected Object pushDownNot(Object object) {
        if (!(object instanceof Document)) {
            throw new QueryParseException("Cannot do NOT on: " + object);
        }
        Document ob = (Document) object;
        Set<String> keySet = ob.keySet();
        if (keySet.size() != 1) {
            throw new QueryParseException("Cannot do NOT on: " + ob);
        }
        String key = keySet.iterator().next();
        Object value = ob.get(key);
        if (!key.startsWith("$")) {
            if (value instanceof Document) {
                // push down inside dbobject
                return new Document(key, pushDownNot(value));
            } else {
                // k = v -> k != v
                return new Document(key, new Document(QueryOperators.NE, value));
            }
        }
        if (QueryOperators.NE.equals(key)) {
            // NOT k != v -> k = v
            return value;
        }
        if (QueryOperators.NOT.equals(key)) {
            // NOT NOT v -> v
            return value;
        }
        if (QueryOperators.AND.equals(key) || QueryOperators.OR.equals(key)) {
            // boolean algebra
            // NOT (v1 AND v2) -> NOT v1 OR NOT v2
            // NOT (v1 OR v2) -> NOT v1 AND NOT v2
            String op = QueryOperators.AND.equals(key) ? QueryOperators.OR : QueryOperators.AND;
            List<Object> list = (List<Object>) value;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, pushDownNot(list.get(i)));
            }
            return new Document(op, list);
        }
        if (QueryOperators.IN.equals(key) || QueryOperators.NIN.equals(key)) {
            // boolean algebra
            // IN <-> NIN
            String op = QueryOperators.IN.equals(key) ? QueryOperators.NIN : QueryOperators.IN;
            return new Document(op, value);
        }
        if (QueryOperators.LT.equals(key) || QueryOperators.GT.equals(key) || QueryOperators.LTE.equals(key)
                || QueryOperators.GTE.equals(key)) {
            // TODO use inverse operators?
            return new Document(QueryOperators.NOT, ob);
        }
        throw new QueryParseException("Unknown operator for NOT: " + key);
    }

    protected Document newDocumentWithField(FieldInfo fieldInfo, Object value) {
        return new Document(fieldInfo.queryField, value);
    }

    public Document walkIsNull(Operand value) {
        FieldInfo fieldInfo = walkReference(value);
        return newDocumentWithField(fieldInfo, null);
    }

    public Document walkIsNotNull(Operand value) {
        FieldInfo fieldInfo = walkReference(value);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.NE, null));
    }

    public Document walkAndMultiExpression(MultiExpression expr) {
        return walkAnd(expr, expr.values);
    }

    public Document walkAnd(Expression expr) {
        return walkAnd(expr, Arrays.asList(expr.lvalue, expr.rvalue));
    }

    protected static final Pattern SLASH_WILDCARD_SLASH = Pattern.compile("/\\*\\d+(/)?");

    protected Document walkAnd(Expression expr, List<Operand> values) {
        if (values.size() == 1) {
            return (Document) walkOperand(values.get(0));
        }
        // PrefixInfo was computed by the QueryOptimizer
        PrefixInfo info = (PrefixInfo) expr.getInfo();
        if (info == null || info.count < 2) {
            List<Object> list = walkOperandList(values);
            return new Document(QueryOperators.AND, list);
        }

        // we have a common prefix for all underlying references, extract it into an $elemMatch node

        // info.prefix is the DBS common prefix, ex: foo/bar/*1; ecm:acp/*1/acl/*1
        // compute MongoDB prefix: foo.bar.; ecm:acp.acl.
        String prefix = SLASH_WILDCARD_SLASH.matcher(info.prefix).replaceAll(".");
        // remove current prefix and trailing . for actual field match
        String fieldBase = stripElemMatchPrefix(prefix.substring(0, prefix.length() - 1));

        String previousElemMatchPrefix = elemMatchPrefix;
        elemMatchPrefix = prefix;
        List<Object> list = walkOperandList(values);
        elemMatchPrefix = previousElemMatchPrefix;

        return new Document(fieldBase, new Document(QueryOperators.ELEM_MATCH, new Document(QueryOperators.AND, list)));
    }

    protected String stripElemMatchPrefix(String field) {
        if (elemMatchPrefix != null && field.startsWith(elemMatchPrefix)) {
            field = field.substring(elemMatchPrefix.length());
        }
        return field;
    }

    public Document walkOr(Operand lvalue, Operand rvalue) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        List<Object> list = new ArrayList<>(Arrays.asList(left, right));
        return new Document(QueryOperators.OR, list);
    }

    protected Object checkBoolean(FieldInfo fieldInfo, Object right) {
        if (fieldInfo.isBoolean()) {
            // convert 0 / 1 to actual booleans
            if (right instanceof Long) {
                if (LONG_ZERO.equals(right)) {
                    right = fieldInfo.isTrueOrNullBoolean ? null : FALSE;
                } else if (LONG_ONE.equals(right)) {
                    right = TRUE;
                } else {
                    throw new QueryParseException("Invalid boolean: " + right);
                }
            }
        }
        return right;
    }

    public Document walkEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        if (isMixinTypes(fieldInfo)) {
            if (!(right instanceof String)) {
                throw new QueryParseException("Invalid EQ rhs: " + rvalue);
            }
            return walkMixinTypes(Collections.singletonList((String) right), true);
        }
        right = checkBoolean(fieldInfo, right);
        // TODO check list fields
        return newDocumentWithField(fieldInfo, right);
    }

    public Document walkNotEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        if (isMixinTypes(fieldInfo)) {
            if (!(right instanceof String)) {
                throw new QueryParseException("Invalid NE rhs: " + rvalue);
            }
            return walkMixinTypes(Collections.singletonList((String) right), false);
        }
        right = checkBoolean(fieldInfo, right);
        // TODO check list fields
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.NE, right));
    }

    public Document walkLt(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.LT, right));
    }

    public Document walkGt(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.GT, right));
    }

    public Document walkLtEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.LTE, right));
    }

    public Document walkGtEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.GTE, right));
    }

    public Document walkBetween(Operand lvalue, Operand rvalue, boolean positive) {
        LiteralList l = (LiteralList) rvalue;
        FieldInfo fieldInfo = walkReference(lvalue);
        Object left = walkOperand(l.get(0));
        Object right = walkOperand(l.get(1));
        if (positive) {
            Document range = new Document();
            range.put(QueryOperators.GTE, left);
            range.put(QueryOperators.LTE, right);
            return newDocumentWithField(fieldInfo, range);
        } else {
            Document a = newDocumentWithField(fieldInfo, new Document(QueryOperators.LT, left));
            Document b = newDocumentWithField(fieldInfo, new Document(QueryOperators.GT, right));
            return new Document(QueryOperators.OR, Arrays.asList(a, b));
        }
    }

    public Document walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        if (!(right instanceof List)) {
            throw new QueryParseException("Invalid IN, right hand side must be a list: " + rvalue);
        }
        if (isMixinTypes(fieldInfo)) {
            return walkMixinTypes((List<String>) right, positive);
        }
        // TODO check list fields
        List<Object> list = (List<Object>) right;
        return newDocumentWithField(fieldInfo, new Document(positive ? QueryOperators.IN : QueryOperators.NIN, list));
    }

    public Document walkLike(Operand lvalue, Operand rvalue, boolean positive, boolean caseInsensitive) {
        FieldInfo fieldInfo = walkReference(lvalue);
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException("Invalid LIKE/ILIKE, right hand side must be a string: " + rvalue);
        }
        // TODO check list fields
        String like = walkStringLiteral((StringLiteral) rvalue);
        String regex = ExpressionEvaluator.likeToRegex(like);
        // MongoDB native matches are unanchored: optimize the regex for faster matches
        if (regex.startsWith(".*")) {
            regex = regex.substring(2);
        } else if (likeAnchored) {
            regex = "^" + regex;
        }
        if (regex.endsWith(".*")) {
            regex = regex.substring(0, regex.length() - 2); // better range index use
        } else if (likeAnchored) {
            regex = regex + "$";
        }

        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
        Pattern pattern = Pattern.compile(regex, flags);
        Object value;
        if (positive) {
            value = pattern;
        } else {
            value = new Document(QueryOperators.NOT, pattern);
        }
        return newDocumentWithField(fieldInfo, value);
    }

    public Object walkOperand(Operand op) {
        if (op instanceof Literal) {
            return walkLiteral((Literal) op);
        } else if (op instanceof LiteralList) {
            return walkLiteralList((LiteralList) op);
        } else if (op instanceof Function) {
            return walkFunction((Function) op);
        } else if (op instanceof Expression) {
            return walkExpression((Expression) op);
        } else if (op instanceof Reference) {
            return walkReference((Reference) op);
        } else {
            throw new QueryParseException("Unknown operand: " + op);
        }
    }

    public Object walkLiteral(Literal lit) {
        if (lit instanceof BooleanLiteral) {
            return walkBooleanLiteral((BooleanLiteral) lit);
        } else if (lit instanceof DateLiteral) {
            return walkDateLiteral((DateLiteral) lit);
        } else if (lit instanceof DoubleLiteral) {
            return walkDoubleLiteral((DoubleLiteral) lit);
        } else if (lit instanceof IntegerLiteral) {
            return walkIntegerLiteral((IntegerLiteral) lit);
        } else if (lit instanceof StringLiteral) {
            return walkStringLiteral((StringLiteral) lit);
        } else {
            throw new QueryParseException("Unknown literal: " + lit);
        }
    }

    public Object walkBooleanLiteral(BooleanLiteral lit) {
        return Boolean.valueOf(lit.value);
    }

    public Date walkDateLiteral(DateLiteral lit) {
        return lit.value.toDate(); // TODO onlyDate
    }

    public Double walkDoubleLiteral(DoubleLiteral lit) {
        return Double.valueOf(lit.value);
    }

    public Long walkIntegerLiteral(IntegerLiteral lit) {
        return Long.valueOf(lit.value);
    }

    public String walkStringLiteral(StringLiteral lit) {
        return lit.value;
    }

    public List<Object> walkLiteralList(LiteralList litList) {
        List<Object> list = new ArrayList<>(litList.size());
        for (Literal lit : litList) {
            list.add(walkLiteral(lit));
        }
        return list;
    }

    protected List<Object> walkOperandList(List<Operand> values) {
        List<Object> list = new LinkedList<>();
        for (Operand value : values) {
            list.add(walkOperand(value));
        }
        return list;
    }

    public Object walkFunction(Function func) {
        throw new UnsupportedOperationException(func.name);
    }

    public Document walkStartsWith(Operand lvalue, Operand rvalue) {
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

    protected Document walkStartsWithPath(String path) {
        // resolve path
        String ancestorId = pathResolver.getIdForPath(path);
        if (ancestorId == null) {
            // no such path
            // TODO XXX do better
            return new Document(MONGODB_ID, "__nosuchid__");
        }
        return new Document(DBSDocument.KEY_ANCESTOR_IDS, ancestorId);
    }

    protected Document walkStartsWithNonPath(Operand lvalue, String path) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Document eq = newDocumentWithField(fieldInfo, path);
        // escape except alphanumeric and others not needing escaping
        String regex = path.replaceAll("([^a-zA-Z0-9 /])", "\\\\$1");
        Pattern pattern = Pattern.compile(regex + "/.*");
        Document like = newDocumentWithField(fieldInfo, pattern);
        return new Document(QueryOperators.OR, Arrays.asList(eq, like));
    }

    protected FieldInfo walkReference(Operand value) {
        if (!(value instanceof Reference)) {
            throw new QueryParseException("Invalid query, left hand side must be a property: " + value);
        }
        return walkReference((Reference) value);
    }

    // non-canonical index syntax, for replaceAll
    protected final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+|\\*|\\*\\d+)\\]" // index in brackets
    );

    /**
     * Canonicalizes a Nuxeo-xpath.
     * <p>
     * Replaces {@code a/foo[123]/b} with {@code a/123/b}
     * <p>
     * A star or a star followed by digits can be used instead of just the digits as well.
     *
     * @param xpath the xpath
     * @return the canonicalized xpath.
     */
    public static String canonicalXPath(String xpath) {
        while (xpath.length() > 0 && xpath.charAt(0) == '/') {
            xpath = xpath.substring(1);
        }
        if (xpath.indexOf('[') == -1) {
            return xpath;
        } else {
            return NON_CANON_INDEX.matcher(xpath).replaceAll("$1");
        }
    }

    protected static class FieldInfo {

        /** NXQL property. */
        protected final String prop;

        /** MongoDB field for query. foo/0/bar -> foo.0.bar; foo / * / bar -> foo.bar */
        protected final String queryField;

        /** MongoDB field for projection. */
        protected final String projectionField;

        protected final Type type;

        /**
         * Boolean system properties only use TRUE or NULL, not FALSE, so queries must be updated accordingly.
         */
        protected final boolean isTrueOrNullBoolean;

        protected FieldInfo(String prop, String queryField, String projectionField, Type type,
                boolean isTrueOrNullBoolean) {
            this.prop = prop;
            this.queryField = queryField;
            this.projectionField = projectionField;
            this.type = type;
            this.isTrueOrNullBoolean = isTrueOrNullBoolean;
        }

        protected boolean isBoolean() {
            return type instanceof BooleanType;
        }
    }

    /**
     * Returns the MongoDB field for this reference.
     */
    public FieldInfo walkReference(Reference ref) {
        FieldInfo fieldInfo = walkReference(ref.name);
        if (DATE_CAST.equals(ref.cast)) {
            Type type = fieldInfo.type;
            if (!(type instanceof DateType
                    || (type instanceof ListType && ((ListType) type).getFieldType() instanceof DateType))) {
                throw new QueryParseException("Cannot cast to " + ref.cast + ": " + ref.name);
            }
            // fieldInfo.isDateCast = true;
        }
        return fieldInfo;
    }

    protected FieldInfo walkReference(String name) {
        String prop = canonicalXPath(name);
        String[] parts = prop.split("/");
        if (prop.startsWith(NXQL.ECM_PREFIX)) {
            if (prop.startsWith(NXQL.ECM_ACL + "/")) {
                return parseACP(prop, parts);
            }
            if (prop.startsWith(NXQL.ECM_TAG)) {
                String queryField = FACETED_TAG + "." + FACETED_TAG_LABEL;
                queryField = stripElemMatchPrefix(queryField);
                return new FieldInfo(prop, queryField, queryField, StringType.INSTANCE, true);
            }
            // simple field
            String field = DBSSession.convToInternal(prop);
            Type type = DBSSession.getType(field);
            String queryField = converter.keyToBson(field);
            queryField = stripElemMatchPrefix(queryField);
            return new FieldInfo(prop, queryField, field, type, true);
        } else {
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
                    field = schema.getField(first);
                    if (field != null) {
                        break;
                    }
                }
                if (field == null) {
                    throw new QueryParseException("No such property: " + name);
                }
            }
            Type type = field.getType();
            if (PROP_UID_MAJOR_VERSION.equals(prop) || PROP_UID_MINOR_VERSION.equals(prop)
                    || PROP_MAJOR_VERSION.equals(prop) || PROP_MINOR_VERSION.equals(prop)) {
                String fieldName = DBSSession.convToInternal(prop);
                return new FieldInfo(prop, fieldName, fieldName, type, true);
            }

            // canonical name
            parts[0] = field.getName().getPrefixedName();
            // are there wildcards or list indexes?
            List<String> queryFieldParts = new LinkedList<>(); // field for query
            List<String> projectionFieldParts = new LinkedList<>(); // field for projection
            boolean firstPart = true;
            for (String part : parts) {
                if (NumberUtils.isDigits(part)) {
                    // explicit list index
                    queryFieldParts.add(part);
                    type = ((ListType) type).getFieldType();
                } else if (!part.startsWith("*")) {
                    // complex sub-property
                    queryFieldParts.add(part);
                    projectionFieldParts.add(part);
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
            String queryField = StringUtils.join(queryFieldParts, '.');
            String projectionField = StringUtils.join(projectionFieldParts, '.');
            queryField = stripElemMatchPrefix(queryField);
            return new FieldInfo(prop, queryField, projectionField, type, false);
        }
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
        String queryField;
        if (NXQL.ECM_ACL_NAME.equals(last)) {
            queryField = KEY_ACP + "." + KEY_ACL_NAME;
        } else {
            String fieldLast = DBSSession.convToInternalAce(last);
            if (fieldLast == null) {
                throw new QueryParseException("No such property: " + prop);
            }
            queryField = KEY_ACP + "." + KEY_ACL + "." + fieldLast;
        }
        Type type = DBSSession.getType(last);
        queryField = stripElemMatchPrefix(queryField);
        return new FieldInfo(prop, queryField, queryField, type, false);
    }

    protected boolean isMixinTypes(FieldInfo fieldInfo) {
        return fieldInfo.queryField.equals(DBSDocument.KEY_MIXIN_TYPES);
    }

    protected Set<String> getMixinDocumentTypes(String mixin) {
        Set<String> types = schemaManager.getDocumentTypeNamesForFacet(mixin);
        return types == null ? Collections.emptySet() : types;
    }

    protected List<String> getDocumentTypes() {
        // TODO precompute in SchemaManager
        if (documentTypes == null) {
            documentTypes = new ArrayList<>();
            for (DocumentType docType : schemaManager.getDocumentTypes()) {
                documentTypes.add(docType.getName());
            }
        }
        return documentTypes;
    }

    protected boolean isNeverPerInstanceMixin(String mixin) {
        return schemaManager.getNoPerDocumentQueryFacets().contains(mixin);
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
    public Document walkMixinTypes(List<String> mixins, boolean include) {
        /*
         * Primary types that match.
         */
        Set<String> matchPrimaryTypes;
        if (include) {
            matchPrimaryTypes = new HashSet<>();
            for (String mixin : mixins) {
                matchPrimaryTypes.addAll(getMixinDocumentTypes(mixin));
            }
        } else {
            matchPrimaryTypes = new HashSet<>(getDocumentTypes());
            for (String mixin : mixins) {
                matchPrimaryTypes.removeAll(getMixinDocumentTypes(mixin));
            }
        }
        /*
         * Instance mixins that match.
         */
        Set<String> matchMixinTypes = new HashSet<>();
        for (String mixin : mixins) {
            if (!isNeverPerInstanceMixin(mixin)) {
                matchMixinTypes.add(mixin);
            }
        }
        /*
         * MongoDB query generation.
         */
        // match on primary type
        Document p = new Document(DBSDocument.KEY_PRIMARY_TYPE, new Document(QueryOperators.IN, matchPrimaryTypes));
        // match on mixin types
        // $in/$nin with an array matches if any/no element of the array matches
        String innin = include ? QueryOperators.IN : QueryOperators.NIN;
        Document m = new Document(DBSDocument.KEY_MIXIN_TYPES, new Document(innin, matchMixinTypes));
        // and/or between those
        String op = include ? QueryOperators.OR : QueryOperators.AND;
        return new Document(op, Arrays.asList(p, m));
    }

}

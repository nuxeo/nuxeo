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
package org.nuxeo.ecm.core.storage.mongodb;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SCORE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_META;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_TEXT_SCORE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
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
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.runtime.api.Framework;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryOperators;

/**
 * Query builder for a MongoDB query from an {@link Expression}.
 *
 * @since 5.9.4
 */
public class MongoDBQueryBuilder {

    private static final Long ZERO = Long.valueOf(0);

    private static final Long ONE = Long.valueOf(1);

    private static final Long MINUS_ONE = Long.valueOf(-1);

    protected static final String DATE_CAST = "DATE";

    protected final AtomicInteger counter = new AtomicInteger();

    protected final SchemaManager schemaManager;

    protected final MongoDBRepository repository;

    protected final String idKey;

    protected List<String> documentTypes;

    protected final Expression expression;

    protected final SelectClause selectClause;

    protected final OrderByClause orderByClause;

    protected final PathResolver pathResolver;

    public boolean hasFulltext;

    public boolean sortOnFulltextScore;

    protected DBObject query;

    protected DBObject orderBy;

    protected DBObject projection;

    boolean projectionHasWildcard;

    private boolean fulltextSearchDisabled;

    public MongoDBQueryBuilder(MongoDBRepository repository, Expression expression, SelectClause selectClause,
            OrderByClause orderByClause, PathResolver pathResolver, boolean fulltextSearchDisabled) {
        schemaManager = Framework.getLocalService(SchemaManager.class);
        this.repository = repository;
        idKey = repository.idKey;
        this.expression = expression;
        this.selectClause = selectClause;
        this.orderByClause = orderByClause;
        this.pathResolver = pathResolver;
        this.fulltextSearchDisabled = fulltextSearchDisabled;
    }

    public void walk() {
        query = walkExpression(expression); // computes hasFulltext
        walkOrderBy(); // computes sortOnFulltextScore
        walkProjection(); // needs hasFulltext and sortOnFulltextScore
    }

    public DBObject getQuery() {
        return query;
    }

    public DBObject getOrderBy() {
        return orderBy;
    }

    public DBObject getProjection() {
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
            orderBy = new BasicDBObject();
            for (OrderByExpr ob : orderByClause.elements) {
                Reference ref = ob.reference;
                boolean desc = ob.isDescending;
                String field = walkReference(ref).queryField;
                if (!orderBy.containsField(field)) {
                    Object value;
                    if (KEY_FULLTEXT_SCORE.equals(field)) {
                        if (!desc) {
                            throw new QueryParseException("Cannot sort by " + NXQL.ECM_FULLTEXT_SCORE + " ascending");
                        }
                        sortOnFulltextScore = true;
                        value = new BasicDBObject(MONGODB_META, MONGODB_TEXT_SCORE);
                    } else {
                        value = desc ? MINUS_ONE : ONE;
                    }
                    orderBy.put(field, value);
                }
            }
            if (sortOnFulltextScore && ((BasicDBObject) orderBy).size() > 1) {
                throw new QueryParseException("Cannot sort by " + NXQL.ECM_FULLTEXT_SCORE + " and other criteria");
            }
        }
    }

    protected void walkProjection() {
        projection = new BasicDBObject();
        projection.put(idKey, ONE); // always useful
        projection.put(KEY_NAME, ONE); // used in ORDER BY ecm:path
        projection.put(KEY_PARENT_ID, ONE); // used in ORDER BY ecm:path
        boolean projectionOnFulltextScore = false;
        for (Operand op : selectClause.getSelectList().values()) {
            if (!(op instanceof Reference)) {
                throw new QueryParseException("Projection not supported: " + op);
            }
            FieldInfo fieldInfo = walkReference((Reference) op);
            projection.put(fieldInfo.projectionField, ONE);
            if (fieldInfo.hasWildcard) {
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
            projection.put(KEY_FULLTEXT_SCORE, new BasicDBObject(MONGODB_META, MONGODB_TEXT_SCORE));
        }
    }

    public DBObject walkExpression(Expression expr) {
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
                return walkMultiExpression((MultiExpression) expr);
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

    protected DBObject walkEcmPath(Operator op, Operand rvalue) {
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
            return new BasicDBObject(MONGODB_ID, "__nosuchid__");
        }
        if (op == Operator.EQ) {
            return new BasicDBObject(idKey, id);
        } else {
            return new BasicDBObject(idKey, new BasicDBObject(QueryOperators.NE, id));
        }
    }

    protected DBObject walkAncestorId(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires = or <> operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires literal id as right argument");
        }
        String ancestorId = ((StringLiteral) rvalue).value;
        if (op == Operator.EQ) {
            return new BasicDBObject(DBSDocument.KEY_ANCESTOR_IDS, ancestorId);
        } else {
            return new BasicDBObject(DBSDocument.KEY_ANCESTOR_IDS, new BasicDBObject(QueryOperators.NE, ancestorId));
        }
    }

    protected DBObject walkEcmFulltext(String name, Operator op, Operand rvalue) {
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
                return new BasicDBObject(MONGODB_ID, "__nosuchid__");
            }
            DBObject textSearch = new BasicDBObject();
            textSearch.put(QueryOperators.SEARCH, ft);
            // TODO language?
            return new BasicDBObject(QueryOperators.TEXT, textSearch);
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

    public DBObject walkNot(Operand value) {
        Object val = walkOperand(value);
        Object not = pushDownNot(val);
        if (!(not instanceof DBObject)) {
            throw new QueryParseException("Cannot do NOT on: " + val);
        }
        return (DBObject) not;
    }

    protected Object pushDownNot(Object object) {
        if (!(object instanceof DBObject)) {
            throw new QueryParseException("Cannot do NOT on: " + object);
        }
        DBObject ob = (DBObject) object;
        Set<String> keySet = ob.keySet();
        if (keySet.size() != 1) {
            throw new QueryParseException("Cannot do NOT on: " + ob);
        }
        String key = keySet.iterator().next();
        Object value = ob.get(key);
        if (!key.startsWith("$")) {
            if (value instanceof DBObject) {
                // push down inside dbobject
                return new BasicDBObject(key, pushDownNot(value));
            } else {
                // k = v -> k != v
                return new BasicDBObject(key, new BasicDBObject(QueryOperators.NE, value));
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
            return new BasicDBObject(op, list);
        }
        if (QueryOperators.IN.equals(key) || QueryOperators.NIN.equals(key)) {
            // boolean algebra
            // IN <-> NIN
            String op = QueryOperators.IN.equals(key) ? QueryOperators.NIN : QueryOperators.IN;
            return new BasicDBObject(op, value);
        }
        if (QueryOperators.LT.equals(key) || QueryOperators.GT.equals(key) || QueryOperators.LTE.equals(key)
                || QueryOperators.GTE.equals(key)) {
            // TODO use inverse operators?
            return new BasicDBObject(QueryOperators.NOT, ob);
        }
        throw new QueryParseException("Unknown operator for NOT: " + key);
    }

    public DBObject walkIsNull(Operand value) {
        FieldInfo fieldInfo = walkReference(value);
        return new FieldInfoDBObject(fieldInfo, null);
    }

    public DBObject walkIsNotNull(Operand value) {
        FieldInfo fieldInfo = walkReference(value);
        return new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.NE, null));
    }

    public DBObject walkMultiExpression(MultiExpression expr) {
        return walkAnd(expr.values);
    }

    public DBObject walkAnd(Operand lvalue, Operand rvalue) {
        return walkAnd(Arrays.asList(lvalue, rvalue));
    }

    protected DBObject walkAnd(List<Operand> values) {
        List<Object> list = walkOperandList(values);
        // check wildcards in the operands, extract common prefixes to use $elemMatch
        Map<String, List<FieldInfoDBObject>> propBaseKeyToDBOs = new LinkedHashMap<>();
        Map<String, String> propBaseKeyToFieldBase = new HashMap<>();
        for (Iterator<Object> it = list.iterator(); it.hasNext();) {
            Object ob = it.next();
            if (ob instanceof FieldInfoDBObject) {
                FieldInfoDBObject fidbo = (FieldInfoDBObject) ob;
                FieldInfo fieldInfo = fidbo.fieldInfo;
                if (fieldInfo.hasWildcard) {
                    if (fieldInfo.fieldSuffix != null && fieldInfo.fieldSuffix.contains("*")) {
                        // a double wildcard of the form foo/*/bar/* is not a problem if bar is an array
                        // TODO prevent deep complex multiple wildcards
                        // throw new QueryParseException("Cannot use two wildcards: " + fieldInfo.prop);
                    }
                    // generate a key unique per correlation for this element match
                    String wildcardNumber = fieldInfo.fieldWildcard;
                    if (wildcardNumber.isEmpty()) {
                        // negative to not collide with regular correlated wildcards
                        wildcardNumber = String.valueOf(-counter.incrementAndGet());
                    }
                    String propBaseKey = fieldInfo.fieldPrefix + "/*" + wildcardNumber;
                    // store object for this key
                    List<FieldInfoDBObject> dbos = propBaseKeyToDBOs.get(propBaseKey);
                    if (dbos == null) {
                        propBaseKeyToDBOs.put(propBaseKey, dbos = new LinkedList<>());
                    }
                    dbos.add(fidbo);
                    // remember for which field base this is
                    String fieldBase = fieldInfo.fieldPrefix.replace("/", ".");
                    propBaseKeyToFieldBase.put(propBaseKey, fieldBase);
                    // remove from list, will be re-added later through propBaseKeyToDBOs
                    it.remove();
                }
            }
        }
        // generate $elemMatch items for correlated queries
        for (Entry<String, List<FieldInfoDBObject>> es : propBaseKeyToDBOs.entrySet()) {
            String propBaseKey = es.getKey();
            List<FieldInfoDBObject> fidbos = es.getValue();
            if (fidbos.size() == 1) {
                // regular uncorrelated match
                list.addAll(fidbos);
            } else {
                DBObject elemMatch = new BasicDBObject();
                for (FieldInfoDBObject fidbo : fidbos) {
                    // truncate field name to just the suffix
                    FieldInfo fieldInfo = fidbo.fieldInfo;
                    Object value = fidbo.get(fieldInfo.queryField);
                    String fieldSuffix = fieldInfo.fieldSuffix.replace("/", ".");
                    if (elemMatch.containsField(fieldSuffix)) {
                        // ecm:acl/*1/principal = 'bob' AND ecm:acl/*1/principal = 'steve'
                        // cannot match
                        // TODO do better
                        value = "__NOSUCHVALUE__";
                    }
                    elemMatch.put(fieldSuffix, value);
                }
                String fieldBase = propBaseKeyToFieldBase.get(propBaseKey);
                BasicDBObject dbo = new BasicDBObject(fieldBase,
                        new BasicDBObject(QueryOperators.ELEM_MATCH, elemMatch));
                list.add(dbo);
            }
        }
        if (list.size() == 1) {
            return (DBObject) list.get(0);
        } else {
            return new BasicDBObject(QueryOperators.AND, list);
        }
    }

    public DBObject walkOr(Operand lvalue, Operand rvalue) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        List<Object> list = new ArrayList<>(Arrays.asList(left, right));
        return new BasicDBObject(QueryOperators.OR, list);
    }

    protected Object checkBoolean(FieldInfo fieldInfo, Object right) {
        if (fieldInfo.isBoolean()) {
            // convert 0 / 1 to actual booleans
            if (right instanceof Long) {
                if (ZERO.equals(right)) {
                    right = fieldInfo.isTrueOrNullBoolean ? null : FALSE;
                } else if (ONE.equals(right)) {
                    right = TRUE;
                } else {
                    throw new QueryParseException("Invalid boolean: " + right);
                }
            }
        }
        return right;
    }

    public DBObject walkEq(Operand lvalue, Operand rvalue) {
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
        return new FieldInfoDBObject(fieldInfo, right);
    }

    public DBObject walkNotEq(Operand lvalue, Operand rvalue) {
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
        return new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.NE, right));
    }

    public DBObject walkLt(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.LT, right));
    }

    public DBObject walkGt(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.GT, right));
    }

    public DBObject walkLtEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.LTE, right));
    }

    public DBObject walkGtEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.GTE, right));
    }

    public DBObject walkBetween(Operand lvalue, Operand rvalue, boolean positive) {
        LiteralList l = (LiteralList) rvalue;
        FieldInfo fieldInfo = walkReference(lvalue);
        Object left = walkOperand(l.get(0));
        Object right = walkOperand(l.get(1));
        if (positive) {
            DBObject range = new BasicDBObject();
            range.put(QueryOperators.GTE, left);
            range.put(QueryOperators.LTE, right);
            return new FieldInfoDBObject(fieldInfo, range);
        } else {
            DBObject a = new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.LT, left));
            DBObject b = new FieldInfoDBObject(fieldInfo, new BasicDBObject(QueryOperators.GT, right));
            return new BasicDBObject(QueryOperators.OR, Arrays.asList(a, b));
        }
    }

    public DBObject walkIn(Operand lvalue, Operand rvalue, boolean positive) {
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
        return new FieldInfoDBObject(fieldInfo,
                new BasicDBObject(positive ? QueryOperators.IN : QueryOperators.NIN, list));
    }

    public DBObject walkLike(Operand lvalue, Operand rvalue, boolean positive, boolean caseInsensitive) {
        FieldInfo fieldInfo = walkReference(lvalue);
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException("Invalid LIKE/ILIKE, right hand side must be a string: " + rvalue);
        }
        // TODO check list fields
        String like = walkStringLiteral((StringLiteral) rvalue);
        String regex = ExpressionEvaluator.likeToRegex(like);

        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
        Pattern pattern = Pattern.compile(regex, flags);
        Object value;
        if (positive) {
            value = pattern;
        } else {
            value = new BasicDBObject(QueryOperators.NOT, pattern);
        }
        return new FieldInfoDBObject(fieldInfo, value);
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

    public DBObject walkStartsWith(Operand lvalue, Operand rvalue) {
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

    protected DBObject walkStartsWithPath(String path) {
        // resolve path
        String ancestorId = pathResolver.getIdForPath(path);
        if (ancestorId == null) {
            // no such path
            // TODO XXX do better
            return new BasicDBObject(MONGODB_ID, "__nosuchid__");
        }
        return new BasicDBObject(DBSDocument.KEY_ANCESTOR_IDS, ancestorId);
    }

    protected DBObject walkStartsWithNonPath(Operand lvalue, String path) {
        FieldInfo fieldInfo = walkReference(lvalue);
        DBObject eq = new FieldInfoDBObject(fieldInfo, path);
        // escape except alphanumeric and others not needing escaping
        String regex = path.replaceAll("([^a-zA-Z0-9 /])", "\\\\$1");
        Pattern pattern = Pattern.compile(regex + "/.*");
        DBObject like = new FieldInfoDBObject(fieldInfo, pattern);
        return new BasicDBObject(QueryOperators.OR, Arrays.asList(eq, like));
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

    /** Splits foo.*.bar into foo, *, bar and split foo.*1.bar into foo, *1, bar with the last bar part optional */
    protected final static Pattern WILDCARD_SPLIT = Pattern.compile("([^*]*)\\.\\*(\\d*)(?:\\.(.*))?");

    protected static class FieldInfo {

        /** NXQL property. */
        protected final String prop;

        /** MongoDB field including wildcards (not used as-is). */
        protected final String fullField;

        /** MongoDB field for query. foo/0/bar -> foo.0.bar; foo / * / bar -> foo.bar */
        protected final String queryField;

        /** MongoDB field for projection. */
        protected final String projectionField;

        protected final Type type;

        /**
         * Boolean system properties only use TRUE or NULL, not FALSE, so queries must be updated accordingly.
         */
        protected final boolean isTrueOrNullBoolean;

        protected final boolean hasWildcard;

        /** Prefix before the wildcard. */
        protected final String fieldPrefix;

        /** Wildcard part after * */
        protected final String fieldWildcard;

        /** Part after wildcard, may be null. */
        protected final String fieldSuffix;

        protected FieldInfo(String prop, String fullField, String queryField, String projectionField, Type type,
                boolean isTrueOrNullBoolean) {
            this.prop = prop;
            this.fullField = fullField;
            this.queryField = queryField;
            this.projectionField = projectionField;
            this.type = type;
            this.isTrueOrNullBoolean = isTrueOrNullBoolean;
            Matcher m = WILDCARD_SPLIT.matcher(fullField);
            if (m.matches()) {
                hasWildcard = true;
                fieldPrefix = m.group(1);
                fieldWildcard = m.group(2);
                fieldSuffix = m.group(3);
            } else {
                hasWildcard = false;
                fieldPrefix = fieldWildcard = fieldSuffix = null;
            }
        }

        protected boolean isBoolean() {
            return type instanceof BooleanType;
        }
    }

    protected static class FieldInfoDBObject extends BasicDBObject {

        private static final long serialVersionUID = 1L;

        protected FieldInfo fieldInfo;

        public FieldInfoDBObject(FieldInfo fieldInfo, Object value) {
            super(fieldInfo.queryField, value);
            this.fieldInfo = fieldInfo;
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
            // simple field
            String field = DBSSession.convToInternal(prop);
            Type type = DBSSession.getType(field);
            String queryField = repository.keyToBson(field);
            return new FieldInfo(prop, field, queryField, field, type, true);
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
            String fullField = StringUtils.join(parts, '.');
            String queryField = StringUtils.join(queryFieldParts, '.');
            String projectionField = StringUtils.join(projectionFieldParts, '.');
            return new FieldInfo(prop, fullField, queryField, projectionField, type, false);
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
        String fullField;
        String queryField;
        String projectionField;
        if (NXQL.ECM_ACL_NAME.equals(last)) {
            fullField = KEY_ACP + "." + KEY_ACL_NAME;
            queryField = KEY_ACP + "." + KEY_ACL_NAME;
            // TODO remember wildcard correlation
        } else {
            String fieldLast = DBSSession.convToInternalAce(last);
            if (fieldLast == null) {
                throw new QueryParseException("No such property: " + prop);
            }
            fullField = KEY_ACP + "." + KEY_ACL + "." + wildcard + "." + fieldLast;
            queryField = KEY_ACP + "." + KEY_ACL + "." + fieldLast;
        }
        Type type = DBSSession.getType(last);
        projectionField = queryField;
        return new FieldInfo(prop, fullField, queryField, projectionField, type, false);
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
    public DBObject walkMixinTypes(List<String> mixins, boolean include) {
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
        DBObject p = new BasicDBObject(DBSDocument.KEY_PRIMARY_TYPE,
                new BasicDBObject(QueryOperators.IN, matchPrimaryTypes));
        // match on mixin types
        // $in/$nin with an array matches if any/no element of the array matches
        String innin = include ? QueryOperators.IN : QueryOperators.NIN;
        DBObject m = new BasicDBObject(DBSDocument.KEY_MIXIN_TYPES, new BasicDBObject(innin, matchMixinTypes));
        // and/or between those
        String op = include ? QueryOperators.OR : QueryOperators.AND;
        return new BasicDBObject(op, Arrays.asList(p, m));
    }

}

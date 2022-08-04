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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.Document;
import org.joda.time.DateTime;
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
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;
import org.nuxeo.ecm.core.storage.QueryOptimizer.PrefixInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.mongodb.QueryOperators;

/**
 * Abstract query builder for a MongoDB query from an {@link Expression}.
 * <p>
 * Must be customized by defining an implementation for the {@link #walkReference(String)} method.
 *
 * @since 5.9.4
 */
public abstract class MongoDBAbstractQueryBuilder {

    public static final Long LONG_ZERO = Long.valueOf(0);

    public static final Long LONG_ONE = Long.valueOf(1);

    public static final Double ONE = Double.valueOf(1);

    public static final Double MINUS_ONE = Double.valueOf(-1);

    protected static final String DATE_CAST = "DATE";

    protected static final String LIKE_ANCHORED_PROP = "nuxeo.mongodb.like.anchored";

    protected final MongoDBConverter converter;

    protected final Expression expression;

    protected Document query;

    /**
     * Prefix to remove for $elemMatch (including final dot), or {@code null} if there's no current prefix to remove.
     */
    protected String elemMatchPrefix;

    protected boolean likeAnchored;

    public MongoDBAbstractQueryBuilder(MongoDBConverter converter, Expression expression) {
        this.converter = converter;
        this.expression = expression;
        likeAnchored = !Framework.getService(ConfigurationService.class).isBooleanPropertyFalse(LIKE_ANCHORED_PROP);
    }

    public void walk() {
        if (expression instanceof MultiExpression && ((MultiExpression) expression).predicates.isEmpty()) {
            // special-case empty query
            query = new Document();
        } else {
            query = walkExpression(expression);
        }
    }

    public Document getQuery() {
        return query;
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
        if (op == Operator.SUM) {
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
        } else if (op == Operator.AND || op == Operator.OR) {
            if (expr instanceof MultiExpression) {
                return walkAndOrMultiExpression((MultiExpression) expr);
            } else {
                return walkAndOr(expr);
            }
        } else if (op == Operator.NOT) {
            return walkNot(lvalue);
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

    public Document walkNot(Operand value) {
        Object val = walkOperand(null, value);
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

    public Document walkAndOrMultiExpression(MultiExpression expr) {
        return walkAndOr(expr, expr.predicates);
    }

    public Document walkAndOr(Expression expr) {
        return walkAndOr(expr, Arrays.asList(expr.lvalue, expr.rvalue));
    }

    protected static final Pattern SLASH_WILDCARD_SLASH = Pattern.compile("/\\*\\d+(/)?");

    protected Document walkAndOr(Expression expr, List<? extends Operand> values) {
        if (values.size() == 1) {
            return (Document) walkOperand(null, values.get(0));
        }
        boolean and = expr.operator == Operator.AND;
        String op = and ? QueryOperators.AND : QueryOperators.OR;
        // PrefixInfo was computed by the QueryOptimizer for common AND predicates
        PrefixInfo info = (PrefixInfo) expr.getInfo();
        if (info == null || info.count < 2 || !and) {
            List<Object> list = walkOperandList(values);
            return new Document(op, list);
        }

        // we have a common prefix for all underlying references, extract it into an $elemMatch node

        String prefix = getMongoDBPrefix(info.prefix);
        String fieldBase = stripElemMatchPrefix(prefix.substring(0, prefix.length() - 1));

        String previousElemMatchPrefix = elemMatchPrefix;
        elemMatchPrefix = prefix;
        List<Object> list = walkOperandList(values);
        elemMatchPrefix = previousElemMatchPrefix;

        return new Document(fieldBase, new Document(QueryOperators.ELEM_MATCH, new Document(op, list)));
    }

    /**
     * Computes the MongoDB prefix from the DBS common prefix.
     *
     * <pre>{@code
     * foo/bar/*1 -> foo.bar.
     * ecm:acp/*1/acl/*1 -> ecm:acp.acl.
     * }</pre>
     */
    // overridden for repository to also strip prefix from unprefixed schemas (files:files/*1 -> files.)
    protected String getMongoDBPrefix(String prefix) {
        return SLASH_WILDCARD_SLASH.matcher(prefix).replaceAll(".");
    }

    // remove current prefix and trailing . for actual field match
    protected String stripElemMatchPrefix(String field) {
        if (elemMatchPrefix != null && field.startsWith(elemMatchPrefix)) {
            field = field.substring(elemMatchPrefix.length());
        }
        return field;
    }

    public Document walkEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        return walkEq(fieldInfo, rvalue);
    }

    public Document walkEq(FieldInfo fieldInfo, Operand rvalue) {
        Object right = walkOperand(fieldInfo, rvalue);
        return newDocumentWithField(fieldInfo, right);
    }

    public Document walkNotEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        return walkNotEq(fieldInfo, rvalue);
    }

    public Document walkNotEq(FieldInfo fieldInfo, Operand rvalue) {
        Object right = walkOperand(fieldInfo, rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.NE, right));
    }

    public Document walkLt(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(fieldInfo, rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.LT, right));
    }

    public Document walkGt(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(fieldInfo, rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.GT, right));
    }

    public Document walkLtEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(fieldInfo, rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.LTE, right));
    }

    public Document walkGtEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(fieldInfo, rvalue);
        return newDocumentWithField(fieldInfo, new Document(QueryOperators.GTE, right));
    }

    public Document walkBetween(Operand lvalue, Operand rvalue, boolean positive) {
        LiteralList l = (LiteralList) rvalue;
        FieldInfo fieldInfo = walkReference(lvalue);
        Object left = walkOperand(fieldInfo, l.get(0));
        Object right = walkOperand(fieldInfo, l.get(1));
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
        return walkIn(fieldInfo, rvalue, positive);
    }

    public Document walkIn(FieldInfo fieldInfo, Operand rvalue, boolean positive) {
        Object right = walkOperand(fieldInfo, rvalue);
        if (!(right instanceof List)) {
            throw new QueryParseException("Invalid IN, right hand side must be a list: " + rvalue);
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
        String like = (String) walkStringLiteral(fieldInfo, (StringLiteral) rvalue);
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

    public Object walkOperand(FieldInfo fieldInfo, Operand op) {
        if (op instanceof Literal) {
            return walkLiteral(fieldInfo, (Literal) op);
        } else if (op instanceof LiteralList) {
            return walkLiteralList(fieldInfo, (LiteralList) op);
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

    public Object walkLiteral(FieldInfo fieldInfo, Literal lit) {
        if (lit instanceof BooleanLiteral) {
            return walkBooleanLiteral(fieldInfo, (BooleanLiteral) lit);
        } else if (lit instanceof DateLiteral) {
            return walkDateLiteral(fieldInfo, (DateLiteral) lit);
        } else if (lit instanceof DoubleLiteral) {
            return walkDoubleLiteral(fieldInfo, (DoubleLiteral) lit);
        } else if (lit instanceof IntegerLiteral) {
            return walkIntegerLiteral(fieldInfo, (IntegerLiteral) lit);
        } else if (lit instanceof StringLiteral) {
            return walkStringLiteral(fieldInfo, (StringLiteral) lit);
        } else {
            throw new QueryParseException("Unknown literal: " + lit);
        }
    }

    public Object walkBooleanLiteral(FieldInfo fieldInfo, BooleanLiteral lit) {
        return Boolean.valueOf(lit.value);
    }

    public Date walkDateLiteral(FieldInfo fieldInfo, DateLiteral lit) {
        return lit.value.toDate(); // TODO onlyDate
    }

    public Double walkDoubleLiteral(FieldInfo fieldInfo, DoubleLiteral lit) {
        return Double.valueOf(lit.value);
    }

    public Object walkIntegerLiteral(FieldInfo fieldInfo, IntegerLiteral lit) {
        long value = lit.value;
        if (fieldInfo != null && fieldInfo.isBoolean()) {
            // convert 0 / 1 to actual booleans
            Boolean b;
            if (value == 0) {
                b = FALSE;
            } else if (value == 1) {
                b = TRUE;
            } else {
                throw new QueryParseException("Invalid boolean: " + value);
            }
            return converter.serializableToBson(fieldInfo.key, b);
        }
        return Long.valueOf(value);
    }

    public Object walkStringLiteral(FieldInfo fieldInfo, StringLiteral lit) {
        String value = lit.value;
        if (fieldInfo != null) {
            return converter.serializableToBson(fieldInfo.key, value);
        }
        return value;
    }

    public List<Object> walkLiteralList(FieldInfo fieldInfo, LiteralList litList) {
        List<Object> list = new ArrayList<>(litList.size());
        for (Literal lit : litList) {
            list.add(walkLiteral(fieldInfo, lit));
        }
        return list;
    }

    protected List<Object> walkOperandList(List<? extends Operand> values) {
        List<Object> list = new LinkedList<>();
        for (Operand value : values) {
            list.add(walkOperand(null, value));
        }
        return list;
    }

    public Object walkFunction(Function func) {
        String name = func.name;
        if (NXQL.NOW_FUNCTION.equalsIgnoreCase(name)) {
            String periodAndDurationText;
            if (func.args == null || func.args.size() != 1) {
                periodAndDurationText = null;
            } else {
                periodAndDurationText = ((StringLiteral) func.args.get(0)).value;
            }
            DateTime dateTime;
            try {
                dateTime = NXQL.nowPlusPeriodAndDuration(periodAndDurationText);
            } catch (IllegalArgumentException e) {
                throw new QueryParseException(e);
            }
            DateLiteral dateLiteral = new DateLiteral(dateTime);
            return walkDateLiteral(null, dateLiteral);
        } else {
            throw new QueryParseException("Function not supported: " + func);
        }
    }

    protected FieldInfo walkReference(Operand value) {
        if (!(value instanceof Reference)) {
            throw new QueryParseException("Invalid query, left hand side must be a property: " + value);
        }
        return walkReference((Reference) value);
    }

    public static class FieldInfo {

        /** NXQL property. */
        public final String prop;

        /**
         * DBS key.
         *
         * @since 11.1
         */
        public final String key;

        /** MongoDB field for query. foo/0/bar -> foo.0.bar; foo / * / bar -> foo.bar */
        public final String queryField;

        /** MongoDB field for projection. */
        public final String projectionField;

        public final Type type;

        public FieldInfo(String prop, String key, String queryField, String projectionField, Type type) {
            this.prop = prop;
            this.key = key;
            this.queryField = queryField;
            this.projectionField = projectionField;
            this.type = type;
        }

        public boolean isBoolean() {
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

    /**
     * Walks a reference, and returns field info about it.
     */
    protected abstract FieldInfo walkReference(String name);

}

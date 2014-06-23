/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.storage.dbs.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.dbs.FulltextQueryAnalyzer.FulltextQuery;
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

    protected final SchemaManager schemaManager;

    protected final PathResolver pathResolver;

    public MongoDBQueryBuilder(PathResolver pathResolver) {
        schemaManager = Framework.getLocalService(SchemaManager.class);
        this.pathResolver = pathResolver;
    }

    public DBObject walkExpression(Expression expr) {
        Operator op = expr.operator;
        Operand lvalue = expr.lvalue;
        Operand rvalue = expr.rvalue;
        String name = lvalue instanceof Reference ? ((Reference) lvalue).name
                : null;
        if (op == Operator.STARTSWITH) {
            return walkStartsWith(lvalue, rvalue);
        } else if (NXQL.ECM_PATH.equals(name)) {
            return walkEcmPath(op, rvalue);
        } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT)
                && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
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
            throw new UnsupportedOperationException("BETWEEN");
        } else if (op == Operator.NOTBETWEEN) {
            throw new UnsupportedOperationException("NOT BETWEEN");
        } else {
            throw new RuntimeException("Unknown operator: " + op);
        }
    }

    protected DBObject walkEcmPath(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new RuntimeException(NXQL.ECM_PATH
                    + " requires = or <> operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new RuntimeException(NXQL.ECM_PATH
                    + " requires literal path as right argument");
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
        String field = walkReference(new Reference(NXQL.ECM_UUID)).field;
        if (op == Operator.EQ) {
            return new BasicDBObject(field, id);
        } else {
            return new BasicDBObject(field, new BasicDBObject(
                    QueryOperators.NE, id));
        }
    }

    protected DBObject walkEcmFulltext(String name, Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.LIKE) {
            throw new RuntimeException(NXQL.ECM_FULLTEXT
                    + " requires = or LIKE operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new RuntimeException(NXQL.ECM_FULLTEXT
                    + " requires literal string as right argument");
        }
        String fulltextQuery = ((StringLiteral) rvalue).value;
        if (name.equals(NXQL.ECM_FULLTEXT)) {
            // standard fulltext query
            fulltextQuery = getMongoDBFulltextQuery(fulltextQuery);
            DBObject textSearch = new BasicDBObject();
            textSearch.put(QueryOperators.SEARCH, fulltextQuery);
            // TODO language?
            return new BasicDBObject(QueryOperators.TEXT, textSearch);
        } else {
            // secondary index match with explicit field
            // do a regexp on the field
            if (name.charAt(NXQL.ECM_FULLTEXT.length()) != '.') {
                throw new RuntimeException(name + " has incorrect syntax"
                        + " for a secondary fulltext index");
            }
            String prop = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
            fulltextQuery = fulltextQuery.replace(" ", "%");
            rvalue = new StringLiteral(fulltextQuery);
            return walkLike(new Reference(prop), rvalue, true, true);
        }
    }

    protected String getMongoDBFulltextQuery(String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        return FulltextQueryAnalyzer.translateFulltext(ft, " ", " ", " -",
                "\"", "\"", Collections.<Character> emptySet(), "\"", "\"",
                false);
    }

    public DBObject walkNot(Operand value) {
        Object val = walkOperand(value);
        Object not = pushDownNot(val);
        if (!(not instanceof DBObject)) {
            throw new RuntimeException("Cannot do NOT on: " + val);
        }
        return (DBObject) not;
    }

    protected Object pushDownNot(Object object) {
        if (!(object instanceof DBObject)) {
            throw new RuntimeException("Cannot do NOT on: " + object);
        }
        DBObject ob = (DBObject) object;
        Set<String> keySet = ob.keySet();
        if (keySet.size() != 1) {
            throw new RuntimeException("Cannot do NOT on: " + ob);
        }
        String key = keySet.iterator().next();
        Object value = ob.get(key);
        if (!key.startsWith("$")) {
            // k = v -> k != v
            return new BasicDBObject(key, new BasicDBObject(QueryOperators.NE,
                    value));
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
            String op = QueryOperators.AND.equals(key) ? QueryOperators.OR
                    : QueryOperators.AND;
            List<Object> list = (List<Object>) value;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, pushDownNot(list.get(i)));
            }
            return new BasicDBObject(op, list);
        }
        if (QueryOperators.IN.equals(key) || QueryOperators.NIN.equals(key)) {
            // boolean algebra
            // IN <-> NIN
            String op = QueryOperators.IN.equals(key) ? QueryOperators.NIN
                    : QueryOperators.IN;
            return new BasicDBObject(op, value);
        }
        if (QueryOperators.LT.equals(key) || QueryOperators.GT.equals(key)
                || QueryOperators.LTE.equals(key)
                || QueryOperators.GTE.equals(key)) {
            return new BasicDBObject(QueryOperators.NOT, ob);
        }
        throw new RuntimeException("Unknown operator for NOT: " + key);
    }

    public DBObject walkIsNull(Operand value) {
        String field = walkReference(value).field;
        return new BasicDBObject(field, null);
    }

    public DBObject walkIsNotNull(Operand value) {
        String field = walkReference(value).field;
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.NE,
                null));
    }

    public DBObject walkMultiExpression(MultiExpression expr) {
        return walkAnd(expr.values);
    }

    public DBObject walkAnd(Operand lvalue, Operand rvalue) {
        return walkAnd(Arrays.asList(lvalue, rvalue));
    }

    protected DBObject walkAnd(List<Operand> values) {
        List<Object> list = walkOperandList(values);
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
        if (fieldInfo.isBoolean) {
            // convert 0 / 1 to actual booleans
            if (right instanceof Long) {
                if (ZERO.equals(right)) {
                    right = fieldInfo.isTrueOrNullBoolean ? null : FALSE;
                } else if (ONE.equals(right)) {
                    right = TRUE;
                } else {
                    throw new RuntimeException("Invalid boolean: " + right);
                }
            }
        }
        return right;
    }

    public DBObject walkEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        right = checkBoolean(fieldInfo, right);
        // TODO check list fields
        return new BasicDBObject(fieldInfo.field, right);
    }

    public DBObject walkNotEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        right = checkBoolean(fieldInfo, right);
        // TODO check list fields
        return new BasicDBObject(fieldInfo.field, new BasicDBObject(
                QueryOperators.NE, right));
    }

    public DBObject walkLt(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue).field;
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.LT,
                right));
    }

    public DBObject walkGt(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue).field;
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.GT,
                right));
    }

    public DBObject walkLtEq(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue).field;
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.LTE,
                right));
    }

    public DBObject walkGtEq(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue).field;
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.GTE,
                right));
    }

    public DBObject walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        String field = walkReference(lvalue).field;
        Object right = walkOperand(rvalue);
        if (!(right instanceof List)) {
            throw new RuntimeException(
                    "Invalid IN, right hand side must be a list: " + rvalue);
        }
        // TODO check list fields
        List<Object> list = (List<Object>) right;
        return new BasicDBObject(field, new BasicDBObject(
                positive ? QueryOperators.IN : QueryOperators.NIN, list));
    }

    public DBObject walkLike(Operand lvalue, Operand rvalue, boolean positive,
            boolean caseInsensitive) {
        String field = walkReference(lvalue).field;
        if (!(rvalue instanceof StringLiteral)) {
            throw new RuntimeException(
                    "Invalid LIKE/ILIKE, right hand side must be a string: "
                            + rvalue);
        }
        // TODO check list fields
        String like = walkStringLiteral((StringLiteral) rvalue);
        // escape with slash except alphanumeric and percent
        String regex = like.replaceAll("([^a-zA-Z0-9%])", "\\\\$1");
        // replace percent with regexp
        regex = regex.replaceAll("%", ".*");

        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
        Pattern pattern = Pattern.compile(regex, flags);
        Object value;
        if (positive) {
            value = pattern;
        } else {
            value = new BasicDBObject(QueryOperators.NOT, pattern);
        }
        return new BasicDBObject(field, value);
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
            throw new RuntimeException("Unknown operand: " + op);
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
            throw new RuntimeException("Unknown literal: " + lit);
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
        List<Object> list = new ArrayList<Object>(litList.size());
        for (Literal lit : litList) {
            list.add(walkLiteral(lit));
        }
        return list;
    }

    protected List<Object> walkOperandList(List<Operand> values) {
        List<Object> list = new ArrayList<>(values.size());
        for (Operand value : values) {
            list.add(walkOperand(value));
        }
        return list;
    }

    public Object walkFunction(Function func) {
        throw new UnsupportedOperationException("Function");
    }

    public DBObject walkStartsWith(Operand lvalue, Operand rvalue) {
        if (!(lvalue instanceof Reference)) {
            throw new RuntimeException(
                    "Invalid STARTSWITH query, left hand side must be a property: "
                            + lvalue);
        }
        String name = ((Reference) lvalue).name;
        if (!(rvalue instanceof StringLiteral)) {
            throw new RuntimeException(
                    "Invalid STARTSWITH query, right hand side must be a literal path: "
                            + rvalue);
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
        String field = walkReference(lvalue).field;
        DBObject eq = new BasicDBObject(field, path);
        // escape except alphanumeric and others not needing escaping
        String regex = path.replaceAll("([^a-zA-Z0-9 /])", "\\\\$1");
        Pattern pattern = Pattern.compile(regex + "/.*");
        DBObject like = new BasicDBObject(field, pattern);
        return new BasicDBObject(QueryOperators.OR, Arrays.asList(eq, like));
    }

    protected FieldInfo walkReference(Operand value) {
        if (!(value instanceof Reference)) {
            throw new RuntimeException(
                    "Invalid query, left hand side must be a property: "
                            + value);
        }
        return walkReference((Reference) value);
    }

    protected static class FieldInfo {
        protected String field;

        protected boolean isBoolean;

        /**
         * Boolean system properties only use TRUE or NULL, not FALSE, so
         * queries must be updated accordingly.
         */
        protected boolean isTrueOrNullBoolean;

        protected FieldInfo(String field, boolean isBoolean,
                boolean isTrueOrNullBoolean) {
            this.field = field;
            this.isBoolean = isBoolean;
            this.isTrueOrNullBoolean = isTrueOrNullBoolean;
        }
    }

    /**
     * Returns the MongoDB field for this reference.
     */
    public FieldInfo walkReference(Reference ref) {
        String name = ref.name;
        String[] split = StringUtils.split(name, '/');
        if (name.startsWith(NXQL.ECM_PREFIX)) {
            String prop = DBSSession.convToInternal(name);
            boolean isBoolean = DBSSession.isBoolean(prop);
            return new FieldInfo(prop, isBoolean, true);
        } else {
            String prop = split[0];
            Field field = schemaManager.getField(prop);
            if (field == null) {
                if (prop.indexOf(':') > -1) {
                    throw new RuntimeException("Unkown property: " + name);
                }
                // check without prefix
                // TODO precompute this in SchemaManagerImpl
                for (Schema schema : schemaManager.getSchemas()) {
                    if (!StringUtils.isBlank(schema.getNamespace().prefix)) {
                        // schema with prefix, do not consider as candidate
                        continue;
                    }
                    if (schema != null) {
                        field = schema.getField(prop);
                        if (field != null) {
                            break;
                        }
                    }
                }
                if (field == null) {
                    throw new RuntimeException("Unkown property: " + name);
                }
            }
            // canonical name
            split[0] = field.getName().getPrefixedName();
            // MongoDB embedded field syntax uses . separator
            name = StringUtils.join(split, '.');
            // isArray = field.getType() instanceof ListType
            // && ((ListType) field.getType()).isArray();
            boolean isBoolean = field.getType() instanceof BooleanType;
            return new FieldInfo(name, isBoolean, false);
        }
    }

}
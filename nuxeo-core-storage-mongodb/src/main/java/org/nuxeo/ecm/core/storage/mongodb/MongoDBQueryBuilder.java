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

import java.util.ArrayList;
import java.util.Arrays;
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

    protected SchemaManager schemaManager;

    public MongoDBQueryBuilder() {
        schemaManager = Framework.getLocalService(SchemaManager.class);
    }

    public DBObject walkExpression(Expression expr) {
        Operator op = expr.operator;
        Operand lvalue = expr.lvalue;
        Operand rvalue = expr.rvalue;
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
        } else if (op == Operator.STARTSWITH) {
            throw new UnsupportedOperationException("STARTSWITH");
        } else {
            throw new RuntimeException("Unknown operator: " + op);
        }
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
        String field = walkReference(value);
        return new BasicDBObject(field, null);
    }

    public DBObject walkIsNotNull(Operand value) {
        String field = walkReference(value);
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

    public DBObject walkEq(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        // TODO check list fields
        return new BasicDBObject(field, right);
    }

    public DBObject walkNotEq(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        // TODO check list fields
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.NE,
                right));
    }

    public DBObject walkLt(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.LT,
                right));
    }

    public DBObject walkGt(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.GT,
                right));
    }

    public DBObject walkLtEq(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.LTE,
                right));
    }

    public DBObject walkGtEq(Operand lvalue, Operand rvalue) {
        String field = walkReference(lvalue);
        Object right = walkOperand(rvalue);
        return new BasicDBObject(field, new BasicDBObject(QueryOperators.GTE,
                right));
    }

    public DBObject walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        String field = walkReference(lvalue);
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
        String field = walkReference(lvalue);
        if (!(rvalue instanceof StringLiteral)) {
            throw new RuntimeException(
                    "Invalid LIKE/ILIKE, right hand side must be a string: "
                            + rvalue);
        }
        // TODO check list fields
        String like = walkStringLiteral((StringLiteral) rvalue);
        // escape with slash except alphabetic and percent
        String regex = like.replaceAll("([^a-zA-Z%])", "\\\\$1");
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

    protected String walkReference(Operand value) {
        if (!(value instanceof Reference)) {
            throw new RuntimeException(
                    "Invalid query, left hand side must be a property: "
                            + value);
        }
        return walkReference((Reference) value);
    }

    /**
     * Returns the MongoDB field for this reference.
     */
    public String walkReference(Reference ref) {
        String name = ref.name;
        String[] split = StringUtils.split(name, '/');
        if (name.startsWith(NXQL.ECM_PREFIX)) {
            String prop = DBSSession.convToInternal(name);
            // isArray = DBSSession.isArray(prop);
            return prop;
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
            return name;
        }
    }

}
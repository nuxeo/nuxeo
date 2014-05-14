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
package org.nuxeo.ecm.core.storage;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

/**
 * Evaluator for an {@link Expression}.
 *
 * @since 5.9.4
 */
public abstract class ExpressionEvaluator {

    protected Boolean bool(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean)) {
            throw new RuntimeException("Not a boolean: " + value);
        }
        return (Boolean) value;
    }

    // ternary logic
    protected Boolean not(Boolean value) {
        if (value == null) {
            return null;
        }
        return !value;
    }

    // ternary logic
    protected Boolean and(Boolean left, Boolean right) {
        if (TRUE.equals(left)) {
            return right;
        } else {
            return left;
        }
    }

    // ternary logic
    protected Boolean or(Boolean left, Boolean right) {
        if (TRUE.equals(left)) {
            return left;
        } else {
            return right;
        }
    }

    // ternary logic
    protected Boolean eq(Object left, Object right) {
        if (left == null || right == null) {
            return null;
        }
        return left.equals(right);
    }

    // ternary logic
    protected Boolean in(Object left, List<Object> right) {
        if (left == null) {
            return null;
        }
        boolean hasNull = false;
        for (Object r : right) {
            if (r == null) {
                hasNull = true;
            } else if (left.equals(r)) {
                return TRUE;
            }
        }
        return hasNull ? null : FALSE;
    }

    // ternary logic
    protected Integer cmp(Object left, Object right) {
        if (left == null || right == null) {
            return null;
        }
        if (!(left instanceof Comparable)) {
            throw new RuntimeException("Not a comparable: " + left);
        }
        return ((Comparable<Object>) left).compareTo(right);
    }

    // ternary logic
    protected Boolean like(Object left, String right, boolean caseInsensitive) {
        if (left == null || right == null) {
            return null;
        }
        if (!(left instanceof String)) {
            throw new RuntimeException("Invalid LIKE lhs: " + left);
        }
        String value = (String) left;
        if (caseInsensitive) {
            value = value.toLowerCase();
            right = right.toLowerCase();
        }
        // escape with slash except alphabetic and percent
        String regex = right.replaceAll("([^a-zA-Z%])", "\\\\$1");
        // replace percent with regexp
        regex = regex.replaceAll("%", ".*");
        boolean match = Pattern.compile(regex).matcher(value).matches();
        return match;
    }

    // if list, use EXIST (SELECT 1 FROM left WHERE left.item = right)
    protected Boolean eqMaybeList(Object left, Object right) {
        if (left instanceof Object[]) {
            for (Object l : ((Object[]) left)) {
                Boolean eq = eq(l, right);
                if (TRUE.equals(eq)) {
                    return TRUE;
                }
            }
            return FALSE;
        } else {
            return eq(left, right);
        }
    }

    // if list, use EXIST (SELECT 1 FROM left WHERE left.item IN right)
    protected Boolean inMaybeList(Object left, List<Object> right) {
        if (left instanceof Object[]) {
            for (Object l : ((Object[]) left)) {
                Boolean in = in(l, right);
                if (TRUE.equals(in)) {
                    return TRUE;
                }
            }
            return FALSE;
        } else {
            return in(left, right);
        }
    }

    protected Boolean likeMaybeList(Object left, String right,
            boolean positive, boolean caseInsensitive) {
        if (left instanceof Object[]) {
            for (Object l : ((Object[]) left)) {
                Boolean like = like(l, right, caseInsensitive);
                if (TRUE.equals(like)) {
                    return Boolean.valueOf(positive);
                }
            }
            return Boolean.valueOf(!positive);
        } else {
            Boolean like = like(left, right, caseInsensitive);
            return positive ? like : not(like);
        }
    }

    public Object walkExpression(Expression expr) {
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

    public Boolean walkNot(Operand value) {
        return not(bool(walkOperand(value)));
    }

    public Boolean walkIsNull(Operand value) {
        return Boolean.valueOf(walkOperand(value) == null);
    }

    public Boolean walkIsNotNull(Operand value) {
        return Boolean.valueOf(walkOperand(value) != null);
    }

    public Boolean walkMultiExpression(MultiExpression expr) {
        Boolean res = TRUE;
        for (Operand value : expr.values) {
            Boolean bool = bool(walkOperand(value));
            if (bool == null) {
                // null is absorbent
                return null;
            }
            res = and(res, bool);
        }
        return res;
    }

    public Boolean walkAnd(Operand lvalue, Operand rvalue) {
        Boolean left = bool(walkOperand(lvalue));
        Boolean right = bool(walkOperand(rvalue));
        return and(left, right);
    }

    public Boolean walkOr(Operand lvalue, Operand rvalue) {
        Boolean left = bool(walkOperand(lvalue));
        Boolean right = bool(walkOperand(rvalue));
        return or(left, right);
    }

    public Boolean walkEq(Operand lvalue, Operand rvalue) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        return eqMaybeList(left, right);
    }

    public Boolean walkNotEq(Operand lvalue, Operand rvalue) {
        return not(walkEq(lvalue, rvalue));
    }

    protected Integer cmp(Operand lvalue, Operand rvalue) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        return cmp(left, right);
    }

    public Boolean walkLt(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp < 0;
    }

    public Boolean walkGt(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp > 0;
    }

    public Boolean walkLtEq(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp <= 0;
    }

    public Boolean walkGtEq(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp >= 0;
    }

    public Boolean walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        if (!(right instanceof List)) {
            throw new RuntimeException("Invalid IN rhs: " + rvalue);
        }
        Boolean in = inMaybeList(left, (List<Object>) right);
        return positive ? in : not(in);
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

    public Object walkDateLiteral(DateLiteral lit) {
        return lit.toCalendar(); // TODO onlyDate
    }

    public Object walkDoubleLiteral(DoubleLiteral lit) {
        return Double.valueOf(lit.value);
    }

    public Object walkIntegerLiteral(IntegerLiteral lit) {
        return Long.valueOf(lit.value);
    }

    public Object walkStringLiteral(StringLiteral lit) {
        return lit.value;
    }

    public Object walkLiteralList(LiteralList litList) {
        ArrayList<Object> list = new ArrayList<Object>(litList.size());
        for (Literal lit : litList) {
            list.add(walkLiteral(lit));
        }
        return list;
    }

    public Boolean walkLike(Operand lvalue, Operand rvalue, boolean positive,
            boolean caseInsensitive) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        if (!(right instanceof String)) {
            throw new RuntimeException("Invalid LIKE rhs: " + rvalue);
        }
        return likeMaybeList(left, (String) right, positive, caseInsensitive);
    }

    public Object walkFunction(Function func) {
        throw new UnsupportedOperationException("Function");
    }

    /**
     * Evaluates a reference over the context state.
     *
     * @param ref the reference
     */
    public abstract Object walkReference(Reference ref);

    /**
     * Evaluates a reference over the given state.
     *
     * @param ref the reference
     * @param map the state representation
     */
    public abstract Object evaluateReference(Reference ref,
            Map<String, Serializable> map);

}
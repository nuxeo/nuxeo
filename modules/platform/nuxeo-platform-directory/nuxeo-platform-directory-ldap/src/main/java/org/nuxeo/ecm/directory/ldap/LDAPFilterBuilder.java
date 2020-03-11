/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.directory.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.query.QueryParseException;
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
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;

/**
 * Creates an LDAP query filter from a Nuxeo Expression.
 *
 * @since 10.3
 */
public class LDAPFilterBuilder {

    protected static final String DATE_CAST = "DATE";

    protected final LDAPDirectory directory;

    public StringBuilder filter = new StringBuilder();

    public int paramIndex = 0;

    public final List<Serializable> params = new ArrayList<>();

    public LDAPFilterBuilder(LDAPDirectory directory) {
        this.directory = directory;
    }

    public void walk(Expression expression) {
        if (expression instanceof MultiExpression && ((MultiExpression) expression).predicates.isEmpty()) {
            // special-case empty query
            return;
        } else {
            walkExpression(expression);
        }
    }

    public void walkExpression(Expression expr) {
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
            throw new QueryParseException("SUM");
        } else if (op == Operator.SUB) {
            throw new QueryParseException("SUB");
        } else if (op == Operator.MUL) {
            throw new QueryParseException("MUL");
        } else if (op == Operator.DIV) {
            throw new QueryParseException("DIV");
        } else if (op == Operator.LT) {
            walkLt(lvalue, rvalue);
        } else if (op == Operator.GT) {
            walkGt(lvalue, rvalue);
        } else if (op == Operator.EQ) {
            walkEq(lvalue, rvalue);
        } else if (op == Operator.NOTEQ) {
            walkNotEq(lvalue, rvalue);
        } else if (op == Operator.LTEQ) {
            walkLtEq(lvalue, rvalue);
        } else if (op == Operator.GTEQ) {
            walkGtEq(lvalue, rvalue);
        } else if (op == Operator.AND) {
            if (expr instanceof MultiExpression) {
                walkAndMultiExpression((MultiExpression) expr);
            } else {
                walkAnd(expr);
            }
        } else if (op == Operator.NOT) {
            walkNot(lvalue);
        } else if (op == Operator.OR) {
            if (expr instanceof MultiExpression) {
                walkOrMultiExpression((MultiExpression) expr);
            } else {
                walkOr(expr);
            }
        } else if (op == Operator.LIKE) {
            walkLike(lvalue, rvalue, true, false);
        } else if (op == Operator.ILIKE) {
            walkLike(lvalue, rvalue, true, true);
        } else if (op == Operator.NOTLIKE) {
            walkLike(lvalue, rvalue, false, false);
        } else if (op == Operator.NOTILIKE) {
            walkLike(lvalue, rvalue, false, true);
        } else if (op == Operator.IN) {
            walkIn(lvalue, rvalue, true);
        } else if (op == Operator.NOTIN) {
            walkIn(lvalue, rvalue, false);
        } else if (op == Operator.ISNULL) {
            walkIsNull(lvalue);
        } else if (op == Operator.ISNOTNULL) {
            walkIsNotNull(lvalue);
        } else if (op == Operator.BETWEEN) {
            walkBetween(lvalue, rvalue, true);
        } else if (op == Operator.NOTBETWEEN) {
            walkBetween(lvalue, rvalue, false);
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

    public void walkNot(Operand value) {
        filter.append("(!");
        walkOperand(value);
        filter.append(')');
    }

    public void walkIsNull(Operand value) {
        filter.append("(!");
        walkIsNotNull(value);
        filter.append(')');
    }

    public void walkIsNotNull(Operand value) {
        filter.append('(');
        walkReference(value);
        filter.append("=*)");
    }

    public void walkAndMultiExpression(MultiExpression expr) {
        walkMulti("&", expr.predicates);
    }

    public void walkAnd(Expression expr) {
        walkMulti("&", Arrays.asList(expr.lvalue, expr.rvalue));
    }

    public void walkOrMultiExpression(MultiExpression expr) {
        walkMulti("|", expr.predicates);
    }

    public void walkOr(Expression expr) {
        walkMulti("|", Arrays.asList(expr.lvalue, expr.rvalue));
    }

    protected void walkMulti(String op, List<? extends Operand> values) {
        if (values.size() == 1) {
            walkOperand(values.get(0));
        } else {
            filter.append('(');
            filter.append(op);
            for (Operand value : values) {
                walkOperand(value);
            }
            filter.append(')');
        }
    }

    public void walkEq(Operand lvalue, Operand rvalue) {
        walkBinOp("=", lvalue, rvalue);
    }

    public void walkNotEq(Operand lvalue, Operand rvalue) {
        filter.append("(!");
        walkEq(lvalue, rvalue);
        filter.append(')');
    }

    public void walkLt(Operand lvalue, Operand rvalue) {
        walkBinOp("<", lvalue, rvalue);
    }

    public void walkGt(Operand lvalue, Operand rvalue) {
        walkBinOp(">", lvalue, rvalue);
    }

    public void walkLtEq(Operand lvalue, Operand rvalue) {
        walkBinOp("<=", lvalue, rvalue);
    }

    public void walkGtEq(Operand lvalue, Operand rvalue) {
        walkBinOp(">=", lvalue, rvalue);
    }

    protected void walkBinOp(String op, Operand lvalue, Operand rvalue) {
        filter.append('(');
        Field field = walkReference(lvalue);
        filter.append(op);
        if (field.getType() instanceof BooleanType) {
            rvalue = makeBoolean(rvalue);
        }
        walkLiteral(rvalue);
        filter.append(')');
    }

    protected Operand makeBoolean(Operand rvalue) {
        if (rvalue instanceof BooleanLiteral) {
            return rvalue;
        }
        long v;
        if (!(rvalue instanceof IntegerLiteral) || ((v = ((IntegerLiteral) rvalue).value) != 0 && v != 1)) {
            throw new QueryParseException("Boolean expressions require boolean or literal 0 or 1 as right argument");
        }
        return new BooleanLiteral(v == 1);
    }

    public void walkBetween(Operand lvalue, Operand rvalue, boolean positive) {
        LiteralList list = (LiteralList) rvalue;
        Literal left = list.get(0);
        Literal right = list.get(1);
        if (!positive) {
            filter.append("(!");
        }
        filter.append("(&");
        walkGtEq(lvalue, left);
        walkLtEq(lvalue, right);
        filter.append(')');
        if (!positive) {
            filter.append(')');
        }
    }

    public void walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        if (!positive) {
            filter.append("(!");
        }
        filter.append("(|");
        for (Literal value : (LiteralList) rvalue) {
            walkEq(lvalue, value);
        }
        filter.append(')');
        if (!positive) {
            filter.append(')');
        }
    }

    public void walkLike(Operand lvalue, Operand rvalue, boolean positive, boolean caseInsensitive) {
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException("Invalid LIKE, right hand side must be a string: " + rvalue);
        }
        String like = ((StringLiteral) rvalue).value;
        if (caseInsensitive) {
            like = like.toLowerCase();
        }

        if (!positive) {
            filter.append("(!");
        }
        filter.append('(');
        walkReference(lvalue);
        filter.append('=');
        walkLikeWildcard(like);
        filter.append(')');
        if (!positive) {
            filter.append(')');
        }
    }

    /**
     * Turns a NXQL LIKE pattern into an LDAP wildcard.
     * <p>
     * % and _ are standard wildcards, and \ escapes them.
     */
    public void walkLikeWildcard(String like) {
        StringBuilder param = new StringBuilder();
        char[] chars = like.toCharArray();
        boolean escape = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            boolean escapeNext = false;
            if (escape) {
                param.append(c);
            } else {
                switch (c) {
                case '%':
                    if (param.length() != 0) {
                        addFilterParam(param.toString());
                        param.setLength(0);
                    }
                    filter.append('*');
                    break;
                case '_': // interpret it as an escaped _, not a wildcard
                    param.append(c);
                    break;
                case '\\':
                    escapeNext = true;
                    break;
                default:
                    param.append(c);
                    break;
                }
            }
            escape = escapeNext;
        }
        if (escape) {
            throw new QueryParseException("Invalid LIKE parameter ending with escape character");
        }
        if (param.length() != 0) {
            addFilterParam(param.toString());
        }
    }

    public void walkOperand(Operand operand) {
        if (operand instanceof Literal) {
            walkLiteral(operand);
        } else if (operand instanceof Function) {
            walkFunction((Function) operand);
        } else if (operand instanceof Expression) {
            walkExpression((Expression) operand);
        } else if (operand instanceof Reference) {
            walkReference(operand);
        } else {
            throw new QueryParseException("Unknown operand: " + operand);
        }
    }

    public void walkLiteral(Operand operand) {
        if (!(operand instanceof Literal)) {
            throw new QueryParseException("Requires literal instead of: " + operand);
        }
        Literal lit = (Literal) operand;
        if (lit instanceof BooleanLiteral) {
            walkBooleanLiteral((BooleanLiteral) lit);
        } else if (lit instanceof DateLiteral) {
            walkDateLiteral((DateLiteral) lit);
        } else if (lit instanceof DoubleLiteral) {
            walkDoubleLiteral((DoubleLiteral) lit);
        } else if (lit instanceof IntegerLiteral) {
            walkIntegerLiteral((IntegerLiteral) lit);
        } else if (lit instanceof StringLiteral) {
            walkStringLiteral((StringLiteral) lit);
        } else {
            throw new QueryParseException("Unknown literal: " + lit);
        }
    }

    public void walkBooleanLiteral(BooleanLiteral lit) {
        addFilterParam(Boolean.valueOf(lit.value));
    }

    public void walkDateLiteral(DateLiteral lit) {
        if (lit.onlyDate) {
            throw new QueryParseException("Cannot use only date in LDAP query: " + lit);
        }
        addFilterParam(lit.toCalendar()); // let LDAP library serialize it
    }

    public void walkDoubleLiteral(DoubleLiteral lit) {
        addFilterParam(Double.valueOf(lit.value));
    }

    public void walkIntegerLiteral(IntegerLiteral lit) {
        addFilterParam(Long.valueOf(lit.value));
    }

    public void walkStringLiteral(StringLiteral lit) {
        addFilterParam(lit.value);
    }

    protected void addFilterParam(Serializable value) {
        filter.append('{');
        filter.append(paramIndex++);
        filter.append('}');
        params.add(value);
    }

    public Object walkFunction(Function func) {
        throw new QueryParseException(func.name);
    }

    public Field walkReference(Operand value) {
        if (!(value instanceof Reference)) {
            throw new QueryParseException("Invalid query, left hand side must be a property: " + value);
        }
        String name = ((Reference) value).name;
        if (directory.isReference(name)) {
            throw new QueryParseException(
                    "Column: " + name + " is a reference and cannot be queried for directory: " + directory.getName());
        }
        Field field = directory.getSchemaFieldMap().get(name);
        if (field == null) {
            throw new QueryParseException("No column: " + name + " for directory: " + directory.getName());
        }
        String backend = directory.getFieldMapper().getBackendField(name);
        filter.append(backend);
        return field;
    }

}

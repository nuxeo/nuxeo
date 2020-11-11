/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql.model;

import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * An infix expression.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Expression implements Operand {

    private static final long serialVersionUID = 6007989243273673300L;

    public final Operator operator;

    public final Operand lvalue;

    public final Operand rvalue;

    /** Arbitrary info associated to the expression. */
    public Object info;

    public Expression(Operand lvalue, Operator operator, Operand rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.operator = operator;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitExpression(this);
    }

    /**
     * Is the unary operator pretty-printed after the operand?
     */
    public boolean isSuffix() {
        return operator == Operator.ISNULL || operator == Operator.ISNOTNULL;
    }

    public void setInfo(Object info) {
        this.info = info;
    }

    public Object getInfo() {
        return info;
    }

    @Override
    public String toString() {
        if (rvalue == null) {
            if (isSuffix()) {
                return lvalue.toString() + ' ' + operator.toString();
            } else {
                return operator.toString() + ' ' + lvalue.toString();
            }
        } else {
            return '(' + lvalue.toString() + ' ' + operator.toString() + ' ' + rvalue.toString() + ')';
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Expression) {
            Expression e = (Expression) obj;
            if (operator.id != e.operator.id) {
                return false;
            }
            if (!lvalue.equals(e.lvalue)) {
                return false;
            }
            if (rvalue != null) {
                if (!rvalue.equals(e.rvalue)) {
                    return false;
                }
            } else if (e.rvalue != null) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + operator.hashCode();
        result = 37 * result + (lvalue == null ? 0 : lvalue.hashCode());
        result = 37 * result + (rvalue == null ? 0 : rvalue.hashCode());
        return result;
    }

    public boolean isPathExpression() {
        return (lvalue instanceof Reference) && NXQL.ECM_PATH.equals(((Reference) lvalue).name);
    }

}

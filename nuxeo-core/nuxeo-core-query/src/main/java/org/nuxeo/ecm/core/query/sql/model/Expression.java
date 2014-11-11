/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Expression implements Operand {

    private static final long serialVersionUID = 6007989243273673300L;

    public final Operator operator;
    public final Operand lvalue;
    public final Operand rvalue;


    public Expression(Operand lvalue, Operator operator, Operand rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.operator = operator;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitExpression(this);
    }

    @Override
    public String toString() {
        if (rvalue == null) {
            return operator.toString() + ' ' + lvalue.toString();
        } else {
            return lvalue.toString() + ' ' + operator.toString() + ' ' + rvalue.toString();
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
        result = 37 * result + lvalue.hashCode();
        result = 37 * result + (rvalue == null ? 0 : rvalue.hashCode());
        return result;
    }


    public boolean isPathExpression() {
        return (lvalue instanceof Reference) && NXQL.ECM_PATH.equals(((Reference)lvalue).name);
    }

}

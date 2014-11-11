/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.sql.model;

import java.util.Iterator;
import java.util.List;

/**
 * An expression for an single operator with an arbitrary number of operands.
 * <p>
 * It extends {@link Predicate} but it's really not a real Predicate (some users
 * of Predicate expect it to have lvalue and rvalue fields, which are null in
 * this class).
 *
 * @author Florent Guillaume
 */
public class MultiExpression extends Predicate {

    private static final long serialVersionUID = 1L;

    public final List<Operand> values;

    public MultiExpression(Operator operator, List<Operand> values) {
        super(null, operator, null);
        this.values = values;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitMultiExpression(this);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(operator);
        buf.append('(');
        for (Iterator<Operand> it = values.iterator(); it.hasNext();) {
            Operand operand = it.next();
            buf.append(operand.toString());
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append(')');
        return buf.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof MultiExpression) {
            return equals((MultiExpression) other);
        }
        return false;
    }

    protected boolean equals(MultiExpression other) {
        return values.equals(other.values) && super.equals(other);
    }

    @Override
    public int hashCode() {
        return values.hashCode() + super.hashCode();
    }

}

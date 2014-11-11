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

/**
 * @author Florent Guillaume
 */
public class OrderByExpr implements Operand {

    private static final long serialVersionUID = 1L;

    public final Reference reference;

    public final boolean isDescending;

    public OrderByExpr(Reference reference, boolean isDescending) {
        this.reference = reference;
        this.isDescending = isDescending;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isDescending ? 1231 : 1237);
        result = prime * result +
                (reference == null ? 0 : reference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof OrderByExpr) {
            return equals((OrderByExpr) other);
        }
        return false;
    }

    private boolean equals(OrderByExpr other) {
        if (isDescending != other.isDescending) {
            return false;
        }
        if (reference == null) {
            if (other.reference != null) {
                return false;
            }
        } else if (!reference.equals(other.reference)) {
            return false;
        }
        return true;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitOrderByExpr(this);
    }

    @Override
    public String toString() {
        if (isDescending) {
            return reference.toString() + " DESC";
        } else {
            return reference.toString();
        }
    }

}

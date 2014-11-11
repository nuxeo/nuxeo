/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
                ((reference == null) ? 0 : reference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof OrderByExpr) {
            return equals((OrderByExpr) other);
        }
        return false;
    }

    private boolean equals(OrderByExpr other) {
        if (isDescending != other.isDescending)
            return false;
        if (reference == null) {
            if (other.reference != null) {
                return false;
            }
        } else if (!reference.equals(other.reference)) {
            return false;
        }
        return true;
    }

    public void accept(IVisitor visitor) {
        visitor.visitOrderByExpr(this);
    }

    @Override
    public String toString() {
        if (isDescending) {
            return reference.toString();
        } else {
            return reference.toString() + " DESC";
        }
    }

}

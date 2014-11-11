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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.sql.model;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class OrderByClause extends Clause {

    private static final long serialVersionUID = 1L;

    public final OrderByList elements;

    public OrderByClause(OrderByList orderBy) {
        this(orderBy, false);
    }

    public OrderByClause(OrderByList orderBy, boolean isDescendent) {
        super("ORDER BY");
        elements = orderBy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OrderByClause) {
            return elements.equals(((OrderByClause) obj).elements);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    public void accept(IVisitor visitor) {
        visitor.visitOrderByClause(this);
    }

    @Override
    public String toString() {
        return elements.toString();
    }

}

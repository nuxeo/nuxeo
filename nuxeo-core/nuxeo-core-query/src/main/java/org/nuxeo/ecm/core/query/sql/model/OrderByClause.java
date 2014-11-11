/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitOrderByClause(this);
    }

    @Override
    public String toString() {
        return elements.toString();
    }

}

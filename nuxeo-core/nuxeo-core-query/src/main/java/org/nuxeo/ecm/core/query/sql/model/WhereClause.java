/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql.model;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WhereClause extends Clause {

    private static final long serialVersionUID = -6192874148042567401L;

    public final Predicate predicate;

    public WhereClause() {
        this(null);
    }

    public WhereClause(Predicate predicate) {
        super("WHERE");
        this.predicate = predicate;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitWhereClause(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof WhereClause) {
            if (predicate != null) {
                return predicate.equals(((WhereClause) obj).predicate);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (predicate == null) {
            return 0;
        }
        return predicate.hashCode();
    }

    @Override
    public String toString() {
        return predicate.toString();
    }

}

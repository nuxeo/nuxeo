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

    public void accept(IVisitor visitor) {
        // TODO Auto-generated method stub
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

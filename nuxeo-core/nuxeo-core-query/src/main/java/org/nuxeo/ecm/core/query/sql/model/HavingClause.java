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
public class HavingClause extends Clause {

    private static final long serialVersionUID = 3686852512501042627L;

    public final Predicate predicate;

    public HavingClause() {
        this(null);
    }

    public HavingClause(Predicate predicate) {
        super("HAVING");
        this.predicate = predicate;
    }

    public void accept(IVisitor visitor) {
        visitor.visitHavingClause(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HavingClause) {
            if (predicate != null) {
                return predicate.equals(((HavingClause) obj).predicate);
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

}

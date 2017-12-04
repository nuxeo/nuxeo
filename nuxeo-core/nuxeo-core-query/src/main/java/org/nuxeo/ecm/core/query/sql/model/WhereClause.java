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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WhereClause extends Clause {

    private static final long serialVersionUID = -6192874148042567401L;

    public final Expression predicate;

    public WhereClause() {
        this(null);
    }

    public WhereClause(Expression predicate) {
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

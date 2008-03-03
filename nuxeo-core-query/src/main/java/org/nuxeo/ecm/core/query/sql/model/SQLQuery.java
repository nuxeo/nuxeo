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

/** * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SQLQuery implements ASTNode {

    private static final long serialVersionUID = 6383829486216039408L;

    public final SelectClause select;
    public final FromClause from;
    public final WhereClause where;
    public final OrderByClause orderBy;
    public final GroupByClause groupBy;
    public final HavingClause having;

    public SQLQuery() {
        this(new SelectClause(), new FromClause());
    }

    public SQLQuery(SelectClause select, FromClause from, WhereClause where, OrderByClause orderBy) {
        this(select, from, where, null, null, orderBy);
    }

    public SQLQuery(SelectClause select, FromClause from, WhereClause where) {
        this(select, from, where, null, null, null);
    }

    public SQLQuery(SelectClause select, FromClause from) {
        this(select, from, null, null, null, null);
    }

    public SQLQuery(SelectClause select, FromClause from, WhereClause where,
            GroupByClause groupBy, HavingClause having, OrderByClause orderBy) {
        assert select != null && from != null;
        this.select = select;
        this.from = from;
        this.where = where;
        this.groupBy = groupBy;
        this.having = having;
        this.orderBy = orderBy;
    }


    public SelectClause getSelectClause() {
        return select;
    }

    public FromClause getFromClause() {
        return from;
    }

    public WhereClause getWhereClause() {
        return where;
    }

    public OrderByClause getOrderByClause() {
        return orderBy;
    }

    public void accept(IVisitor visitor) {
        visitor.visitQuery(this);
    }


    @Override
    // FIXME: not finished
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT ").append(select).append(" FROM ").append(from);
        if (where != null) {
            buf.append(" WHERE ").append(where);
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SQLQuery) {
            SQLQuery q = (SQLQuery) obj;
            if (!select.equals(q.select) || !from.equals(q.from)) {
                return false;
            }
            if (where == null) {
                if (q.where != null) {
                    return false;
                }
            } else if (!where.equals(q.where)) {
                return false;
            }
            if (orderBy == null) {
                if (q.orderBy != null) {
                    return false;
                }
            } else if (!orderBy.equals(q.orderBy)) {
                return false;
            }
            if (groupBy == null) {
                if (q.groupBy != null) {
                    return false;
                }
            } else if (!groupBy.equals(q.groupBy)) {
                return false;
            }
            if (having == null) {
                if (q.having != null) {
                    return false;
                }
            } else if (!having.equals(q.having)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + select.hashCode();
        result = 37 * result + from.hashCode();
        result = 37 * result + (where == null ? 0 : where.hashCode());
        result = 37 * result + (orderBy == null ? 0 : orderBy.hashCode());
        result = 37 * result + (groupBy == null ? 0 : groupBy.hashCode());
        result = 37 * result + (having == null ? 0 : having.hashCode());
        return result;
    }

}

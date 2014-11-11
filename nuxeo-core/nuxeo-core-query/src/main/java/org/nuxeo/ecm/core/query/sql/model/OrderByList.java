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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;

/**
 * @author Florent Guillaume
 */
public class OrderByList extends ArrayList<OrderByExpr> implements Operand {

    private static final long serialVersionUID = 1L;

    public OrderByList(OrderByExpr expr) {
        add(expr);
    }

    public void accept(IVisitor visitor) {
        visitor.visitOrderByList(this);
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<String>(size());
        for (OrderByExpr expr : this) {
            list.add(expr.toString());
        }
        return StringUtils.join(list, ", ");
    }

}

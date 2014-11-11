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

    @Override
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

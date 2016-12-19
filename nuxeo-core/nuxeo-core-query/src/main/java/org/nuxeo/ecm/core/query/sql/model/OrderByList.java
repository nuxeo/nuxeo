/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.query.sql.model;

import java.util.ArrayList;
import java.util.stream.Collectors;

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
        return stream().map(OrderByExpr::toString).collect(Collectors.joining(", "));
    }

}

/*
 * Copyright (c) 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.query.sql.model;

import java.util.ArrayList;

public class EsIdentifierList extends ArrayList<String> implements Operand {

    private static final long serialVersionUID = 4590324482296853715L;

    public EsIdentifierList() {
        super();
    }

    public EsIdentifierList(String identifiers) {
        for (String index: identifiers.split(",")) {
            this.add(index);
        }
    }

    @Override
    public void accept(IVisitor visitor) {
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (isEmpty()) {
            return "";
        }
        buf.append(get(0).toString());
        for (int i = 1, size = size(); i < size; i++) {
            buf.append(",").append(get(i).toString());
        }
        return buf.toString();
    }

}

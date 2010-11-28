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

import java.util.ArrayList;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LiteralList extends ArrayList<Literal> implements Operand {

    private static final long serialVersionUID = 4590326082296853715L;


    @Override
    public void accept(IVisitor visitor) {
        visitor.visitLiteralList(this);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (isEmpty()) {
            return "";
        }
        buf.append(get(0).toString());
        for (int i = 1, size = size(); i < size; i++) {
            buf.append(", ").append(get(i).toString());
        }
        return buf.toString();
    }

}

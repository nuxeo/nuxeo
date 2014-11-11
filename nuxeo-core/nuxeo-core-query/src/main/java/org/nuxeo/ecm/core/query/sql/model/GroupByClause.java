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

import java.util.List;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GroupByClause extends Clause {

    private static final long serialVersionUID = 3007583185472406626L;

    public final String[] elements;

    public GroupByClause() {
        super("GROUP BY");
        elements = new String[0];
    }

    public GroupByClause(String[] groupBy) {
        super("GROUP BY");
        elements = groupBy;
    }

    public GroupByClause(String groupBy) {
        super("GROUP BY");
        elements = new String[] { groupBy };
    }

    public GroupByClause(List<String> groupBy) {
        super("GROUP BY");
        elements = groupBy.toArray(new String[groupBy.size()]);
    }

    public void accept(IVisitor visitor) {
        visitor.visitGroupByClause(this);
    }

}

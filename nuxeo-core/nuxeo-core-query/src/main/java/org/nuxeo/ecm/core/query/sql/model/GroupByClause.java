/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitGroupByClause(this);
    }

}

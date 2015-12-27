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

import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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

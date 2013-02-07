/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.tag;

import java.util.List;

import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Select;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;

/**
 * Query Maker specialized for tagging queries that need joins.
 */
public class TagQueryMaker extends NXQLQueryMaker {

    /** The NXTAG query type. */
    public static final String NXTAG = "NXTAG";

    public static final String SCHEMA_TAG = "tag";

    public static final String SCHEMA_RELATION = "relation";

    public static final String PROPERTY_SOURCE = "source";

    public static final String PROPERTY_TARGET = "target";

    /**
     * Makes sure the Tag table is joined with the relation target instead of
     * the hierarchy id.
     */
    public static final String TAG_IS_TARGET = "TAGISTARGET: ";

    /**
     * Adds a COUNT() on the relation source, to count documents.
     */
    public static final String COUNT_SOURCE = "COUNTSOURCE: ";

    protected String type;

    protected Table relationTable;

    protected Column firstSelectedColumn;

    @Override
    public String getName() {
        return NXTAG;
    }

    @Override
    public boolean accepts(String queryType) {
        return queryType.equals(NXTAG);
    }

    @Override
    public Query buildQuery(SQLInfo sqlInfo, Model model,
            PathResolver pathResolver, String query, QueryFilter queryFilter,
            Object... params) throws StorageException {
        if (query.startsWith(TAG_IS_TARGET)) {
            type = TAG_IS_TARGET;
        } else if (query.startsWith(COUNT_SOURCE)) {
            type = COUNT_SOURCE;
            // SELECT "TAG"."LABEL" AS "_C1",
            // COUNT(DISTINCT "RELATION"."SOURCE") AS "_C2"
            // FROM "HIERARCHY"
            // JOIN "RELATION" ON "HIERARCHY"."ID" = "RELATION"."ID"
            // JOIN "DUBLINCORE" ON "HIERARCHY"."ID" = "DUBLINCORE"."ID"
            // JOIN "TAG" ON "RELATION"."TARGET" = "TAG"."ID"
            // WHERE "HIERARCHY"."PRIMARYTYPE" IN ('Tagging')
            // AND ("RELATION"."SOURCE" = '47c4c0f7...') -- or IN ()
            // AND ("DUBLINCORE"."CREATOR" = 'Administrator')
            // GROUP BY "_C1"
        } else {
            throw new QueryMakerException("Bad query: " + query);
        }
        query = query.substring(type.length());
        return super.buildQuery(sqlInfo, model, pathResolver, query,
                queryFilter, params);
    }

    /**
     * Adds an initial join on the Relation table, and records it.
     */
    @Override
    protected void fixInitialJoins() {
        relationTable = getFragmentTable(dataHierTable, SCHEMA_RELATION,
                SCHEMA_RELATION, -1, false);
    }

    /**
     * Patches the Tag join to join on the relation target instead of the
     * hierarchy id.
     */
    @Override
    protected void addJoin(int kind, String alias, Table table, String column,
            Table contextTable, String contextColumn, String name, int index,
            String primaryType) {
        if (table.getKey().equals(SCHEMA_TAG)) {
            kind = Join.INNER;
            contextTable = relationTable;
            contextColumn = PROPERTY_TARGET;
        }
        super.addJoin(kind, alias, table, column, contextTable, contextColumn,
                name, index, null);
    }

    @Override
    protected String getSelectColName(Column col) {
        String name = super.getSelectColName(col);
        if (firstSelectedColumn == null) {
            firstSelectedColumn = col;
        }
        if (type == COUNT_SOURCE
                && col.getTable().getKey().equals(SCHEMA_RELATION)
                && col.getKey().equals(PROPERTY_SOURCE)) {
            name = String.format("COUNT(DISTINCT %s)", name);
        }
        return name;
    }

    @Override
    protected void fixWhatColumns(List<Column> whatColumns) {
        if (type == COUNT_SOURCE) {
            // 2nd col is a COUNT -> different type
            Column targetCol = whatColumns.remove(1);
            Column countCol = new Column(targetCol.getTable(), null,
                    ColumnType.INTEGER, null);
            whatColumns.add(countCol);
        }
    }

    @Override
    protected void fixSelect(Select select) {
        if (type == COUNT_SOURCE) {
            // add a GROUP BY on first col
            String name;
            if (dialect.needsOriginalColumnInGroupBy()) {
                name = firstSelectedColumn.getFullQuotedName();
            } else {
                name = dialect.openQuote() + COL_ALIAS_PREFIX + "1"
                        + dialect.closeQuote();
            }
            select.setGroupBy(name);
        }
    }

}

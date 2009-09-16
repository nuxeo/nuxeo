/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.chemistry.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.chemistry.Property;
import org.apache.chemistry.cmissql.CmisSqlLexer;
import org.apache.chemistry.cmissql.CmisSqlParser;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoCmisWalker.Join;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.SQLInfo;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.Model.PropertyInfo;
import org.nuxeo.ecm.core.storage.sql.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Select;
import org.nuxeo.ecm.core.storage.sql.db.Table;
import org.nuxeo.ecm.core.storage.sql.db.TableAlias;
import org.nuxeo.ecm.core.storage.sql.db.dialect.Dialect;

/**
 * Transformer of CMISQL queries into underlying SQL queries to the actual
 * database.
 *
 * @author Florent Guillaume
 */
public class CMISQLQueryMaker implements QueryMaker {

    public static final String TYPE = "CMISQL";

    private static final String CMIS_PREFIX = "cmis:";

    public static final String DC_FRAGMENT_NAME = "dublincore";

    public static final String DC_CREATOR_KEY = "creator";

    public static final String DC_CREATED_KEY = "created";

    public static final String DC_MODIFIED_KEY = "modified";

    protected SQLInfo sqlInfo;

    protected Database database;

    protected Dialect dialect;

    protected Model model;

    protected Session session;

    /** true if the proxies table is used (not excluded by toplevel clause). */
    protected boolean considerProxies;

    /**
     * The hierarchy table, which may be an alias table.
     */
    protected Table hierTable;

    /**
     * Quoted id in the hierarchy. This is the id returned by the query.
     */
    protected String hierId;

    /**
     * The hierarchy table of the data.
     */
    protected Table joinedHierTable;

    /**
     * Quoted id attached to the data that matches.
     */
    protected String joinedHierId;

    /** propertyInfos uppercased names mapped to normal name. */
    protected Map<String, String> propertyInfoNames;

    /** propertyInfoNames + specialPropNames */
    protected Map<String, String> allPropNames;


    private static class QueryMakerException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public QueryMakerException(String message) {
            super(message);
        }
    }

    public String getName() {
        return TYPE;
    }

    public boolean accepts(String queryType) {
        return queryType.equals(TYPE);
    }

    public Query buildQuery(SQLInfo sqlInfo, Model model, Session session,
            String query, QueryFilter queryFilter, Object... params)
            throws StorageException {
        this.sqlInfo = sqlInfo;
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        this.session = session;
        // TODO precompute this only once
        propertyInfoNames = new HashMap<String, String>();
        for (String name : model.getPropertyInfoNames()) {
            propertyInfoNames.put(name.toUpperCase(), name);
        }
        allPropNames = new HashMap<String, String>(propertyInfoNames);
        allPropNames.putAll(systemPropNames);

        hierTable = database.getTable(model.hierTableName);
        hierId = hierTable.getColumn(model.MAIN_KEY).getFullQuotedName();

        // NuxeoConnection connection = (NuxeoConnection) params[0];
        NuxeoCmisWalker walker;
        try {
            CharStream input = new ANTLRInputStream(new ByteArrayInputStream(
                    query.getBytes("UTF-8")));
            TokenSource lexer = new CmisSqlLexer(input);
            TokenStream tokens = new CommonTokenStream(lexer);
            CommonTree tree = (CommonTree) new CmisSqlParser(tokens).query().getTree();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
            nodes.setTokenStream(tokens);
            walker = new NuxeoCmisWalker(nodes);
            walker.query(this);
        } catch (IOException e) {
            throw new StorageException(e.getMessage(), e);
        } catch (RecognitionException e) {
            throw new StorageException("Cannot parse query: " + query, e);
        }

        // per qualifier, find all tables (fragments)
        Map<String, Map<String, Table>> tablesPerQual = new HashMap<String, Map<String, Table>>();
        for (Entry<String, Set<String>> entry : walker.columnsPerQual.entrySet()) {
            String qual = entry.getKey();
            Map<String, Table> tables = tablesPerQual.get(qual);
            if (tables == null) {
                tablesPerQual.put(qual, tables = new HashMap<String, Table>());
            }
            for (String fqn : entry.getValue()) {
                Column col = walker.columns.get(fqn);
                tables.put(col.getTable().getName(), col.getTable());
            }
        }
        // per qualifier, include hier first in fragments
        Map<String, List<Table>> qualTables = new HashMap<String, List<Table>>();
        for (Entry<String, Map<String, Table>> entry : tablesPerQual.entrySet()) {
            String qual = entry.getKey();
            Map<String, Table> tableMap = entry.getValue();
            // add hier as well in fragments
            String hierAlias = getAlias(hierTable, qual);
            Table hierAliasTable = findTable(hierTable, hierAlias);
            tableMap.remove(hierAlias);
            List<Table> tables = new LinkedList<Table>(tableMap.values());
            tables.add(hierAliasTable);
            qualTables.put(qual, tables);
        }

        // walk joins
        boolean first = true;
        StringBuilder buf = new StringBuilder();
        for (Join j : walker.from_joins) {
            String qual = j.corr;
            Table table = null; // table this join is about
            if (first) {
                // start with hier
                String hierAlias = getAlias(hierTable, qual);
                table = findTable(hierTable, hierAlias);
            } else {
                // do requested join
                if (j.kind.equals("LEFT") || j.kind.equals("RIGHT")) {
                    buf.append(" ");
                    buf.append(j.kind);
                }
                buf.append(" JOIN ");
                // find which table in on1/on2 refers to current qualifier
                Set<String> tables = walker.columnsPerQual.get(qual);
                for (String fqn : Arrays.asList(j.on1, j.on2)) {
                    if (!tables.contains(fqn)) {
                        continue;
                    }
                    Column col = walker.columns.get(fqn);
                    table = col.getTable();
                }
                if (table == null) {
                    throw new StorageException(
                            "Bad query, qualifier not found: " + qual);
                }
            }

            // join requested table
            String name;
            if (table.isAlias()) {
                name = table.getRealTable().getQuotedName() + " "
                        + table.getQuotedName();
            } else {
                name = table.getQuotedName();
            }
            buf.append(name);

            if (!first) {
                // emit actual join requested
                buf.append(" ON ");
                buf.append(j.on1);
                buf.append(" = ");
                buf.append(j.on2);
            }

            // join other fragments for qualifier
            for (Table t : qualTables.get(qual)) {
                if (t.getName().equals(table.getName())) {
                    // this one was the first, already done
                    continue;
                }
                String n;
                if (t.isAlias()) {
                    n = t.getRealTable().getQuotedName() + " "
                            + t.getQuotedName();
                } else {
                    n = t.getQuotedName();
                }
                buf.append(" LEFT JOIN ");
                buf.append(n);
                buf.append(" ON ");
                buf.append(t.getColumn(model.MAIN_KEY).getFullQuotedName());
                buf.append(" = ");
                buf.append(table.getColumn(model.MAIN_KEY).getFullQuotedName());
            }

            first = false;
        }

        List<String> whereClauses = new LinkedList<String>();
        if (walker.select_where != null) {
            whereClauses.add('(' + walker.select_where + ')');
        }

        Query q = new Query();

        // Security check
        queryFilter = null;
        if (queryFilter != null && queryFilter.getPrincipals() != null) {
            whereClauses.add(dialect.getSecurityCheckSql(hierId));
            Serializable principals;
            Serializable permissions;
            if (dialect.supportsArrays()) {
                principals = queryFilter.getPrincipals();
                permissions = queryFilter.getPermissions();
            } else {
                principals = StringUtils.join(queryFilter.getPrincipals(), '|');
                permissions = StringUtils.join(queryFilter.getPermissions(),
                        '|');
            }
            q.selectParams.add(principals);
            q.selectParams.add(permissions);
        }

        // what we select
        List<Column> whatColumns = new LinkedList<Column>();
        List<String> whatColumnsAliases = new LinkedList<String>();
        for (String col : walker.select_what) {
            whatColumns.add(walker.columns.get(col));
            whatColumnsAliases.add(walker.columnsSpecified.get(col));
        }
        String what = StringUtils.join(walker.select_what, ", ");

        /*
         * Create the whole select.
         */
        Select select = new Select(null);
        select.setWhat(what);
        select.setFrom(buf.toString());
        select.setWhere(StringUtils.join(whereClauses, " AND "));
        select.setOrderBy(StringUtils.join(walker.select_orderby, ", "));

        q.selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns,
                whatColumnsAliases, null, null);
        return q;
    }

    protected Column findColumn(String name, String qualifier) {
        boolean allowArray = false;
        boolean inOrderBy = false;
        Column column;
        name = name.toUpperCase();
        if (name.startsWith(CMIS_PREFIX.toUpperCase())) {
            column = getSpecialColumn(name);
        } else {
            String propertyName = propertyInfoNames.get(name);
            if (propertyName == null) {
                throw new QueryMakerException("Unknown field: " + name);
            }
            PropertyInfo propertyInfo = model.getPropertyInfo(propertyName);
            Table table = database.getTable(propertyInfo.fragmentName);
            if (propertyInfo.propertyType.isArray()) {
                if (!allowArray) {
                    String msg = inOrderBy ? "Cannot use multi-valued property %s in ORDER BY clause"
                            : "Cannot use multi-valued property %s";
                    throw new QueryMakerException(String.format(msg, name));
                }
                column = table.getColumn(model.COLL_TABLE_VALUE_KEY);
            } else {
                column = table.getColumn(propertyInfo.fragmentKey);
            }
        }
        if (qualifier != null) {
            String alias = getAlias(column.getTable(), qualifier);
            column = findTable(column.getTable(), alias).getColumn(
                    column.getKey());
            // TODO ensure key == name, or add getName()
        }
        return column;
    }

    protected Table findTable(Table table, String alias) {
        if (alias == null) {
            return table;
        } else {
            return new TableAlias(table, alias);
        }
    }

    protected String getAlias(Table table, String qualifier) {
        if (qualifier == null) {
            return null;
        } else {
            return "_" + qualifier + "_" + table.getName();
        }
    }

    protected static Map<String, String> systemPropNames = new HashMap<String, String>();
    static {
        for (String prop : Arrays.asList( //
                Property.ID, //
                Property.TYPE_ID, //
                Property.PARENT_ID, //
                Property.NAME, //
                Property.CREATED_BY, //
                Property.CREATION_DATE, //
                Property.LAST_MODIFICATION_DATE //
        )) {
            systemPropNames.put(prop.toUpperCase(), prop);
        }
    }

    protected Column getSpecialColumn(String name) {
        // TODO precompute uppercased versions
        if (name.equals(Property.ID.toUpperCase())) {
            return hierTable.getColumn(model.MAIN_KEY);
        }
        if (name.equals(Property.TYPE_ID.toUpperCase())) {
            // joinedHierTable
            return hierTable.getColumn(model.MAIN_PRIMARY_TYPE_KEY);
        }
        if (name.equals(Property.PARENT_ID.toUpperCase())) {
            return hierTable.getColumn(model.HIER_PARENT_KEY);
        }
        if (name.equals(Property.NAME.toUpperCase())) {
            return hierTable.getColumn(model.HIER_CHILD_NAME_KEY);
        }
        if (name.equals(Property.CREATED_BY.toUpperCase())) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_CREATOR_KEY);
        }
        if (name.equals(Property.CREATION_DATE.toUpperCase())) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_CREATED_KEY);
        }
        if (name.equals(Property.LAST_MODIFICATION_DATE.toUpperCase())) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(
                    DC_MODIFIED_KEY);
        }
        // map.put(Property.LAST_MODIFIED_BY, "dc:contributors");
        throw new QueryMakerException("Unknown field: " + name);
    }

    protected String getCanonicalColumnName(String name, String qual) {
        name = allPropNames.get(name.toUpperCase());
        return qual == null ? name : qual + '.' + name;
    }

}

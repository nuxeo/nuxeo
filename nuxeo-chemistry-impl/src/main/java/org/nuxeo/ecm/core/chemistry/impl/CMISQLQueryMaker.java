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
import java.util.ArrayList;
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
import org.apache.chemistry.BaseType;
import org.apache.chemistry.Property;
import org.apache.chemistry.cmissql.CmisSqlLexer;
import org.apache.chemistry.cmissql.CmisSqlParser;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoCmisWalker.Join;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.TypeConstants;
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

    protected Database database;

    protected Dialect dialect;

    protected Model model;

    protected Table hierTable;

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
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        // TODO precompute this only once
        propertyInfoNames = new HashMap<String, String>();
        for (String name : model.getPropertyInfoNames()) {
            propertyInfoNames.put(name.toUpperCase(), name);
        }
        allPropNames = new HashMap<String, String>(propertyInfoNames);
        allPropNames.putAll(systemPropNames);

        hierTable = database.getTable(model.hierTableName);

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
        if (!walker.errorMessages.isEmpty()) {
            throw new StorageException("Cannot parse query: " + query + " ("
                    + StringUtils.join(walker.errorMessages, ", ") + ")");
        }

        /*
         * Find info about fragments needed.
         */

        // add main id to all qualifiers
        for (String qual : new ArrayList<String>(walker.columnsPerQual.keySet())) {
            for (String propertyId : Arrays.asList(Property.ID,
                    Property.TYPE_ID)) {
                String col = walker.referToColumn(propertyId, qual);
                if (!walker.select_what.contains(col)) {
                    walker.select_what.add(col);
                }
            }
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
        // per qualifier, include hier in fragments
        Map<String, List<Table>> qualTables = new HashMap<String, List<Table>>();
        for (Entry<String, Map<String, Table>> entry : tablesPerQual.entrySet()) {
            String qual = entry.getKey();
            Map<String, Table> tableMap = entry.getValue();
            // add hier as well in fragments
            tableMap.remove(getTableAlias(hierTable, qual));
            List<Table> tables = new LinkedList<Table>(tableMap.values());
            tables.add(getTable(hierTable, qual));
            qualTables.put(qual, tables);
        }

        Query q = new Query();
        List<String> whereClauses = new LinkedList<String>();

        /*
         * Walk joins.
         */

        boolean first = true;
        StringBuilder buf = new StringBuilder();
        int njoin = 0;
        for (Join j : walker.from_joins) {

            // table this join is about

            String qual = j.corr;
            njoin++;
            Table table = null;
            if (first) {
                // start with hier
                table = getTable(hierTable, qual);
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
            String tableMainId = table.getColumn(model.MAIN_KEY).getFullQuotedName();

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
                buf.append(tableMainId);
            }

            // restrict to relevant primary types

            String nuxeoType;
            boolean skipFolderish;
            if (j.table.equalsIgnoreCase(BaseType.DOCUMENT.getId())) {
                nuxeoType = TypeConstants.DOCUMENT;
                skipFolderish = true;
            } else if (j.table.equalsIgnoreCase(BaseType.FOLDER.getId())) {
                nuxeoType = "Folder"; // TODO extract constant
                skipFolderish = false;
            } else {
                nuxeoType = j.table;
                skipFolderish = false;
            }
            Set<String> subTypes = model.getDocumentSubTypes(nuxeoType);
            if (subTypes == null) {
                throw new QueryMakerException("Unknown type: " + j.table);
            }
            List<String> types = new ArrayList<String>();
            List<String> qms = new ArrayList<String>();
            for (String type : subTypes) {
                if (skipFolderish
                        && model.getDocumentTypeFacets(type).contains(
                                FacetNames.FOLDERISH)) {
                    continue;
                }
                if (type.equals(model.ROOT_TYPE)) {
                    continue;
                }
                types.add(type);
                qms.add("?");
            }
            if (types.isEmpty()) {
                types.add("__NOSUCHTYPE__");
                qms.add("?");
            }
            whereClauses.add(String.format("%s IN (%s)", table.getColumn(
                    model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName(),
                    StringUtils.join(qms, ", ")));
            for (String type : types) {
                q.selectParams.add(type);
            }

            // security check

            if (queryFilter != null && queryFilter.getPrincipals() != null) {
                Serializable principals;
                Serializable permissions;
                if (dialect.supportsArrays()) {
                    principals = queryFilter.getPrincipals();
                    permissions = queryFilter.getPermissions();
                } else {
                    principals = StringUtils.join(queryFilter.getPrincipals(),
                            '|');
                    permissions = StringUtils.join(
                            queryFilter.getPermissions(), '|');
                }
                if (dialect.supportsReadAcl()) {
                    /* optimized read acl */
                    String readAclTable;
                    String readAclIdCol;
                    String readAclAclIdCol;
                    if (walker.from_joins.size() == 1) {
                        readAclTable = model.HIER_READ_ACL_TABLE_NAME;
                        readAclIdCol = model.HIER_READ_ACL_TABLE_NAME + '.'
                                + model.HIER_READ_ACL_ID;
                        readAclAclIdCol = model.HIER_READ_ACL_TABLE_NAME + '.'
                                + model.HIER_READ_ACL_ACL_ID;
                    } else {
                        String alias = "nxr" + njoin;
                        readAclTable = model.HIER_READ_ACL_TABLE_NAME + " "
                                + alias; // TODO dialect
                        readAclIdCol = alias + '.' + model.HIER_READ_ACL_ID;
                        readAclAclIdCol = alias + '.'
                                + model.HIER_READ_ACL_ACL_ID;
                    }
                    whereClauses.add(dialect.getReadAclsCheckSql(readAclAclIdCol));
                    q.selectParams.add(principals);
                    buf.append(String.format(" JOIN %s ON %s = %s",
                            readAclTable, tableMainId, readAclIdCol));
                } else {
                    whereClauses.add(dialect.getSecurityCheckSql(tableMainId));
                    q.selectParams.add(principals);
                    q.selectParams.add(permissions);
                }
            }

            first = false;
        }

        /*
         * Where clause.
         */

        if (walker.select_where != null) {
            whereClauses.add('(' + walker.select_where + ')');
        }

        /*
         * What we select.
         */

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

    protected Column findColumn(String name, String qualifier, boolean multi) {
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
            if (multi != propertyInfo.propertyType.isArray()) {
                String msg = multi ? "Must use multi-valued property instead of %s"
                        : "Cannot use multi-valued property %s";
                throw new QueryMakerException(String.format(msg, name));
            }
            if (multi) {
                column = table.getColumn(model.COLL_TABLE_VALUE_KEY);
            } else {
                column = table.getColumn(propertyInfo.fragmentKey);
            }
        }
        if (qualifier != null) {
            column = getTable(column.getTable(), qualifier).getColumn(
                    column.getKey());
            // TODO ensure key == name, or add getName()
        }
        return column;
    }

    protected Table getTable(Table table, String qualifier) {
        if (qualifier == null) {
            return table;
        } else {
            return new TableAlias(table, getTableAlias(table, qualifier));
        }
    }

    protected String getTableAlias(Table table, String qualifier) {
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

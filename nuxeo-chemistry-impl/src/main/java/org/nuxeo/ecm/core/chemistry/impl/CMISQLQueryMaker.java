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
import java.util.HashSet;
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

    // ----- filled during the tree walk -----

    /** columns referenced, keyed by full quoted name (includes alias name) */
    public Map<String, Column> columns = new HashMap<String, Column>();

    /** qualifier to set of columns full quoted names */
    public Map<String, Set<String>> columnsPerQual = new HashMap<String, Set<String>>();

    /** original column names as specified in query */
    public Map<String, String> columnsSpecified = new HashMap<String, String>();

    /** joins added by fulltext match */
    public final List<String> fulltextJoins = new LinkedList<String>();

    /** joins params added by fulltext match */
    public final List<String> fulltextJoinsParams = new LinkedList<String>();

    public List<String> errorMessages = new LinkedList<String>();

    protected static class Join {
        String kind;

        String table;

        String corr;

        String on1;

        String on2;
    }

    protected static class QueryMakerException extends RuntimeException {
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
        if (!errorMessages.isEmpty()) {
            throw new StorageException("Cannot parse query: " + query + " ("
                    + StringUtils.join(errorMessages, ", ") + ")");
        }

        /*
         * Find info about fragments needed.
         */

        // add main id to all qualifiers
        for (String qual : new ArrayList<String>(columnsPerQual.keySet())) {
            for (String propertyId : Arrays.asList(Property.ID,
                    Property.TYPE_ID)) {
                String col = referToColumn(propertyId, qual);
                if (!walker.select_what.contains(col)) {
                    walker.select_what.add(col);
                }
            }
        }

        // per qualifier, find all tables (fragments)
        Map<String, Map<String, Table>> tablesPerQual = new HashMap<String, Map<String, Table>>();
        for (Entry<String, Set<String>> entry : columnsPerQual.entrySet()) {
            String qual = entry.getKey();
            Map<String, Table> tables = tablesPerQual.get(qual);
            if (tables == null) {
                tablesPerQual.put(qual, tables = new HashMap<String, Table>());
            }
            for (String fqn : entry.getValue()) {
                Column col = columns.get(fqn);
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

        List<String> whereClauses = new LinkedList<String>();
        List<Serializable> whereParams = new LinkedList<Serializable>();

        /*
         * Walk joins.
         */

        StringBuilder from = new StringBuilder();
        List<Serializable> fromParams = new LinkedList<Serializable>();
        int njoin = 0;
        for (Join j : walker.from_joins) {
            njoin++;

            // table this join is about

            String qual = j.corr;
            Table table = null;
            if (njoin == 1) {
                // start with hier
                table = getTable(hierTable, qual);
            } else {
                // do requested join
                if (j.kind.equals("LEFT") || j.kind.equals("RIGHT")) {
                    from.append(" ");
                    from.append(j.kind);
                }
                from.append(" JOIN ");
                // find which table in on1/on2 refers to current qualifier
                Set<String> tables = columnsPerQual.get(qual);
                for (String fqn : Arrays.asList(j.on1, j.on2)) {
                    if (!tables.contains(fqn)) {
                        continue;
                    }
                    Column col = columns.get(fqn);
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
            from.append(name);

            if (njoin != 1) {
                // emit actual join requested
                from.append(" ON ");
                from.append(j.on1);
                from.append(" = ");
                from.append(j.on2);
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
                from.append(" LEFT JOIN ");
                from.append(n);
                from.append(" ON ");
                from.append(t.getColumn(model.MAIN_KEY).getFullQuotedName());
                from.append(" = ");
                from.append(tableMainId);
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
                whereParams.add(type);
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
                    whereParams.add(principals);
                    from.append(String.format(" JOIN %s ON %s = %s",
                            readAclTable, tableMainId, readAclIdCol));
                } else {
                    whereClauses.add(dialect.getSecurityCheckSql(tableMainId));
                    whereParams.add(principals);
                    whereParams.add(permissions);
                }
            }
        }

        /*
         * Joins for the external fulltext matches (H2).
         */

        for (int ftj = 0; ftj < fulltextJoins.size(); ftj++) {
            // LEFT JOIN because we want a row even if there's no match
            // so that the WHERE clause can test and provide a boolean
            from.append(" LEFT JOIN " + fulltextJoins.get(ftj));
            fromParams.add(fulltextJoinsParams.get(ftj));
        }

        /*
         * Where clause.
         */

        if (walker.select_where != null) {
            whereClauses.add('(' + walker.select_where + ')');
            whereParams.addAll(walker.select_where_params);
        }

        /*
         * What we select.
         */

        List<Column> whatColumns = new LinkedList<Column>();
        List<String> whatColumnsAliases = new LinkedList<String>();
        for (String col : walker.select_what) {
            whatColumns.add(columns.get(col));
            whatColumnsAliases.add(columnsSpecified.get(col));
        }
        String what = StringUtils.join(walker.select_what, ", ");

        /*
         * Create the whole select.
         */

        Select select = new Select(null);
        select.setWhat(what);
        select.setFrom(from.toString());
        // TODO(fromParams); // TODO add before whereParams
        select.setWhere(StringUtils.join(whereClauses, " AND "));
        select.setOrderBy(StringUtils.join(walker.select_orderby, ", "));

        Query q = new Query();
        q.selectInfo = new SQLInfoSelect(select.getStatement(), whatColumns,
                whatColumnsAliases, null, null);
        q.selectParams = fromParams;
        q.selectParams.addAll(whereParams);
        return q;
    }

    public String referToColumn(String c, String qual) {
        Column col = findColumn(c, qual, false);
        String fqn = col.getFullQuotedName();
        columns.put(fqn, col);
        columnsSpecified.put(fqn, getCanonicalColumnName(c, qual));
        Set<String> set = columnsPerQual.get(qual);
        if (set == null) {
            columnsPerQual.put(qual, set = new HashSet<String>());
        }
        set.add(fqn);
        return fqn;
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

    protected String getInFolderSql(String qual, String arg,
            List<Serializable> params) {
        String idCol = referToColumn(Property.PARENT_ID, qual);
        params.add(arg);
        return idCol + " = ?";
    }

    protected String getInTreeSql(String qual, String arg,
            List<Serializable> params) {
        String idCol = referToColumn(Property.ID, qual);
        params.add(arg);
        return dialect.getInTreeSql(idCol);
    }

    protected String getContainsSql(String qual, String arg,
            List<Serializable> params) {
        Column mainCol = findColumn(Property.ID, qual, false);
        String[] info = dialect.getFulltextMatch(Model.FULLTEXT_DEFAULT_INDEX,
                arg, mainCol, model, database);
        String joinExpr = info[0];
        String joinParam = info[1];
        String whereExpr = info[2];
        String whereParam = info[3];
        String joinAlias = getFtJoinAlias(++ftJoinNumber);
        if (joinExpr != null) {
            // specific join table (H2)
            fulltextJoins.add(String.format(joinExpr, joinAlias));
            if (joinParam != null) {
                fulltextJoinsParams.add(joinParam);
            }
            // XXX compat with older Nuxeo, now presetn in DialectH2
            if (whereExpr == null) {
                whereExpr = "%s.KEY IS NOT NULL";
                whereParam = null;
            }
        }
        String sql = String.format(whereExpr, joinAlias);
        if (whereParam != null) {
            params.add(whereParam);
        }
        return sql;
    }

    private int ftJoinNumber;

    private String getFtJoinAlias(int num) {
        if (num == 1) {
            return "_FT";
        } else {
            return "_FT" + num;
        }
    }

}

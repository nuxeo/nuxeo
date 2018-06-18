/*
 * (C) Copyright 2006-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.opencmis.impl.server;

import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IN_MIGRATION;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDICATED_PROPERTY;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.server.support.query.AbstractPredicateWalker;
import org.apache.chemistry.opencmis.server.support.query.CmisQlStrictLexer;
import org.apache.chemistry.opencmis.server.support.query.CmisSelector;
import org.apache.chemistry.opencmis.server.support.query.ColumnReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference.CmisQlFunction;
import org.apache.chemistry.opencmis.server.support.query.QueryObject;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.JoinSpec;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.SortSpec;
import org.apache.chemistry.opencmis.server.support.query.QueryUtilStrict;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.opencmis.impl.util.TypeManagerImpl;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.security.SecurityPolicy;
import org.nuxeo.ecm.core.security.SecurityPolicy.QueryTransformer;
import org.nuxeo.ecm.core.security.SecurityPolicyService;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelProperty;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.MapMaker;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Select;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.TableAlias;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextMatchInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Transformer of CMISQL queries into real SQL queries for the actual database.
 */
public class CMISQLQueryMaker implements QueryMaker {

    private static final Log log = LogFactory.getLog(CMISQLQueryMaker.class);

    public static final String TYPE = "CMISQL";

    public static final String CMIS_PREFIX = "cmis:";

    public static final String NX_PREFIX = "nuxeo:";

    public static final String DC_FRAGMENT_NAME = "dublincore";

    public static final String DC_TITLE_KEY = "title";

    public static final String DC_DESCRIPTION_KEY = "description";

    public static final String DC_CREATOR_KEY = "creator";

    public static final String DC_CREATED_KEY = "created";

    public static final String DC_MODIFIED_KEY = "modified";

    public static final String DC_LAST_CONTRIBUTOR_KEY = "lastContributor";

    public static final String REL_FRAGMENT_NAME = "relation";

    public static final String REL_SOURCE_KEY = "source";

    public static final String REL_TARGET_KEY = "target";

    // list of SQL column where NULL (missing value) should be treated as
    // Boolean.FALSE
    public static final Set<String> NULL_IS_FALSE_COLUMNS = new HashSet<>(
            Arrays.asList(Model.HIER_TABLE_NAME + " " + Model.MAIN_IS_VERSION_KEY,
                    Model.VERSION_TABLE_NAME + " " + Model.VERSION_IS_LATEST_KEY,
                    Model.VERSION_TABLE_NAME + " " + Model.VERSION_IS_LATEST_MAJOR_KEY,
                    Model.HIER_TABLE_NAME + " " + Model.MAIN_CHECKED_IN_KEY,
                    Model.HIER_TABLE_NAME + " " + Model.MAIN_IS_TRASHED_KEY));

    /**
     * These mixins never match an instance mixin when used in a clause nuxeo:secondaryObjectTypeIds = 'foo'
     */
    protected static final Set<String> MIXINS_NOT_PER_INSTANCE = new HashSet<>(
            Arrays.asList(FacetNames.FOLDERISH, FacetNames.HIDDEN_IN_NAVIGATION));

    protected Database database;

    protected Dialect dialect;

    protected Model model;

    protected Table hierTable;

    public boolean skipDeleted = true;

    // ----- filled during walks of the clauses -----

    protected QueryUtilStrict queryUtil;

    protected QueryObject query;

    protected FulltextMatchInfo fulltextMatchInfo;

    protected Set<String> lifecycleWhereClauseQualifiers = new HashSet<>();

    protected Set<String> mixinTypeWhereClauseQualifiers = new HashSet<>();

    /** Qualifier to type. */
    protected Map<String, String> qualifierToType = new HashMap<>();

    /** Qualifier to canonical qualifier (correlation name). */
    protected Map<String, String> canonicalQualifier = new HashMap<>();

    /** Map of qualifier -> fragment -> table */
    protected Map<String, Map<String, Table>> allTables = new HashMap<>();

    /** All qualifiers used (includes virtual columns) */
    protected Set<String> allQualifiers = new HashSet<>();

    /** The qualifiers which correspond to versionable types. */
    protected Set<String> versionableQualifiers = new HashSet<>();

    /** The columns we'll actually request from the database. */
    protected List<SqlColumn> realColumns = new LinkedList<>();

    /** Parameters for above (for SCORE expressions on some databases) */
    protected List<String> realColumnsParams = new LinkedList<>();

    /** The non-real-columns we'll return as well. */
    protected Map<String, ColumnReference> virtualColumns = new HashMap<>();

    /** Type info returned to caller. */
    protected Map<String, PropertyDefinition<?>> typeInfo = null;

    /** Search only latest version = !searchAllVersions. */
    protected boolean searchLatestVersion = false;

    /** used for diagnostic when using DISTINCT */
    protected List<String> virtualColumnNames = new LinkedList<>();

    /**
     * Column corresponding to a returned value computed from an actual SQL expression.
     */
    public static class SqlColumn {

        /** Column name or expression passed to SQL statement. */
        public final String sql;

        /** Column used to get the value from the result set. */
        public final Column column;

        /** Key for the value returned to the caller. */
        public final String key;

        public SqlColumn(String sql, Column column, String key) {
            this.sql = sql;
            this.column = column;
            this.key = key;
        }
    }

    @Override
    public String getName() {
        return TYPE;
    }

    @Override
    public boolean accepts(String queryType) {
        return queryType.equals(TYPE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The optional parameters must be passed: {@code params[0]} is the {@link NuxeoCmisService}, optional
     * {@code params[1]} is a type info map, optional {@code params[2]} is searchAllVersions (default
     * {@code Boolean.TRUE} for this method).
     */
    @Override
    public Query buildQuery(SQLInfo sqlInfo, Model model, PathResolver pathResolver, String statement,
            QueryFilter queryFilter, Object... params) {
        database = sqlInfo.database;
        dialect = sqlInfo.dialect;
        this.model = model;
        NuxeoCmisService service = (NuxeoCmisService) params[0];
        if (params.length > 1) {
            typeInfo = (Map<String, PropertyDefinition<?>>) params[1];
        }
        if (params.length > 2) {
            Boolean searchAllVersions = (Boolean) params[2];
            searchLatestVersion = Boolean.FALSE.equals(searchAllVersions);
        }
        TypeManagerImpl typeManager = service.getTypeManager();
        TrashService trashService = Framework.getService(TrashService.class);

        boolean addSystemColumns = true; // TODO

        hierTable = database.getTable(Model.HIER_TABLE_NAME);

        statement = applySecurityPolicyQueryTransformers(service, queryFilter.getPrincipal(), statement);
        try {
            queryUtil = new QueryUtilStrict(statement, typeManager, new AnalyzingWalker(), false);
            queryUtil.processStatement();
            query = queryUtil.getQueryObject();
        } catch (RecognitionException e) {
            throw new QueryParseException(queryUtil.getErrorMessage(e), e);
        }

        resolveQualifiers();

        // now resolve column selectors to actual database columns
        for (CmisSelector sel : query.getSelectReferences()) {
            recordSelectSelector(sel);
        }
        for (CmisSelector sel : query.getJoinReferences()) {
            recordSelector(sel, ClauseType.JOIN);
        }
        for (CmisSelector sel : query.getWhereReferences()) {
            recordSelector(sel, ClauseType.WHERE);
        }
        for (SortSpec spec : query.getOrderBys()) {
            recordSelector(spec.getSelector(), ClauseType.ORDER_BY);
        }

        findVersionableQualifiers();

        boolean distinct = false; // TODO extension
        addSystemColumns(addSystemColumns, distinct);

        /*
         * Find info about fragments needed.
         */

        List<String> whereClauses = new LinkedList<String>();
        List<Serializable> whereParams = new LinkedList<Serializable>();

        /*
         * Walk joins.
         */

        List<JoinSpec> joins = query.getJoins();
        StringBuilder from = new StringBuilder();
        List<Serializable> fromParams = new LinkedList<Serializable>();
        for (int njoin = -1; njoin < joins.size(); njoin++) {
            JoinSpec join;
            boolean outerJoin;
            String alias;
            if (njoin == -1) {
                join = null;
                outerJoin = false;
                alias = query.getMainTypeAlias();
            } else {
                join = joins.get(njoin);
                outerJoin = join.kind.equals("LEFT") || join.kind.equals("RIGHT");
                alias = join.alias;
            }

            String typeQueryName = qualifierToType.get(alias);
            String qual = canonicalQualifier.get(alias);
            Table qualHierTable = getTable(hierTable, qual);

            // determine relevant primary types

            List<String> types = new ArrayList<String>();
            TypeDefinition td = query.getTypeDefinitionFromQueryName(typeQueryName);
            if (td.getParentTypeId() != null) {
                // don't add abstract root types
                types.add(td.getId());
            }
            LinkedList<TypeDefinitionContainer> typesTodo = new LinkedList<TypeDefinitionContainer>();
            typesTodo.addAll(typeManager.getTypeDescendants(td.getId(), -1, Boolean.TRUE));
            // recurse to get all subtypes
            TypeDefinitionContainer tc;
            while ((tc = typesTodo.poll()) != null) {
                types.add(tc.getTypeDefinition().getId());
                typesTodo.addAll(tc.getChildren());
            }
            if (types.isEmpty()) {
                // shoudn't happen
                types = Collections.singletonList("__NOSUCHTYPE__");
            }
            // build clause
            StringBuilder qms = new StringBuilder();
            for (int i = 0; i < types.size(); i++) {
                if (i != 0) {
                    qms.append(", ");
                }
                qms.append("?");
            }
            String primaryTypeClause = String.format("%s IN (%s)",
                    qualHierTable.getColumn(Model.MAIN_PRIMARY_TYPE_KEY).getFullQuotedName(), qms);

            // table this join is about

            Table table;
            if (join == null) {
                table = qualHierTable;
            } else {
                // find which table in onLeft/onRight refers to current
                // qualifier
                table = null;
                for (ColumnReference col : Arrays.asList(join.onLeft, join.onRight)) {
                    if (alias.equals(col.getQualifier())) {
                        // TODO match with canonical qualifier instead?
                        table = ((Column) col.getInfo()).getTable();
                        break;
                    }
                }
                if (table == null) {
                    throw new QueryParseException("Bad query, qualifier not found: " + qual);
                }
            }
            String tableName;
            if (table.isAlias()) {
                tableName = table.getRealTable().getQuotedName() + " " + table.getQuotedName();
            } else {
                tableName = table.getQuotedName();
            }
            boolean isRelation = table.getKey().equals(REL_FRAGMENT_NAME);

            // join clause on requested columns

            boolean primaryTypeClauseDone = false;

            if (join == null) {
                from.append(tableName);
            } else {
                if (outerJoin) {
                    from.append(" ");
                    from.append(join.kind);
                }
                from.append(" JOIN ");
                from.append(tableName);
                from.append(" ON (");
                from.append(((Column) join.onLeft.getInfo()).getFullQuotedName());
                from.append(" = ");
                from.append(((Column) join.onRight.getInfo()).getFullQuotedName());
                if (outerJoin && table.getKey().equals(Model.HIER_TABLE_NAME)) {
                    // outer join, type check must be part of JOIN
                    from.append(" AND ");
                    from.append(primaryTypeClause);
                    fromParams.addAll(types);
                    primaryTypeClauseDone = true;
                }
                from.append(")");
            }

            // join other fragments for qualifier

            String tableMainId = table.getColumn(Model.MAIN_KEY).getFullQuotedName();

            for (Table t : allTables.get(qual).values()) {
                if (t.getKey().equals(table.getKey())) {
                    // already done above
                    continue;
                }
                String n;
                if (t.isAlias()) {
                    n = t.getRealTable().getQuotedName() + " " + t.getQuotedName();
                } else {
                    n = t.getQuotedName();
                }
                from.append(" LEFT JOIN ");
                from.append(n);
                from.append(" ON (");
                from.append(t.getColumn(Model.MAIN_KEY).getFullQuotedName());
                from.append(" = ");
                from.append(tableMainId);
                if (outerJoin && t.getKey().equals(Model.HIER_TABLE_NAME)) {
                    // outer join, type check must be part of JOIN
                    from.append(" AND ");
                    from.append(primaryTypeClause);
                    fromParams.addAll(types);
                    primaryTypeClauseDone = true;
                }
                from.append(")");
            }

            // primary type clause, if not included in a JOIN

            if (!primaryTypeClauseDone) {
                whereClauses.add(primaryTypeClause);
                whereParams.addAll(types);
            }

            // not trashed filter

            if (skipDeleted) {
                Supplier<String> lifecycleTrashedConstraint = () -> {
                    ModelProperty propertyInfo = model.getPropertyInfo(Model.MISC_LIFECYCLE_STATE_PROP);
                    Column lscol = getTable(database.getTable(propertyInfo.fragmentName), qual).getColumn(
                            propertyInfo.fragmentKey);
                    String lscolName = lscol.getFullQuotedName();
                    return String.format("(%s <> ? OR %s IS NULL)", lscolName, lscolName);
                };
                Supplier<String> propertyTrashedConstraint = () -> {
                    ModelProperty propertyInfo = model.getPropertyInfo(Model.MAIN_IS_TRASHED_PROP);
                    Column lscol = qualHierTable.getColumn(propertyInfo.fragmentKey);
                    String lscolName = lscol.getFullQuotedName();
                    return String.format("(%s <> ? OR %s IS NULL)", lscolName, lscolName);
                };

                if (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
                    whereClauses.add(lifecycleTrashedConstraint.get());
                    whereParams.add(LifeCycleConstants.DELETED_STATE);

                } else if (trashService.hasFeature(TRASHED_STATE_IN_MIGRATION)) {
                    whereClauses.add(String.format("(%s OR %s)", lifecycleTrashedConstraint.get(),
                            propertyTrashedConstraint.get()));
                    whereParams.add(LifeCycleConstants.DELETED_STATE);
                    whereParams.add(Boolean.TRUE);

                } else if (trashService.hasFeature(TRASHED_STATE_IS_DEDICATED_PROPERTY)) {
                    whereClauses.add(propertyTrashedConstraint.get());
                    whereParams.add(Boolean.TRUE);
                }
            }

            // searchAllVersions filter

            boolean versionable = versionableQualifiers.contains(qual);
            if (searchLatestVersion && versionable) {
                // add islatestversion = true
                Table ver = getTable(database.getTable(Model.VERSION_TABLE_NAME), qual);
                Column latestvercol = ver.getColumn(Model.VERSION_IS_LATEST_KEY);
                String latestvercolName = latestvercol.getFullQuotedName();
                whereClauses.add(String.format("(%s = ?)", latestvercolName));
                whereParams.add(Boolean.TRUE);
            }

            // security check

            boolean checkSecurity = !isRelation && queryFilter.getPrincipals() != null;
            if (checkSecurity) {
                Serializable principals;
                Serializable permissions;
                if (dialect.supportsArrays()) {
                    principals = queryFilter.getPrincipals();
                    permissions = queryFilter.getPermissions();
                } else {
                    principals = StringUtils.join(queryFilter.getPrincipals(), '|');
                    permissions = StringUtils.join(queryFilter.getPermissions(), '|');
                }
                if (dialect.supportsReadAcl()) {
                    /* optimized read acl */
                    String readAclTable;
                    String readAclTableAlias;
                    String aclrumTable;
                    String aclrumTableAlias;
                    if (joins.size() == 0) {
                        readAclTable = Model.HIER_READ_ACL_TABLE_NAME;
                        readAclTableAlias = readAclTable;
                        aclrumTable = Model.ACLR_USER_MAP_TABLE_NAME;
                        aclrumTableAlias = aclrumTable;
                    } else {
                        readAclTableAlias = "nxr" + (njoin + 1);
                        readAclTable = Model.HIER_READ_ACL_TABLE_NAME + ' ' + readAclTableAlias; // TODO dialect
                        aclrumTableAlias = "aclrum" + (njoin + 1);
                        aclrumTable = Model.ACLR_USER_MAP_TABLE_NAME + ' ' + aclrumTableAlias; // TODO dialect
                    }
                    String readAclIdCol = readAclTableAlias + '.' + Model.HIER_READ_ACL_ID;
                    String readAclAclIdCol = readAclTableAlias + '.' + Model.HIER_READ_ACL_ACL_ID;
                    String aclrumAclIdCol = aclrumTableAlias + '.' + Model.ACLR_USER_MAP_ACL_ID;
                    String aclrumUserIdCol = aclrumTableAlias + '.' + Model.ACLR_USER_MAP_USER_ID;
                    // first join with hierarchy_read_acl
                    if (outerJoin) {
                        from.append(" ");
                        from.append(join.kind);
                    }
                    from.append(String.format(" JOIN %s ON (%s = %s)", readAclTable, tableMainId, readAclIdCol));
                    // second join with aclr_user_map
                    String securityCheck = dialect.getReadAclsCheckSql(aclrumUserIdCol);
                    String joinOn = String.format("%s = %s", readAclAclIdCol, aclrumAclIdCol);
                    if (outerJoin) {
                        from.append(" ");
                        from.append(join.kind);
                        // outer join, security check must be part of JOIN
                        joinOn = String.format("%s AND %s", joinOn, securityCheck);
                        fromParams.add(principals);
                    } else {
                        // inner join, security check can go in WHERE clause
                        whereClauses.add(securityCheck);
                        whereParams.add(principals);
                    }
                    from.append(String.format(" JOIN %s ON (%s)", aclrumTable, joinOn));
                } else {
                    String securityCheck = dialect.getSecurityCheckSql(tableMainId);
                    if (outerJoin) {
                        securityCheck = String.format("(%s OR %s IS NULL)", securityCheck, tableMainId);
                    }
                    whereClauses.add(securityCheck);
                    whereParams.add(principals);
                    whereParams.add(permissions);
                }
            }
        }

        /*
         * WHERE clause.
         */

        Tree whereNode = queryUtil.getWalker().getWherePredicateTree();
        if (whereNode != null) {
            GeneratingWalker generator = new GeneratingWalker();
            generator.walkPredicate(whereNode);
            whereClauses.add(generator.whereBuf.toString());
            whereParams.addAll(generator.whereBufParams);

            // add JOINs for the external fulltext matches
            Collections.sort(generator.ftJoins); // implicit JOINs last
                                                 // (PostgreSQL)
            for (org.nuxeo.ecm.core.storage.sql.jdbc.db.Join join : generator.ftJoins) {
                from.append(join.toSql(dialect));
                if (join.tableParam != null) {
                    fromParams.add(join.tableParam);
                }
            }
        }

        /*
         * SELECT clause.
         */

        List<String> selectWhat = new ArrayList<String>();
        List<Serializable> selectParams = new ArrayList<Serializable>(1);
        for (SqlColumn rc : realColumns) {
            selectWhat.add(rc.sql);
        }
        selectParams.addAll(realColumnsParams);

        CMISQLMapMaker mapMaker = new CMISQLMapMaker(realColumns, virtualColumns, service);
        String what = StringUtils.join(selectWhat, ", ");
        if (distinct) {
            what = "DISTINCT " + what;
        }

        /*
         * ORDER BY clause.
         */

        List<String> orderbys = new LinkedList<String>();
        for (SortSpec spec : query.getOrderBys()) {
            String orderby;
            CmisSelector sel = spec.getSelector();
            if (sel instanceof ColumnReference) {
                Column column = (Column) sel.getInfo();
                orderby = column.getFullQuotedName();
            } else {
                orderby = fulltextMatchInfo.scoreAlias;
            }
            if (!spec.ascending) {
                orderby += " DESC";
            }
            orderbys.add(orderby);
        }

        /*
         * Create the whole select.
         */

        Select select = new Select(null);
        select.setWhat(what);
        select.setFrom(from.toString());
        // TODO(fromParams); // TODO add before whereParams
        select.setWhere(StringUtils.join(whereClauses, " AND "));
        select.setOrderBy(StringUtils.join(orderbys, ", "));

        Query q = new Query();
        q.selectInfo = new SQLInfoSelect(select.getStatement(), mapMaker);
        q.selectParams = selectParams;
        q.selectParams.addAll(fromParams);
        q.selectParams.addAll(whereParams);
        return q;
    }

    /**
     * Applies security policies query transformers to the statement, if possible. Otherwise raises an exception.
     *
     * @since 5.7.2
     * @throws CmisRuntimeException If a security policy prevents doing CMIS queries.
     */
    protected String applySecurityPolicyQueryTransformers(NuxeoCmisService service, Principal principal,
            String statement) {
        SecurityPolicyService securityPolicyService = Framework.getService(SecurityPolicyService.class);
        if (securityPolicyService == null) {
            return statement;
        }
        String repositoryId = service.getNuxeoRepository().getId();
        for (SecurityPolicy policy : securityPolicyService.getPolicies()) {
            if (!policy.isRestrictingPermission(SecurityConstants.BROWSE)) {
                continue;
            }
            // check CMISQL transformer (new @since 5.7.2)
            if (!policy.isExpressibleInQuery(repositoryId, TYPE)) {
                throw new CmisRuntimeException(
                        "Security policy " + policy.getClass().getName() + " prevents CMISQL execution");
            }
            QueryTransformer transformer = policy.getQueryTransformer(repositoryId, TYPE);
            statement = transformer.transform(principal, statement);
        }
        return statement;
    }

    protected void findVersionableQualifiers() {
        List<JoinSpec> joins = query.getJoins();
        for (int njoin = -1; njoin < joins.size(); njoin++) {
            boolean firstTable = njoin == -1;
            String alias;
            if (firstTable) {
                alias = query.getMainTypeAlias();
            } else {
                alias = joins.get(njoin).alias;
            }
            String typeQueryName = qualifierToType.get(alias);
            TypeDefinition td = query.getTypeDefinitionFromQueryName(typeQueryName);
            boolean versionable = td.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT;
            if (versionable) {
                String qual = canonicalQualifier.get(alias);
                versionableQualifiers.add(qual);
            }
        }
    }

    protected boolean isFacetsColumn(String name) {
        return PropertyIds.SECONDARY_OBJECT_TYPE_IDS.equals(name) || NuxeoTypeHelper.NX_FACETS.equals(name);
    }

    // add main id to all qualifiers if
    // - we have no DISTINCT (in which case more columns don't matter), or
    // - we have virtual columns, or
    // - system columns are requested
    // check no added columns would bias the DISTINCT
    // after this method, allTables also contain hier table for virtual columns
    protected void addSystemColumns(boolean addSystemColumns, boolean distinct) {
        TrashService trashService = Framework.getService(TrashService.class);
        boolean lifeCycleTrashState = trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)
                || trashService.hasFeature(TRASHED_STATE_IN_MIGRATION);

        List<CmisSelector> addedSystemColumns = new ArrayList<CmisSelector>(2);

        for (String qual : allQualifiers) {
            TypeDefinition type = getTypeForQualifier(qual);

            // additional references to cmis:objectId and cmis:objectTypeId
            for (String propertyId : Arrays.asList(PropertyIds.OBJECT_ID, PropertyIds.OBJECT_TYPE_ID)) {
                ColumnReference col = new ColumnReference(qual, propertyId);
                col.setTypeDefinition(propertyId, type);
                String key = getColumnKey(col);
                boolean add = true;
                for (SqlColumn rc : realColumns) {
                    if (rc.key.equals(key)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    addedSystemColumns.add(col);
                }
            }
            if (skipDeleted && lifeCycleTrashState || lifecycleWhereClauseQualifiers.contains(qual)) {
                // add lifecycle state column
                ModelProperty propertyInfo = model.getPropertyInfo(Model.MISC_LIFECYCLE_STATE_PROP);
                Table table = getTable(database.getTable(propertyInfo.fragmentName), qual);
                recordFragment(qual, table);
            }
            if (mixinTypeWhereClauseQualifiers.contains(qual)) {
                recordFragment(qual, getTable(hierTable, qual));
            }
        }

        // additional system columns to select on
        if (!distinct) {
            for (CmisSelector col : addedSystemColumns) {
                recordSelectSelector(col);
            }
        } else {
            if (!addedSystemColumns.isEmpty()) {
                if (!virtualColumnNames.isEmpty()) {
                    throw new QueryParseException(
                            "Cannot use DISTINCT with virtual columns: " + StringUtils.join(virtualColumnNames, ", "));
                }
                if (addSystemColumns) {
                    throw new QueryParseException("Cannot use DISTINCT without explicit " + PropertyIds.OBJECT_ID);
                }
                // don't add system columns as it would prevent DISTINCT from
                // working
            }
        }

        // for all qualifiers
        for (String qual : allQualifiers) {
            // include hier in fragments
            recordFragment(qual, getTable(hierTable, qual));
            // if only latest version include the version table
            boolean versionable = versionableQualifiers.contains(qual);
            if (searchLatestVersion && versionable) {
                Table ver = database.getTable(Model.VERSION_TABLE_NAME);
                recordFragment(qual, getTable(ver, qual));
            }
        }

    }

    /**
     * Records a SELECT selector, and associates it to a database column.
     */
    protected void recordSelectSelector(CmisSelector sel) {
        if (sel instanceof FunctionReference) {
            FunctionReference fr = (FunctionReference) sel;
            if (fr.getFunction() != CmisQlFunction.SCORE) {
                throw new CmisRuntimeException("Unknown function: " + fr.getFunction());
            }
            String key = fr.getAliasName();
            if (key == null) {
                key = "SEARCH_SCORE"; // default, from spec
            }
            String scoreExprSql = fulltextMatchInfo.scoreExpr + " AS " + fulltextMatchInfo.scoreAlias;
            SqlColumn c = new SqlColumn(scoreExprSql, fulltextMatchInfo.scoreCol, key);
            realColumns.add(c);
            if (fulltextMatchInfo.scoreExprParam != null) {
                realColumnsParams.add(fulltextMatchInfo.scoreExprParam);
            }
            if (typeInfo != null) {
                PropertyDecimalDefinitionImpl pd = new PropertyDecimalDefinitionImpl();
                pd.setId(key);
                pd.setQueryName(key);
                pd.setCardinality(Cardinality.SINGLE);
                pd.setDisplayName("Score");
                pd.setLocalName("score");
                typeInfo.put(key, pd);
            }
        } else { // sel instanceof ColumnReference
            ColumnReference col = (ColumnReference) sel;
            String qual = canonicalQualifier.get(col.getQualifier());

            if (col.getPropertyQueryName().equals("*")) {
                TypeDefinition type = getTypeForQualifier(qual);
                for (PropertyDefinition<?> pd : type.getPropertyDefinitions().values()) {
                    String id = pd.getId();
                    if ((pd.getCardinality() == Cardinality.SINGLE //
                            && Boolean.TRUE.equals(pd.isQueryable())) || id.equals(PropertyIds.BASE_TYPE_ID)) {
                        ColumnReference c = new ColumnReference(qual, id);
                        c.setTypeDefinition(id, type);
                        recordSelectSelector(c);
                    }
                }
                return;
            }

            String key = getColumnKey(col);
            PropertyDefinition<?> pd = col.getPropertyDefinition();
            Column column = getColumn(col);
            if (column != null && pd.getCardinality() == Cardinality.SINGLE) {
                col.setInfo(column);
                recordColumnFragment(qual, column);
                String sql = column.getFullQuotedName();
                SqlColumn c = new SqlColumn(sql, column, key);
                realColumns.add(c);
            } else {
                virtualColumns.put(key, col);
                virtualColumnNames.add(key);
                allQualifiers.add(qual);
            }
            if (typeInfo != null) {
                typeInfo.put(key, pd);
            }
        }
    }

    public enum ClauseType {
        JOIN,
        WHERE,
        ORDER_BY;
    }

    /**
     * Records a JOIN / WHERE / ORDER BY selector, and associates it to a database column.
     */
    protected void recordSelector(CmisSelector sel, ClauseType clauseType) {
        if (sel instanceof FunctionReference) {
            FunctionReference fr = (FunctionReference) sel;
            if (clauseType != ClauseType.ORDER_BY) {
                throw new QueryParseException("Cannot use function in " + clauseType + " clause: " + fr.getFunction());
            }
            // ORDER BY SCORE, nothing further to record
            if (fulltextMatchInfo == null) {
                throw new QueryParseException("Cannot use ORDER BY SCORE without CONTAINS");
            }
            return;
        }
        ColumnReference col = (ColumnReference) sel;
        PropertyDefinition<?> pd = col.getPropertyDefinition();
        boolean multi = pd.getCardinality() == Cardinality.MULTI;

        // fetch column and associate it to the selector
        Column column = getColumn(col);
        if (!isFacetsColumn(col.getPropertyId()) && column == null) {
            throw new QueryParseException(
                    "Cannot use column in " + clauseType + " clause: " + col.getPropertyQueryName());
        }
        col.setInfo(column);
        String qual = canonicalQualifier.get(col.getQualifier());

        TrashService trashService = Framework.getService(TrashService.class);
        boolean trashInMigration = trashService.hasFeature(TRASHED_STATE_IN_MIGRATION);
        if (clauseType == ClauseType.WHERE && NuxeoTypeHelper.NX_LIFECYCLE_STATE.equals(col.getPropertyId())
                && (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE) || trashInMigration)) {
            // explicit lifecycle query: do not include the 'deleted' lifecycle filter
            skipDeleted = false;
            lifecycleWhereClauseQualifiers.add(qual);
        }
        if (clauseType == ClauseType.WHERE && NuxeoTypeHelper.NX_ISTRASHED.equals(col.getPropertyId())
                && (trashService.hasFeature(TRASHED_STATE_IS_DEDICATED_PROPERTY) || trashInMigration)) {
            // explicit trashed query: do not include the `isTrashed = 0` filter
            skipDeleted = false;
        }
        if (clauseType == ClauseType.WHERE && isFacetsColumn(col.getPropertyId())) {
            mixinTypeWhereClauseQualifiers.add(qual);
        }
        // record as a needed fragment
        if (!multi) {
            recordColumnFragment(qual, column);
        }
    }

    /**
     * Records a database column's fragment (to know what to join).
     */
    protected void recordColumnFragment(String qual, Column column) {
        recordFragment(qual, column.getTable());
    }

    /**
     * Records a database table and qualifier (to know what to join).
     */
    protected void recordFragment(String qual, Table table) {
        String fragment = table.getKey();
        Map<String, Table> tablesByFragment = allTables.get(qual);
        if (tablesByFragment == null) {
            allTables.put(qual, tablesByFragment = new HashMap<>());
        }
        tablesByFragment.put(fragment, table);
        allQualifiers.add(qual);
    }

    /**
     * Finds what qualifiers are allowed and to what correlation name they are mapped.
     */
    protected void resolveQualifiers() {
        Map<String, String> types = query.getTypes();
        Map<String, AtomicInteger> typeCount = new HashMap<>();
        for (Entry<String, String> en : types.entrySet()) {
            String qual = en.getKey();
            String typeQueryName = en.getValue();
            qualifierToType.put(qual, typeQueryName);
            // if an alias, use as its own correlation name
            canonicalQualifier.put(qual, qual);
            // also use alias as correlation name for this type
            // (ambiguous types removed later)
            canonicalQualifier.put(typeQueryName, qual);
            // count type use
            if (!typeCount.containsKey(typeQueryName)) {
                typeCount.put(typeQueryName, new AtomicInteger(0));
            }
            typeCount.get(typeQueryName).incrementAndGet();
        }
        for (Entry<String, AtomicInteger> en : typeCount.entrySet()) {
            String typeQueryName = en.getKey();
            if (en.getValue().get() == 1) {
                // for types used once, allow direct type reference
                qualifierToType.put(typeQueryName, typeQueryName);
            } else {
                // ambiguous type, not legal as qualifier
                canonicalQualifier.remove(typeQueryName);
            }
        }
        // if only one type, allow omitted qualifier (null)
        if (types.size() == 1) {
            String typeQueryName = types.values().iterator().next();
            qualifierToType.put(null, typeQueryName);
            // correlation name is actually null for all qualifiers
            for (String qual : qualifierToType.keySet()) {
                canonicalQualifier.put(qual, null);
            }
        }
    }

    /**
     * Finds a database column from a CMIS reference.
     */
    protected Column getColumn(ColumnReference col) {
        String qual = canonicalQualifier.get(col.getQualifier());
        String id = col.getPropertyId();
        Column column;
        if (id.startsWith(CMIS_PREFIX) || id.startsWith(NX_PREFIX)) {
            column = getSystemColumn(qual, id);
        } else {
            ModelProperty propertyInfo = model.getPropertyInfo(id);
            boolean multi = propertyInfo.propertyType.isArray();
            Table table = database.getTable(propertyInfo.fragmentName);
            String key = multi ? Model.COLL_TABLE_VALUE_KEY : propertyInfo.fragmentKey;
            column = getTable(table, qual).getColumn(key);
        }
        return column;
    }

    protected Column getSystemColumn(String qual, String id) {
        Column column = getSystemColumn(id);
        if (column != null && qual != null) {
            // alias table according to qualifier
            Table table = column.getTable();
            column = getTable(table, qual).getColumn(column.getKey());
            // TODO ensure key == name, or add getName()
        }
        return column;
    }

    protected Column getSystemColumn(String id) {
        if (id.equals(PropertyIds.OBJECT_ID)) {
            return hierTable.getColumn(Model.MAIN_KEY);
        }
        if (id.equals(PropertyIds.PARENT_ID)) {
            return hierTable.getColumn(Model.HIER_PARENT_KEY);
        }
        if (id.equals(NuxeoTypeHelper.NX_PARENT_ID)) {
            return hierTable.getColumn(Model.HIER_PARENT_KEY);
        }
        if (id.equals(NuxeoTypeHelper.NX_PATH_SEGMENT)) {
            return hierTable.getColumn(Model.HIER_CHILD_NAME_KEY);
        }
        if (id.equals(NuxeoTypeHelper.NX_POS)) {
            return hierTable.getColumn(Model.HIER_CHILD_POS_KEY);
        }
        if (id.equals(PropertyIds.OBJECT_TYPE_ID)) {
            // joinedHierTable
            return hierTable.getColumn(Model.MAIN_PRIMARY_TYPE_KEY);
        }
        if (id.equals(PropertyIds.VERSION_LABEL)) {
            return database.getTable(Model.VERSION_TABLE_NAME).getColumn(Model.VERSION_LABEL_KEY);
        }
        if (id.equals(PropertyIds.IS_LATEST_MAJOR_VERSION)) {
            return database.getTable(Model.VERSION_TABLE_NAME).getColumn(Model.VERSION_IS_LATEST_MAJOR_KEY);
        }
        if (id.equals(PropertyIds.IS_LATEST_VERSION)) {
            return database.getTable(Model.VERSION_TABLE_NAME).getColumn(Model.VERSION_IS_LATEST_KEY);
        }
        if (id.equals(NuxeoTypeHelper.NX_ISVERSION)) {
            return database.getTable(Model.HIER_TABLE_NAME).getColumn(Model.MAIN_IS_VERSION_KEY);
        }
        if (id.equals(NuxeoTypeHelper.NX_ISCHECKEDIN)) {
            return database.getTable(Model.HIER_TABLE_NAME).getColumn(Model.MAIN_CHECKED_IN_KEY);
        }
        if (id.equals(NuxeoTypeHelper.NX_ISTRASHED)) {
            return database.getTable(Model.HIER_TABLE_NAME).getColumn(Model.MAIN_IS_TRASHED_KEY);
        }
        if (id.equals(NuxeoTypeHelper.NX_LIFECYCLE_STATE)) {
            ModelProperty propertyInfo = model.getPropertyInfo(Model.MISC_LIFECYCLE_STATE_PROP);
            return database.getTable(propertyInfo.fragmentName).getColumn(propertyInfo.fragmentKey);
        }
        if (id.equals(PropertyIds.NAME)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_TITLE_KEY);
        }
        if (id.equals(PropertyIds.DESCRIPTION)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_DESCRIPTION_KEY);
        }
        if (id.equals(PropertyIds.CREATED_BY)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_CREATOR_KEY);
        }
        if (id.equals(PropertyIds.CREATION_DATE)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_CREATED_KEY);
        }
        if (id.equals(PropertyIds.LAST_MODIFICATION_DATE)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_MODIFIED_KEY);
        }
        if (id.equals(PropertyIds.LAST_MODIFIED_BY)) {
            return database.getTable(DC_FRAGMENT_NAME).getColumn(DC_LAST_CONTRIBUTOR_KEY);
        }
        if (id.equals(PropertyIds.SOURCE_ID)) {
            return database.getTable(REL_FRAGMENT_NAME).getColumn(REL_SOURCE_KEY);
        }
        if (id.equals(PropertyIds.TARGET_ID)) {
            return database.getTable(REL_FRAGMENT_NAME).getColumn(REL_TARGET_KEY);
        }
        return null;
    }

    /** Get key to use in data returned to high-level caller. */
    protected static String getColumnKey(ColumnReference col) {
        String alias = col.getAliasName();
        if (alias != null) {
            return alias;
        }
        return getPropertyKey(col.getQualifier(), col.getPropertyQueryName());
    }

    protected static String getPropertyKey(String qual, String id) {
        if (qual == null) {
            return id;
        }
        return qual + '.' + id;
    }

    protected TypeDefinition getTypeForQualifier(String qual) {
        String typeQueryName = qualifierToType.get(qual);
        return query.getTypeDefinitionFromQueryName(typeQueryName);
    }

    protected Table getTable(Table table, String qual) {
        if (qual == null) {
            return table;
        } else {
            return new TableAlias(table, getTableAlias(table, qual));
        }
    }

    protected String getTableAlias(Table table, String qual) {
        return "_" + qual + "_" + table.getPhysicalName();
    }

    /**
     * Map maker that can deal with aliased column names and computed values.
     */
    // static to avoid keeping the whole QueryMaker in the returned object
    public static class CMISQLMapMaker implements MapMaker {

        protected List<SqlColumn> realColumns;

        protected Map<String, ColumnReference> virtualColumns;

        protected NuxeoCmisService service;

        public CMISQLMapMaker(List<SqlColumn> realColumns, Map<String, ColumnReference> virtualColumns,
                NuxeoCmisService service) {
            this.realColumns = realColumns;
            this.virtualColumns = virtualColumns;
            this.service = service;
        }

        @Override
        public Map<String, Serializable> makeMap(ResultSet rs) throws SQLException {
            Map<String, Serializable> map = new HashMap<>();

            // get values from result set
            int i = 1;
            for (SqlColumn rc : realColumns) {
                Serializable value = rc.column.getFromResultSet(rs, i++);
                String key = rc.column.getKey();
                // type conversion to CMIS values
                if (value instanceof Long) {
                    value = BigInteger.valueOf(((Long) value).longValue());
                } else if (value instanceof Integer) {
                    value = BigInteger.valueOf(((Integer) value).intValue());
                } else if (value instanceof Double) {
                    value = BigDecimal.valueOf(((Double) value).doubleValue());
                } else if (value == null) {
                    // special handling of some columns where NULL means FALSE
                    String column = rc.column.getTable().getRealTable().getKey() + " " + key;
                    if (NULL_IS_FALSE_COLUMNS.contains(column)) {
                        value = Boolean.FALSE;
                    }
                }
                if (Model.MAIN_KEY.equals(key) || Model.HIER_PARENT_KEY.equals(key)) {
                    value = String.valueOf(value); // idToString
                }
                map.put(rc.key, value);
            }

            // virtual values
            // map to store actual data for each qualifier
            TypeManagerImpl typeManager = service.getTypeManager();
            Map<String, NuxeoObjectData> datas = null;
            for (Entry<String, ColumnReference> vc : virtualColumns.entrySet()) {
                String key = vc.getKey();
                ColumnReference col = vc.getValue();
                String qual = col.getQualifier();
                if (col.getPropertyId().equals(PropertyIds.BASE_TYPE_ID)) {
                    // special case, no need to get full Nuxeo Document
                    String typeId = (String) map.get(getPropertyKey(qual, PropertyIds.OBJECT_TYPE_ID));
                    TypeDefinitionContainer type = typeManager.getTypeById(typeId);
                    String baseTypeId = type.getTypeDefinition().getBaseTypeId().value();
                    map.put(key, baseTypeId);
                    continue;
                }
                if (datas == null) {
                    datas = new HashMap<>(2);
                }
                NuxeoObjectData data = datas.get(qual);
                if (data == null) {
                    // find main id for this qualifier in the result set
                    // (main id always included in joins)
                    // TODO check what happens if cmis:objectId is aliased
                    String id = (String) map.get(getPropertyKey(qual, PropertyIds.OBJECT_ID));
                    try {
                        // reentrant call to the same session, but the MapMaker
                        // is only called from the IterableQueryResult in
                        // queryAndFetch which manipulates no session state
                        // TODO constructing the DocumentModel (in
                        // NuxeoObjectData) is expensive, try to get value
                        // directly
                        data = (NuxeoObjectData) service.getObject(service.getNuxeoRepository().getId(), id, null, null,
                                null, null, null, null, null);
                    } catch (CmisRuntimeException e) {
                        log.error("Cannot get document: " + id, e);
                    }
                    datas.put(qual, data);
                }
                Serializable v;
                if (data == null) {
                    // could not fetch
                    v = null;
                } else {
                    NuxeoPropertyDataBase<?> pd = (NuxeoPropertyDataBase<?>) data.getProperty(col.getPropertyId());
                    if (pd == null) {
                        v = null;
                    } else {
                        if (pd.getPropertyDefinition().getCardinality() == Cardinality.SINGLE) {
                            v = (Serializable) pd.getFirstValue();
                        } else {
                            v = (Serializable) pd.getValues();
                        }
                    }
                }
                map.put(key, v);
            }

            return map;
        }
    }

    /**
     * Walker of the WHERE clause to gather fulltext info.
     */
    public class AnalyzingWalker extends AbstractPredicateWalker {

        public static final String NX_FULLTEXT_INDEX_PREFIX = "nx:";

        public boolean hasContains;

        @Override
        public Boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
            if (hasContains && Framework.getService(ConfigurationService.class)
                                        .isBooleanPropertyFalse(NuxeoRepository.RELAX_CMIS_SPEC)) {
                throw new QueryParseException("At most one CONTAINS() is allowed");
            }
            hasContains = true;

            String qual = qualNode == null ? null : qualNode.getText();
            qual = canonicalQualifier.get(qual);
            Column column = getSystemColumn(qual, PropertyIds.OBJECT_ID);
            String statement = (String) super.walkString(queryNode);
            String indexName = Model.FULLTEXT_DEFAULT_INDEX;

            // micro parsing of the fulltext statement to perform fulltext
            // search on a non default index
            if (statement.startsWith(NX_FULLTEXT_INDEX_PREFIX)) {
                statement = statement.substring(NX_FULLTEXT_INDEX_PREFIX.length());
                int firstColumnIdx = statement.indexOf(':');
                if (firstColumnIdx > 0 && firstColumnIdx < statement.length() - 1) {
                    String requestedIndexName = statement.substring(0, firstColumnIdx);
                    statement = statement.substring(firstColumnIdx + 1);
                    if (model.getFulltextConfiguration().indexNames.contains(requestedIndexName)) {
                        indexName = requestedIndexName;
                    } else {
                        throw new QueryParseException("No such fulltext index: " + requestedIndexName);
                    }
                } else {
                    log.warn(String.format("fail to microparse custom fulltext index:" + " fallback to '%s'",
                            indexName));
                }
            }
            // CMIS syntax to our internal google-like internal syntax
            statement = cmisToFulltextQuery(statement);
            // internal syntax to backend syntax
            statement = dialect.getDialectFulltextQuery(statement);
            fulltextMatchInfo = dialect.getFulltextScoredMatchInfo(statement, indexName, 1, column, model, database);
            return null;
        }
    }

    protected static String cmisToFulltextQuery(String statement) {
        // internal syntax has implicit AND
        statement = statement.replace(" and ", " ");
        statement = statement.replace(" AND ", " ");
        return statement;
    }

    /**
     * Walker of the WHERE clause that generates final SQL.
     */
    public class GeneratingWalker extends AbstractPredicateWalker {

        public StringBuilder whereBuf = new StringBuilder();

        public LinkedList<Serializable> whereBufParams = new LinkedList<Serializable>();

        /** joins added by fulltext match */
        public final List<org.nuxeo.ecm.core.storage.sql.jdbc.db.Join> ftJoins = new LinkedList<org.nuxeo.ecm.core.storage.sql.jdbc.db.Join>();

        @Override
        public Boolean walkNot(Tree opNode, Tree node) {
            whereBuf.append("NOT ");
            walkPredicate(node);
            return null;
        }

        @Override
        public Boolean walkAnd(Tree opNode, Tree leftNode, Tree rightNode) {
            whereBuf.append("(");
            walkPredicate(leftNode);
            whereBuf.append(" AND ");
            walkPredicate(rightNode);
            whereBuf.append(")");
            return null;
        }

        @Override
        public Boolean walkOr(Tree opNode, Tree leftNode, Tree rightNode) {
            whereBuf.append("(");
            walkPredicate(leftNode);
            whereBuf.append(" OR ");
            walkPredicate(rightNode);
            whereBuf.append(")");
            return null;
        }

        @Override
        public Boolean walkEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            if (isFacetsColumn(leftNode.getText())) {
                walkFacets(opNode, leftNode, rightNode);
                return null;
            }
            if (leftNode.getType() == CmisQlStrictLexer.COL && rightNode.getType() == CmisQlStrictLexer.BOOL_LIT
                    && !Boolean.parseBoolean(rightNode.getText())) {
                // special handling of the " = false" case for column where
                // NULL means false
                walkIsNullOrFalse(leftNode);
                return null;
            }
            // normal case
            walkExpr(leftNode);
            whereBuf.append(" = ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkNotEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            if (leftNode.getType() == CmisQlStrictLexer.COL && rightNode.getType() == CmisQlStrictLexer.BOOL_LIT
                    && Boolean.parseBoolean(rightNode.getText())) {
                // special handling of the " <> true" case for column where
                // NULL means false
                walkIsNullOrFalse(leftNode);
                return null;
            }
            walkExpr(leftNode);
            whereBuf.append(" <> ");
            walkExpr(rightNode);
            return null;
        }

        protected void walkIsNullOrFalse(Tree leftNode) {
            Column c = resolveColumn(leftNode);
            String columnSpec = c.getTable().getRealTable().getKey() + " " + c.getKey();
            if (NULL_IS_FALSE_COLUMNS.contains(columnSpec)) {
                // treat NULL and FALSE as equivalent
                whereBuf.append("(");
                walkExpr(leftNode);
                whereBuf.append(" IS NULL OR ");
                walkExpr(leftNode);
                whereBuf.append(" = ?)");
                whereBufParams.add(Boolean.FALSE);
            } else {
                // explicit false equality test
                walkExpr(leftNode);
                whereBuf.append(" = ?");
                whereBufParams.add(Boolean.FALSE);
            }
        }

        @Override
        public Boolean walkGreaterThan(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" > ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkGreaterOrEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" >= ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkLessThan(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" < ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkLessOrEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            whereBuf.append(" <= ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            whereBuf.append(" IN ");
            walkExpr(listNode);
            return null;
        }

        @Override
        public Boolean walkNotIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            whereBuf.append(" NOT IN ");
            walkExpr(listNode);
            return null;
        }

        @Override
        public Boolean walkInAny(Tree opNode, Tree colNode, Tree listNode) {
            if (isFacetsColumn(resolveColumnReference(colNode).getName())) {
                walkFacets(opNode, colNode, listNode);
                return null;
            }
            walkAny(colNode, "IN", listNode);
            return null;
        }

        @Override
        public Boolean walkNotInAny(Tree opNode, Tree colNode, Tree listNode) {
            if (isFacetsColumn(resolveColumnReference(colNode).getName())) {
                walkFacets(opNode, colNode, listNode);
                return null;
            }
            walkAny(colNode, "NOT IN", listNode);
            return null;
        }

        @Override
        public Boolean walkEqAny(Tree opNode, Tree literalNode, Tree colNode) {
            if (isFacetsColumn(resolveColumnReference(colNode).getName())) {
                walkFacets(opNode, colNode, literalNode);
                return null;
            }
            // note that argument order is reversed
            walkAny(colNode, "=", literalNode);
            return null;
        }

        protected void walkAny(Tree colNode, String op, Tree exprNode) {
            int token = ((Tree) colNode).getTokenStartIndex();
            ColumnReference col = (ColumnReference) query.getColumnReference(Integer.valueOf(token));
            PropertyDefinition<?> pd = col.getPropertyDefinition();
            if (pd.getCardinality() != Cardinality.MULTI) {
                throw new QueryParseException(
                        "Cannot use " + op + " ANY with single-valued property: " + col.getPropertyQueryName());
            }
            Column column = (Column) col.getInfo();
            String qual = canonicalQualifier.get(col.getQualifier());
            // we need the real table and column in the subquery
            Table realTable = column.getTable().getRealTable();
            Column realColumn = realTable.getColumn(column.getKey());
            Column hierMainColumn = getTable(hierTable, qual).getColumn(Model.MAIN_KEY);
            Column multiMainColumn = realTable.getColumn(Model.MAIN_KEY);

            whereBuf.append("EXISTS (SELECT 1 FROM ");
            whereBuf.append(realTable.getQuotedName());
            whereBuf.append(" WHERE ");
            whereBuf.append(hierMainColumn.getFullQuotedName());
            whereBuf.append(" = ");
            whereBuf.append(multiMainColumn.getFullQuotedName());
            whereBuf.append(" AND ");
            whereBuf.append(realColumn.getFullQuotedName());
            whereBuf.append(" ");
            whereBuf.append(op);
            whereBuf.append(" ");
            walkExpr(exprNode);
            whereBuf.append(")");
        }

        @Override
        public Boolean walkIsNull(Tree opNode, Tree colNode) {
            return walkIsNullOrIsNotNull(colNode, true);
        }

        @Override
        public Boolean walkIsNotNull(Tree opNode, Tree colNode) {
            return walkIsNullOrIsNotNull(colNode, false);
        }

        protected Boolean walkIsNullOrIsNotNull(Tree colNode, boolean isNull) {
            int token = ((Tree) colNode).getTokenStartIndex();
            ColumnReference col = (ColumnReference) query.getColumnReference(Integer.valueOf(token));
            PropertyDefinition<?> pd = col.getPropertyDefinition();
            boolean multi = pd.getCardinality() == Cardinality.MULTI;
            if (multi) {
                // we need the real table and column in the subquery
                Column column = (Column) col.getInfo();
                String qual = canonicalQualifier.get(col.getQualifier());
                Table realTable = column.getTable().getRealTable();
                Column hierMainColumn = getTable(hierTable, qual).getColumn(Model.MAIN_KEY);
                Column multiMainColumn = realTable.getColumn(Model.MAIN_KEY);
                if (isNull) {
                    whereBuf.append("NOT ");
                }
                whereBuf.append("EXISTS (SELECT 1 FROM ");
                whereBuf.append(realTable.getQuotedName());
                whereBuf.append(" WHERE ");
                whereBuf.append(hierMainColumn.getFullQuotedName());
                whereBuf.append(" = ");
                whereBuf.append(multiMainColumn.getFullQuotedName());
                whereBuf.append(')');
            } else {
                walkExpr(colNode);
                whereBuf.append(isNull ? " IS NULL" : " IS NOT NULL");
            }
            return null;
        }

        @Override
        public Boolean walkLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            whereBuf.append(" LIKE ");
            walkExpr(stringNode);
            return null;
        }

        @Override
        public Boolean walkNotLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            whereBuf.append(" NOT LIKE ");
            walkExpr(stringNode);
            return null;
        }

        @Override
        public Boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
            if (fulltextMatchInfo.joins != null) {
                ftJoins.addAll(fulltextMatchInfo.joins);
            }
            whereBuf.append(fulltextMatchInfo.whereExpr);
            if (fulltextMatchInfo.whereExprParam != null) {
                whereBufParams.add(fulltextMatchInfo.whereExprParam);
            }
            return null;
        }

        @Override
        public Boolean walkInFolder(Tree opNode, Tree qualNode, Tree paramNode) {
            String qual = qualNode == null ? null : qualNode.getText();
            qual = canonicalQualifier.get(qual);
            // this is from the hierarchy table which is always present
            Column column = getSystemColumn(qual, PropertyIds.PARENT_ID);
            whereBuf.append(column.getFullQuotedName());
            whereBuf.append(" = ?");
            String id = (String) super.walkString(paramNode);
            whereBufParams.add(model.idFromString(id));
            return null;
        }

        @Override
        public Boolean walkInTree(Tree opNode, Tree qualNode, Tree paramNode) {
            String qual = qualNode == null ? null : qualNode.getText();
            qual = canonicalQualifier.get(qual);
            // this is from the hierarchy table which is always present
            Column column = getSystemColumn(qual, PropertyIds.OBJECT_ID);
            String id = (String) super.walkString(paramNode);
            String sql = dialect.getInTreeSql(column.getFullQuotedName(), id);
            if (sql == null) {
                whereBuf.append("0=1");
            } else {
                whereBuf.append(sql);
                whereBufParams.add(model.idFromString(id));
            }
            return null;
        }

        @Override
        public Object walkList(Tree node) {
            whereBuf.append("(");
            for (int i = 0; i < node.getChildCount(); i++) {
                if (i != 0) {
                    whereBuf.append(", ");
                }
                Tree child = node.getChild(i);
                walkExpr(child);
            }
            whereBuf.append(")");
            return null;
        }

        @Override
        public Object walkBoolean(Tree node) {
            Serializable value = (Serializable) super.walkBoolean(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkNumber(Tree node) {
            Serializable value = (Serializable) super.walkNumber(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkString(Tree node) {
            Serializable value = (Serializable) super.walkString(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkTimestamp(Tree node) {
            Serializable value = (Serializable) super.walkTimestamp(node);
            whereBuf.append("?");
            whereBufParams.add(value);
            return null;
        }

        @Override
        public Object walkCol(Tree node) {
            whereBuf.append(resolveColumn(node).getFullQuotedName());
            return null;
        }

        public ColumnReference resolveColumnReference(Tree node) {
            int token = node.getTokenStartIndex();
            CmisSelector sel = query.getColumnReference(Integer.valueOf(token));
            if (sel instanceof ColumnReference) {
                return (ColumnReference) sel;
            } else {
                throw new QueryParseException("Cannot use column in WHERE clause: " + sel.getName());
            }
        }

        public Column resolveColumn(Tree node) {
            return (Column) resolveColumnReference(node).getInfo();
        }

        protected void walkFacets(Tree opNode, Tree colNodel, Tree literalNode) {
            boolean include;
            Set<String> mixins;

            int opType = opNode.getType();
            if (opType == CmisQlStrictLexer.EQ_ANY) {
                include = true;
                if (literalNode.getType() != CmisQlStrictLexer.STRING_LIT) {
                    throw new QueryParseException(colNodel.getText() + " = requires literal string as right argument");
                }
                String value = super.walkString(literalNode).toString();
                mixins = Collections.singleton(value);
            } else if (opType == CmisQlStrictLexer.IN_ANY || opType == CmisQlStrictLexer.NOT_IN_ANY) {
                include = opType == CmisQlStrictLexer.IN_ANY;
                mixins = new TreeSet<>();
                for (int i = 0; i < literalNode.getChildCount(); i++) {
                    mixins.add(super.walkString(literalNode.getChild(i)).toString());
                }
            } else {
                throw new QueryParseException(colNodel.getText() + " unsupported operator: " + opNode.getText());
            }

            /*
             * Primary types - static mixins
             */
            Set<String> types;
            if (include) {
                types = new HashSet<>();
                for (String mixin : mixins) {
                    types.addAll(model.getMixinDocumentTypes(mixin));
                }
            } else {
                types = new HashSet<>(model.getDocumentTypes());
                for (String mixin : mixins) {
                    types.removeAll(model.getMixinDocumentTypes(mixin));
                }
            }

            /*
             * Instance mixins
             */
            Set<String> instanceMixins = new HashSet<>();
            for (String mixin : mixins) {
                if (!MIXINS_NOT_PER_INSTANCE.contains(mixin)) {
                    instanceMixins.add(mixin);
                }
            }

            /*
             * SQL generation
             */

            ColumnReference facetsCol = resolveColumnReference(colNodel);
            String qual = canonicalQualifier.get(facetsCol.getQualifier());
            Table table = getTable(hierTable, qual);

            if (!types.isEmpty()) {
                Column col = table.getColumn(Model.MAIN_PRIMARY_TYPE_KEY);
                whereBuf.append(col.getFullQuotedName());
                whereBuf.append(" IN ");
                whereBuf.append('(');
                for (Iterator<String> it = types.iterator(); it.hasNext();) {
                    whereBuf.append('?');
                    whereBufParams.add(it.next());
                    if (it.hasNext()) {
                        whereBuf.append(", ");
                    }
                }
                whereBuf.append(')');

                if (!instanceMixins.isEmpty()) {
                    whereBuf.append(include ? " OR " : " AND ");
                }
            }

            if (!instanceMixins.isEmpty()) {
                whereBuf.append('(');
                Column mixinsColumn = table.getColumn(Model.MAIN_MIXIN_TYPES_KEY);
                String[] returnParam = new String[1];
                for (Iterator<String> it = instanceMixins.iterator(); it.hasNext();) {
                    String mixin = it.next();
                    String sql = dialect.getMatchMixinType(mixinsColumn, mixin, include, returnParam);
                    whereBuf.append(sql);
                    if (returnParam[0] != null) {
                        whereBufParams.add(returnParam[0]);
                    }
                    if (it.hasNext()) {
                        whereBuf.append(include ? " OR " : " AND ");
                    }
                }
                if (!include) {
                    whereBuf.append(" OR ");
                    whereBuf.append(mixinsColumn.getFullQuotedName());
                    whereBuf.append(" IS NULL");
                }
                whereBuf.append(')');
            }

            if (types.isEmpty() && instanceMixins.isEmpty()) {
                whereBuf.append(include ? "0=1" : "0=0");
            }
        }
    }
}

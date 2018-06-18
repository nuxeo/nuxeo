/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.chemistry.opencmis.commons.enums.BaseTypeId.CMIS_DOCUMENT;
import static org.apache.chemistry.opencmis.commons.enums.BaseTypeId.CMIS_RELATIONSHIP;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.server.support.query.AbstractPredicateWalker;
import org.apache.chemistry.opencmis.server.support.query.CmisSelector;
import org.apache.chemistry.opencmis.server.support.query.ColumnReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference.CmisQlFunction;
import org.apache.chemistry.opencmis.server.support.query.QueryObject;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.SortSpec;
import org.apache.chemistry.opencmis.server.support.query.QueryUtilStrict;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.opencmis.impl.util.TypeManagerImpl;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Transformer of CMISQL queries into NXQL queries.
 */
public class CMISQLtoNXQL {

    private static final Log log = LogFactory.getLog(CMISQLtoNXQL.class);

    protected static final String CMIS_PREFIX = "cmis:";

    protected static final String NX_PREFIX = "nuxeo:";

    protected static final String NXQL_DOCUMENT = "Document";

    protected static final String NXQL_RELATION = "Relation";

    protected static final String NXQL_DC_TITLE = "dc:title";

    protected static final String NXQL_DC_DESCRIPTION = "dc:description";

    protected static final String NXQL_DC_CREATOR = "dc:creator";

    protected static final String NXQL_DC_CREATED = "dc:created";

    protected static final String NXQL_DC_MODIFIED = "dc:modified";

    protected static final String NXQL_DC_LAST_CONTRIBUTOR = "dc:lastContributor";

    protected static final String NXQL_REL_SOURCE = "relation:source";

    protected static final String NXQL_REL_TARGET = "relation:target";

    protected static final DateTimeFormatter ISO_DATE_TIME_FORMAT = ISODateTimeFormat.dateTime();

    private static final char QUOTE = '\'';

    private static final String SPACE_ASC = " asc";

    private static final String SPACE_DESC = " desc";

    // list of SQL column where NULL (missing value) should be treated as
    // Boolean.FALSE in a query result
    protected static final Set<String> NULL_IS_FALSE_COLUMNS = new HashSet<>(Arrays.asList(NXQL.ECM_ISVERSION,
            NXQL.ECM_ISLATESTVERSION, NXQL.ECM_ISLATESTMAJORVERSION, NXQL.ECM_ISCHECKEDIN));

    protected final boolean supportsProxies;

    protected Map<String, PropertyDefinition<?>> typeInfo;

    protected CoreSession coreSession;

    // ----- filled during walks of the clauses -----

    protected QueryUtilStrict queryUtil;

    protected QueryObject query;

    protected TypeDefinition fromType;

    protected boolean skipDeleted = true;

    // ----- passed to IterableQueryResult -----

    /** The real columns, CMIS name mapped to NXQL. */
    protected Map<String, String> realColumns = new LinkedHashMap<>();

    /** The non-real-columns we'll return as well. */
    protected Map<String, ColumnReference> virtualColumns = new LinkedHashMap<>();

    public CMISQLtoNXQL(boolean supportsProxies) {
        this.supportsProxies = supportsProxies;
    }

    /**
     * Gets the NXQL from a CMISQL query.
     */
    public String getNXQL(String cmisql, NuxeoCmisService service, Map<String, PropertyDefinition<?>> typeInfo,
            boolean searchAllVersions) throws QueryParseException {
        this.typeInfo = typeInfo;
        boolean searchLatestVersion = !searchAllVersions;
        TypeManagerImpl typeManager = service.getTypeManager();
        coreSession = service.coreSession;

        try {
            queryUtil = new QueryUtilStrict(cmisql, typeManager, new AnalyzingWalker(), false);
            queryUtil.processStatement();
            query = queryUtil.getQueryObject();
        } catch (RecognitionException e) {
            throw new QueryParseException(queryUtil.getErrorMessage(e), e);
        }
        if (query.getTypes().size() != 1 && query.getJoinedSecondaryTypes() == null) {
            throw new QueryParseException("JOINs not supported in query: " + cmisql);
        }

        fromType = query.getMainFromName();
        BaseTypeId fromBaseTypeId = fromType.getBaseTypeId();

        // now resolve column selectors to actual database columns
        for (CmisSelector sel : query.getSelectReferences()) {
            recordSelectSelector(sel);
        }
        for (CmisSelector sel : query.getJoinReferences()) {
            ColumnReference col = ((ColumnReference) sel);
            if (col.getTypeDefinition().getBaseTypeId() == BaseTypeId.CMIS_SECONDARY) {
                // ignore reference to ON FACET.cmis:objectId
                continue;
            }
            recordSelector(sel, JOIN);
        }
        for (CmisSelector sel : query.getWhereReferences()) {
            recordSelector(sel, WHERE);
        }
        for (SortSpec spec : query.getOrderBys()) {
            recordSelector(spec.getSelector(), ORDER_BY);
        }

        addSystemColumns();

        List<String> whereClauses = new ArrayList<>();

        // what to select (result columns)

        String what = StringUtils.join(realColumns.values(), ", ");

        // determine relevant primary types

        String nxqlFrom;
        if (fromBaseTypeId == CMIS_RELATIONSHIP) {
            if (fromType.getParentTypeId() == null) {
                nxqlFrom = NXQL_RELATION;
            } else {
                nxqlFrom = fromType.getId();
            }
        } else {
            nxqlFrom = NXQL_DOCUMENT;
            List<String> types = new ArrayList<>();
            if (fromType.getParentTypeId() != null) {
                // don't add abstract root types
                types.add(fromType.getId());
            }
            LinkedList<TypeDefinitionContainer> typesTodo = new LinkedList<>();
            typesTodo.addAll(typeManager.getTypeDescendants(fromType.getId(), -1, Boolean.TRUE));
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
            StringBuilder pt = new StringBuilder();
            pt.append(NXQL.ECM_PRIMARYTYPE);
            pt.append(" IN (");
            for (Iterator<String> it = types.iterator(); it.hasNext();) {
                pt.append(QUOTE);
                pt.append(it.next());
                pt.append(QUOTE);
                if (it.hasNext()) {
                    pt.append(", ");
                }
            }
            pt.append(")");
            whereClauses.add(pt.toString());
        }

        // lifecycle not deleted filter

        if (skipDeleted) {
            whereClauses.add(String.format("%s = 0", NXQL.ECM_ISTRASHED));
        }

        // searchAllVersions filter

        if (searchLatestVersion && fromBaseTypeId == CMIS_DOCUMENT) {
            whereClauses.add(String.format("%s = 1", NXQL.ECM_ISLATESTVERSION));
        }

        // no proxies

        if (!supportsProxies) {
            whereClauses.add(NXQL.ECM_ISPROXY + " = 0");
        }

        // WHERE clause

        Tree whereNode = queryUtil.getWalker().getWherePredicateTree();
        if (whereNode != null) {
            GeneratingWalker generator = new GeneratingWalker();
            generator.walkPredicate(whereNode);
            whereClauses.add(generator.buf.toString());
        }

        // ORDER BY clause

        List<String> orderbys = new ArrayList<>();
        for (SortSpec spec : query.getOrderBys()) {
            String orderby;
            CmisSelector sel = spec.getSelector();
            if (sel instanceof ColumnReference) {
                orderby = (String) sel.getInfo();
            } else {
                orderby = NXQL.ECM_FULLTEXT_SCORE;
            }
            if (!spec.ascending) {
                orderby += " DESC";
            }
            orderbys.add(orderby);
        }

        // create the whole select

        String where = StringUtils.join(whereClauses, " AND ");
        String nxql = "SELECT " + what + " FROM " + nxqlFrom + " WHERE " + where;
        if (!orderbys.isEmpty()) {
            nxql += " ORDER BY " + StringUtils.join(orderbys, ", ");
        }
        // System.err.println("CMIS: " + statement);
        // System.err.println("NXQL: " + nxql);
        return nxql;
    }

    public IterableQueryResult getIterableQueryResult(IterableQueryResult it, NuxeoCmisService service) {
        return new NXQLtoCMISIterableQueryResult(it, realColumns, virtualColumns, service);
    }

    public PartialList<Map<String, Serializable>> convertToCMIS(PartialList<Map<String, Serializable>> pl,
            NuxeoCmisService service) {
        return pl.stream().map(map -> convertToCMISMap(map, realColumns, virtualColumns, service)).collect(
                Collectors.collectingAndThen(Collectors.toList(), result -> new PartialList<>(result, pl.totalSize())));
    }

    protected boolean isFacetsColumn(String name) {
        return PropertyIds.SECONDARY_OBJECT_TYPE_IDS.equals(name) || NuxeoTypeHelper.NX_FACETS.equals(name);
    }

    protected void addSystemColumns() {
        // additional references to cmis:objectId and cmis:objectTypeId
        for (String propertyId : Arrays.asList(PropertyIds.OBJECT_ID, PropertyIds.OBJECT_TYPE_ID)) {
            if (!realColumns.containsKey(propertyId)) {
                ColumnReference col = new ColumnReference(propertyId);
                col.setTypeDefinition(propertyId, fromType);
                recordSelectSelector(col);
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
            realColumns.put(key, NXQL.ECM_FULLTEXT_SCORE);
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

            if (col.getPropertyQueryName().equals("*")) {
                for (PropertyDefinition<?> pd : fromType.getPropertyDefinitions().values()) {
                    String id = pd.getId();
                    if ((pd.getCardinality() == Cardinality.SINGLE //
                            && Boolean.TRUE.equals(pd.isQueryable())) //
                            || id.equals(PropertyIds.BASE_TYPE_ID)) {
                        ColumnReference c = new ColumnReference(null, id);
                        c.setTypeDefinition(id, fromType);
                        recordSelectSelector(c);
                    }
                }
                return;
            }

            String key = col.getPropertyQueryName();
            PropertyDefinition<?> pd = col.getPropertyDefinition();
            String nxqlCol = getColumn(col);
            String id = pd.getId();
            if (nxqlCol != null && pd.getCardinality() == Cardinality.SINGLE && (Boolean.TRUE.equals(pd.isQueryable())
                    || id.equals(PropertyIds.BASE_TYPE_ID) || id.equals(PropertyIds.OBJECT_TYPE_ID))) {
                col.setInfo(nxqlCol);
                realColumns.put(key, nxqlCol);
            } else {
                virtualColumns.put(key, col);
            }
            if (typeInfo != null) {
                typeInfo.put(key, pd);
            }
        }
    }

    protected static final String JOIN = "JOIN";

    protected static final String WHERE = "WHERE";

    protected static final String ORDER_BY = "ORDER BY";

    /**
     * Records a JOIN / WHERE / ORDER BY selector, and associates it to a database column.
     */
    protected void recordSelector(CmisSelector sel, String clauseType) {
        if (sel instanceof FunctionReference) {
            FunctionReference fr = (FunctionReference) sel;
            if (clauseType != ORDER_BY) { // == ok
                throw new QueryParseException("Cannot use function in " + clauseType + " clause: " + fr.getFunction());
            }
            // ORDER BY SCORE, nothing further to record
            return;
        }
        ColumnReference col = (ColumnReference) sel;

        // fetch column and associate it to the selector
        String column = getColumn(col);
        if (!isFacetsColumn(col.getPropertyId()) && column == null) {
            throw new QueryParseException(
                    "Cannot use column in " + clauseType + " clause: " + col.getPropertyQueryName());
        }
        col.setInfo(column);

        if (clauseType == WHERE && NuxeoTypeHelper.NX_LIFECYCLE_STATE.equals(col.getPropertyId())
                && Framework.getService(TrashService.class).hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            // explicit lifecycle query: do not include the 'deleted' lifecycle filter
            skipDeleted = false;
        }
        if (clauseType == WHERE && NuxeoTypeHelper.NX_ISTRASHED.equals(col.getPropertyId())) {
            // explicit trashed query: do not include the `isTrashed = 0` filter
            skipDeleted = false;
        }
    }

    /**
     * Finds a NXQL column from a CMIS reference.
     */
    protected String getColumn(ColumnReference col) {
        return getColumn(col.getPropertyId());
    }

    /**
     * Finds a NXQL column from a CMIS reference.
     */
    protected String getColumn(String propertyId) {
        if (propertyId.startsWith(CMIS_PREFIX) || propertyId.startsWith(NX_PREFIX)) {
            return getSystemColumn(propertyId);
        } else {
            if (propertyId.indexOf(':') == -1) {
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);

                for (Schema schema : schemaManager.getSchemas()) {
                    if (!schema.getNamespace().hasPrefix()) {
                        // schema without prefix, try it
                        if (schema.hasField(propertyId)) {
                            propertyId = schema.getName() + ":" + propertyId;
                            break;
                        }
                    }
                }
            }
            // CMIS property names are identical to NXQL ones for non-system properties
            return propertyId;
        }
    }

    /**
     * Finds a NXQL system column from a CMIS system property id.
     */
    protected String getSystemColumn(String propertyId) {
        switch (propertyId) {
        case PropertyIds.OBJECT_ID:
            return NXQL.ECM_UUID;
        case PropertyIds.PARENT_ID:
        case NuxeoTypeHelper.NX_PARENT_ID:
            return NXQL.ECM_PARENTID;
        case NuxeoTypeHelper.NX_PATH_SEGMENT:
            return NXQL.ECM_NAME;
        case NuxeoTypeHelper.NX_POS:
            return NXQL.ECM_POS;
        case PropertyIds.OBJECT_TYPE_ID:
            return NXQL.ECM_PRIMARYTYPE;
        case PropertyIds.SECONDARY_OBJECT_TYPE_IDS:
        case NuxeoTypeHelper.NX_FACETS:
            return NXQL.ECM_MIXINTYPE;
        case PropertyIds.VERSION_LABEL:
            return NXQL.ECM_VERSIONLABEL;
        case PropertyIds.IS_LATEST_MAJOR_VERSION:
            return NXQL.ECM_ISLATESTMAJORVERSION;
        case PropertyIds.IS_LATEST_VERSION:
            return NXQL.ECM_ISLATESTVERSION;
        case NuxeoTypeHelper.NX_ISVERSION:
            return NXQL.ECM_ISVERSION;
        case NuxeoTypeHelper.NX_ISCHECKEDIN:
            return NXQL.ECM_ISCHECKEDIN;
        case NuxeoTypeHelper.NX_ISTRASHED:
            return NXQL.ECM_ISTRASHED;
        case NuxeoTypeHelper.NX_LIFECYCLE_STATE:
            return NXQL.ECM_LIFECYCLESTATE;
        case PropertyIds.NAME:
            return NXQL_DC_TITLE;
        case PropertyIds.DESCRIPTION:
            return NXQL_DC_DESCRIPTION;
        case PropertyIds.CREATED_BY:
            return NXQL_DC_CREATOR;
        case PropertyIds.CREATION_DATE:
            return NXQL_DC_CREATED;
        case PropertyIds.LAST_MODIFICATION_DATE:
            return NXQL_DC_MODIFIED;
        case PropertyIds.LAST_MODIFIED_BY:
            return NXQL_DC_LAST_CONTRIBUTOR;
        case PropertyIds.SOURCE_ID:
            return NXQL_REL_SOURCE;
        case PropertyIds.TARGET_ID:
            return NXQL_REL_TARGET;
        }
        return null;
    }

    protected static String cmisToNxqlFulltextQuery(String statement) {
        // NXQL syntax has implicit AND
        statement = statement.replace(" and ", " ");
        statement = statement.replace(" AND ", " ");
        return statement;
    }

    /**
     * Convert an ORDER BY part from CMISQL to NXQL.
     *
     * @since 6.0
     */
    protected String convertOrderBy(String orderBy, TypeManagerImpl typeManager) {
        List<String> list = new ArrayList<>(1);
        for (String order : orderBy.split(",")) {
            order = order.trim();
            String lower = order.toLowerCase();
            String prop;
            boolean asc;
            if (lower.endsWith(SPACE_ASC)) {
                prop = order.substring(0, order.length() - SPACE_ASC.length()).trim();
                asc = true;
            } else if (lower.endsWith(SPACE_DESC)) {
                prop = order.substring(0, order.length() - SPACE_DESC.length()).trim();
                asc = false;
            } else {
                prop = order;
                asc = true; // default is repository-specific
            }
            // assume query name is same as property id
            String propId = typeManager.getPropertyIdForQueryName(prop);
            if (propId == null) {
                throw new CmisInvalidArgumentException("Invalid orderBy: " + orderBy);
            }
            String col = getColumn(propId);
            list.add(asc ? col : (col + " DESC"));
        }
        return StringUtils.join(list, ", ");
    }

    /**
     * Walker of the WHERE clause that doesn't parse fulltext expressions.
     */
    public class AnalyzingWalker extends AbstractPredicateWalker {

        public boolean hasContains;

        @Override
        public Boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
            if (hasContains && Framework.getService(ConfigurationService.class)
                                        .isBooleanPropertyFalse(NuxeoRepository.RELAX_CMIS_SPEC)) {
                throw new QueryParseException("At most one CONTAINS() is allowed");
            }
            hasContains = true;
            return null;
        }
    }

    /**
     * Walker of the WHERE clause that generates NXQL.
     */
    public class GeneratingWalker extends AbstractPredicateWalker {

        public static final String NX_FULLTEXT_INDEX_PREFIX = "nx:";

        public StringBuilder buf = new StringBuilder();

        @Override
        public Boolean walkNot(Tree opNode, Tree node) {
            buf.append("NOT ");
            walkPredicate(node);
            return null;
        }

        @Override
        public Boolean walkAnd(Tree opNode, Tree leftNode, Tree rightNode) {
            buf.append("(");
            walkPredicate(leftNode);
            buf.append(" AND ");
            walkPredicate(rightNode);
            buf.append(")");
            return null;
        }

        @Override
        public Boolean walkOr(Tree opNode, Tree leftNode, Tree rightNode) {
            buf.append("(");
            walkPredicate(leftNode);
            buf.append(" OR ");
            walkPredicate(rightNode);
            buf.append(")");
            return null;
        }

        @Override
        public Boolean walkEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            buf.append(" = ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkNotEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            buf.append(" <> ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkGreaterThan(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            buf.append(" > ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkGreaterOrEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            buf.append(" >= ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkLessThan(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            buf.append(" < ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkLessOrEquals(Tree opNode, Tree leftNode, Tree rightNode) {
            walkExpr(leftNode);
            buf.append(" <= ");
            walkExpr(rightNode);
            return null;
        }

        @Override
        public Boolean walkIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            buf.append(" IN ");
            walkExpr(listNode);
            return null;
        }

        @Override
        public Boolean walkNotIn(Tree opNode, Tree colNode, Tree listNode) {
            walkExpr(colNode);
            buf.append(" NOT IN ");
            walkExpr(listNode);
            return null;
        }

        @Override
        public Boolean walkInAny(Tree opNode, Tree colNode, Tree listNode) {
            walkAny(colNode, "IN", listNode);
            return null;
        }

        @Override
        public Boolean walkNotInAny(Tree opNode, Tree colNode, Tree listNode) {
            walkAny(colNode, "NOT IN", listNode);
            return null;
        }

        @Override
        public Boolean walkEqAny(Tree opNode, Tree literalNode, Tree colNode) {
            // note that argument order is reversed
            walkAny(colNode, "=", literalNode);
            return null;
        }

        protected void walkAny(Tree colNode, String op, Tree exprNode) {
            ColumnReference col = getColumnReference(colNode);
            if (col.getPropertyDefinition().getCardinality() != Cardinality.MULTI) {
                throw new QueryParseException(
                        "Cannot use " + op + " ANY with single-valued property: " + col.getPropertyQueryName());
            }
            String nxqlCol = (String) col.getInfo();
            buf.append(nxqlCol);
            if (!NXQL.ECM_MIXINTYPE.equals(nxqlCol)) {
                buf.append("/*");
            }
            buf.append(' ');
            buf.append(op);
            buf.append(' ');
            walkExpr(exprNode);
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
            ColumnReference col = getColumnReference(colNode);
            boolean multi = col.getPropertyDefinition().getCardinality() == Cardinality.MULTI;
            walkExpr(colNode);
            if (multi) {
                buf.append("/*");
            }
            buf.append(isNull ? " IS NULL" : " IS NOT NULL");
            return null;
        }

        @Override
        public Boolean walkLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            buf.append(" LIKE ");
            walkExpr(stringNode);
            return null;
        }

        @Override
        public Boolean walkNotLike(Tree opNode, Tree colNode, Tree stringNode) {
            walkExpr(colNode);
            buf.append(" NOT LIKE ");
            walkExpr(stringNode);
            return null;
        }

        @Override
        public Boolean walkContains(Tree opNode, Tree qualNode, Tree queryNode) {
            String statement = (String) super.walkString(queryNode);
            String indexName = NXQL.ECM_FULLTEXT;
            // micro parsing of the fulltext statement to perform fulltext
            // search on a non default index
            if (statement.startsWith(NX_FULLTEXT_INDEX_PREFIX)) {
                statement = statement.substring(NX_FULLTEXT_INDEX_PREFIX.length());
                int firstColumnIdx = statement.indexOf(':');
                if (firstColumnIdx > 0 && firstColumnIdx < statement.length() - 1) {
                    indexName += '_' + statement.substring(0, firstColumnIdx);
                    statement = statement.substring(firstColumnIdx + 1);
                } else {
                    log.warn(String.format("fail to microparse custom fulltext index:" + " fallback to '%s'",
                            indexName));
                }
            }
            // CMIS syntax to NXQL syntax
            statement = cmisToNxqlFulltextQuery(statement);
            buf.append(indexName);
            buf.append(" = ");
            buf.append(NXQL.escapeString(statement));
            return null;
        }

        @Override
        public Boolean walkInFolder(Tree opNode, Tree qualNode, Tree paramNode) {
            String id = (String) super.walkString(paramNode);
            buf.append(NXQL.ECM_PARENTID);
            buf.append(" = ");
            buf.append(NXQL.escapeString(id));
            return null;
        }

        @Override
        public Boolean walkInTree(Tree opNode, Tree qualNode, Tree paramNode) {
            String id = (String) super.walkString(paramNode);
            // don't use ecm:ancestorId because the Elasticsearch converter doesn't understand it
            // buf.append(NXQL.ECM_ANCESTORID);
            // buf.append(" = ");
            // buf.append(NXQL.escapeString(id));
            String path;
            DocumentRef docRef = new IdRef(id);
            if (coreSession.exists(docRef)) {
                path = coreSession.getDocument(docRef).getPathAsString();
            } else {
                // TODO better removal
                path = "/__NOSUCHPATH__";
            }
            buf.append(NXQL.ECM_PATH);
            buf.append(" STARTSWITH ");
            buf.append(NXQL.escapeString(path));
            return null;
        }

        @Override
        public Object walkList(Tree node) {
            buf.append("(");
            for (int i = 0; i < node.getChildCount(); i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                Tree child = node.getChild(i);
                walkExpr(child);
            }
            buf.append(")");
            return null;
        }

        @Override
        public Object walkBoolean(Tree node) {
            Object value = super.walkBoolean(node);
            buf.append(Boolean.FALSE.equals(value) ? "0" : "1");
            return null;
        }

        @Override
        public Object walkNumber(Tree node) {
            // Double or Long
            Number value = (Number) super.walkNumber(node);
            buf.append(value.toString());
            return null;
        }

        @Override
        public Object walkString(Tree node) {
            String value = (String) super.walkString(node);
            buf.append(NXQL.escapeString(value));
            return null;
        }

        @Override
        public Object walkTimestamp(Tree node) {
            Calendar value = (Calendar) super.walkTimestamp(node);
            buf.append("TIMESTAMP ");
            buf.append(QUOTE);
            buf.append(ISO_DATE_TIME_FORMAT.print(LocalDateTime.fromCalendarFields(value)));
            buf.append(QUOTE);
            return null;
        }

        @Override
        public Object walkCol(Tree node) {
            String nxqlCol = (String) getColumnReference(node).getInfo();
            buf.append(nxqlCol);
            return null;
        }

        protected ColumnReference getColumnReference(Tree node) {
            CmisSelector sel = query.getColumnReference(Integer.valueOf(node.getTokenStartIndex()));
            if (sel instanceof ColumnReference) {
                return (ColumnReference) sel;
            } else {
                throw new QueryParseException("Cannot use column in WHERE clause: " + sel.getName());
            }
        }
    }

    /**
     * IterableQueryResult wrapping the one from the NXQL query to turn values into CMIS ones.
     */
    // static to avoid keeping the whole QueryMaker in the returned object
    public static class NXQLtoCMISIterableQueryResult
            implements IterableQueryResult, Iterator<Map<String, Serializable>> {

        protected IterableQueryResult it;

        protected Iterator<Map<String, Serializable>> iter;

        protected Map<String, String> realColumns;

        protected Map<String, ColumnReference> virtualColumns;

        protected NuxeoCmisService service;

        public NXQLtoCMISIterableQueryResult(IterableQueryResult it, Map<String, String> realColumns,
                Map<String, ColumnReference> virtualColumns, NuxeoCmisService service) {
            this.it = it;
            iter = it.iterator();
            this.realColumns = realColumns;
            this.virtualColumns = virtualColumns;
            this.service = service;
        }

        @Override
        public Iterator<Map<String, Serializable>> iterator() {
            return this;
        }

        @Override
        public void close() {
            it.close();
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean isLife() {
            return it.isLife();
        }

        @Override
        public boolean mustBeClosed() {
            return it.mustBeClosed();
        }

        @Override
        public long size() {
            return it.size();
        }

        @Override
        public long pos() {
            return it.pos();
        }

        @Override
        public void skipTo(long pos) {
            it.skipTo(pos);
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Serializable> next() {
            // map of NXQL to value
            Map<String, Serializable> nxqlMap = iter.next();
            return convertToCMISMap(nxqlMap, realColumns, virtualColumns, service);

        }

    }

    protected static Map<String, Serializable> convertToCMISMap(Map<String, Serializable> nxqlMap,
            Map<String, String> realColumns, Map<String, ColumnReference> virtualColumns, NuxeoCmisService service) {
        // find the CMIS keys and values
        Map<String, Serializable> cmisMap = new HashMap<>();
        for (Entry<String, String> en : realColumns.entrySet()) {
            String cmisCol = en.getKey();
            String nxqlCol = en.getValue();
            Serializable value = nxqlMap.get(nxqlCol);
            // type conversion to CMIS values
            if (value instanceof Long) {
                value = BigInteger.valueOf(((Long) value).longValue());
            } else if (value instanceof Integer) {
                value = BigInteger.valueOf(((Integer) value).intValue());
            } else if (value instanceof Double) {
                if (((Double) value).isNaN()) {
                    value = BigDecimal.ZERO;
                } else {
                    value = BigDecimal.valueOf(((Double) value).doubleValue());
                }
            } else if (value == null) {
                // special handling of some columns where NULL means FALSE
                if (NULL_IS_FALSE_COLUMNS.contains(nxqlCol)) {
                    value = Boolean.FALSE;
                }
            }
            cmisMap.put(cmisCol, value);
        }

        // virtual values
        // map to store actual data for each qualifier
        Map<String, NuxeoObjectData> datas = null;
        TypeManagerImpl typeManager = service.getTypeManager();
        for (Entry<String, ColumnReference> vc : virtualColumns.entrySet()) {
            String key = vc.getKey();
            ColumnReference col = vc.getValue();
            String qual = col.getQualifier();
            if (col.getPropertyId().equals(PropertyIds.BASE_TYPE_ID)) {
                // special case, no need to get full Nuxeo Document
                String typeId = (String) cmisMap.get(PropertyIds.OBJECT_TYPE_ID);
                if (typeId == null) {
                    throw new NullPointerException();
                }
                TypeDefinitionContainer type = typeManager.getTypeById(typeId);
                String baseTypeId = type.getTypeDefinition().getBaseTypeId().value();
                cmisMap.put(key, baseTypeId);
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
                String id = (String) cmisMap.get(PropertyIds.OBJECT_ID);
                try {
                    // reentrant call to the same session, but the MapMaker
                    // is only called from the IterableQueryResult in
                    // queryAndFetch which manipulates no session state
                    // TODO constructing the DocumentModel (in
                    // NuxeoObjectData) is expensive, try to get value
                    // directly
                    data = service.getObject(service.getNuxeoRepository().getId(), id, null, null, null, null, null,
                            null, null);
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
                NuxeoPropertyDataBase<?> pd = data.getProperty(col.getPropertyId());
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
            cmisMap.put(key, v);
        }

        return cmisMap;
    }

}

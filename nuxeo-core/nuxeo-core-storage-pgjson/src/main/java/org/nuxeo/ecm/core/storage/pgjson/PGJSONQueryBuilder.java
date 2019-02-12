/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.pgjson;

import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IN_MIGRATION;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDICATED_PROPERTY;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.FACETED_TAG;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.FACETED_TAG_LABEL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.COL_MIXIN_TYPES;
import static org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.COL_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.PSEUDO_KEY_JSON;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_BOOLEAN;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_JSON;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_STRING;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_STRING_ARRAY;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_TIMESTAMP;

import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.FullTextUtils;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.io.registry.TestWriterRegistry.MapWriter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.storage.pgjson.PGColumn.PGColumnAndValue;
import org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.TypesMap;
import org.nuxeo.ecm.core.storage.pgjson.PGType.PGTypeAndValue;
import org.nuxeo.ecm.core.storage.pgjson.PGType.PGTypeWithName;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.DialectPostgreSQL;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationStatus;

/**
 * Query builder for a PostgreSQL query of the repository from an {@link Expression}.
 *
 * @since 11.1
 */
public class PGJSONQueryBuilder extends DefaultQueryVisitor {

    private static final Logger log = LogManager.getLogger(PGJSONQueryBuilder.class);

    protected static final String PATH_SEP = "/";

    protected static final String DATE_CAST = "DATE";

    protected static final String COUNT_FUNCTION = "COUNT";

    protected static final String AVG_FUNCTION = "AVG";

    // digits or star or star followed by digits, for xpath segments
    protected final static Pattern INDEX = Pattern.compile("\\d+|\\*|\\*\\d+");

    // non-canonical xpath index syntax, for replaceAll
    protected final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+|\\*|\\*\\d+)\\]" // index in brackets
    );

    protected final SchemaManager schemaManager;

    protected final PathResolver pathResolver;

    protected final Map<String, PGColumn> keyToColumn;

    protected final PGColumn jsonDocColumn;

    protected final TypesMap typesMap;

    protected boolean fulltextSearchDisabled;

    /** The query we're building. */
    public final StringBuilder buf = new StringBuilder();

    /** The columns from the select clause. */
    public final List<PGColumn> selectColumns = new ArrayList<>();

    /** The types and values for the prepared statement parameters in the query. */
    public final List<PGTypeAndValue> values = new ArrayList<>();

    protected int currentReferenceStart = -1;

    protected String currentCast;

    protected PGType currentPGType;

    protected Type currentType;

    protected List<String> documentTypes;

    public PGJSONQueryBuilder(PGJSONRepository repository, PathResolver pathResolver, boolean fulltextSearchDisabled) {
        this.pathResolver = pathResolver;
        keyToColumn = repository.getKeyToColumn();
        jsonDocColumn = keyToColumn.get(PSEUDO_KEY_JSON);
        typesMap = repository.getTypesMap();
        this.fulltextSearchDisabled = fulltextSearchDisabled;
        schemaManager = Framework.getService(SchemaManager.class);
    }

    @Override
    public void visitSelectClause(SelectClause node) {
        for (Iterator<Operand> it = node.elements.values().iterator(); it.hasNext();) {
            Operand operand = it.next();
            if (!(operand instanceof Reference)) {
                throw new QueryParseException("Invalid projection: " + operand);
            }
            Reference reference = (Reference) operand;
            visitReference(reference);
            PGColumn column = new PGColumn(reference.name, null, currentPGType);
            selectColumns.add(column);
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
    }

    public void visit(Expression node) {
        if (node instanceof MultiExpression) {
            visitMultiExpression((MultiExpression) node);
        } else {
            visitExpression(node);
        }
    }

    @Override
    public void visitMultiExpression(MultiExpression node) {
        buf.append('(');
        for (Iterator<Predicate> it = node.predicates.iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                node.operator.accept(this);
            }
        }
        buf.append(')');
    }

    @Override
    public void visitExpression(Expression node) {
        buf.append('(');
        Operand lvalue = node.lvalue;
        Reference ref = lvalue instanceof Reference ? (Reference) lvalue : null;
        String name = ref == null ? null : ref.name;
        Operator op = node.operator;
        if (op == Operator.STARTSWITH) {
            visitExpressionStartsWith(node);
        } else if (NXQL.ECM_PATH.equals(name)) {
            visitExpressionEcmPath(node);
        } else if (NXQL.ECM_ANCESTORID.equals(name)) {
            visitExpressionAncestorId(node);
        } else if (NXQL.ECM_ISVERSION_OLD.equals(name) || NXQL.ECM_ISVERSION.equals(name)
                || NXQL.ECM_ISPROXY.equals(name)) {
            visitExpressionWhereFalseIsNull(node);
        } else if (NXQL.ECM_ISCHECKEDIN.equals(name) || NXQL.ECM_ISLATESTVERSION.equals(name)
                || NXQL.ECM_ISLATESTMAJORVERSION.equals(name)) {
            visitExpressionWhereFalseMayBeNull(node);
        } else if (NXQL.ECM_ISTRASHED.equals(name)) {
            visitExpressionIsTrashed(node);
        } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
            if (op == Operator.EQ || op == Operator.NOTEQ || op == Operator.IN || op == Operator.NOTIN) {
                visitExpressionEqOrInMixins(op, node.rvalue);
            } else {
                throw new QueryParseException(NXQL.ECM_MIXINTYPE + " can only be used with equality operators");
            }
        } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT) && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
            visitExpressionFulltext(node, name);
        } else if (op == Operator.EQ || op == Operator.NOTEQ || op == Operator.IN || op == Operator.NOTIN) {
            visitExpressionEqOrIn(lvalue, op, node.rvalue);
        } else if (op == Operator.LIKE || op == Operator.NOTLIKE || op == Operator.ILIKE || op == Operator.NOTILIKE) {
            visitExpressionLikeOrIlike(node);
        } else if (op == Operator.BETWEEN || op == Operator.NOTBETWEEN) {
            visitExpressionBetween(node);
        } else {
            super.visitExpression(node);
        }
        currentReferenceStart = -1;
        currentCast = null;
        currentPGType = null;
        currentType = null;
        buf.append(')');
    }

    protected void visitExpressionBetween(Expression node) {
        node.lvalue.accept(this);
        LiteralList l = (LiteralList) node.rvalue;
        buf.append(' ');
        node.operator.accept(this);
        buf.append(' ');
        l.get(0).accept(this);
        buf.append(" AND ");
        l.get(1).accept(this);
    }

    protected void visitExpressionStartsWith(Expression node) {
        Operand lvalue = node.lvalue;
        Operand rvalue = node.rvalue;
        if (!(lvalue instanceof Reference)) {
            throw new QueryParseException("Illegal left argument for " + Operator.STARTSWITH + ": " + lvalue);
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(Operator.STARTSWITH + " requires literal path as right argument");
        }
        String path = ((StringLiteral) rvalue).value;
        if (path.length() > 1 && path.endsWith(PATH_SEP)) { // remove trailing slash
            path = path.substring(0, path.length() - PATH_SEP.length());
        }
        String name = ((Reference) lvalue).name;
        if (NXQL.ECM_PATH.equals(name)) {
            visitExpressionStartsWithPath(path);
        } else {
            visitExpressionStartsWithNonPath(lvalue, path);
        }
    }

    protected void visitExpressionStartsWithPath(String path) {
        // find the id from the path
        Serializable id = pathResolver.getIdForPath(path);
        if (id == null) {
            // no such path, always return a false
            // TODO remove the expression more intelligently from the parse tree
            buf.append("false");
        } else {
            PGColumn col = keyToColumn.get(KEY_ANCESTOR_IDS);
            buf.append(col.name);
            buf.append(" @> ?");
            values.add(new PGTypeAndValue(col.type, new Object[] { id }));
        }
    }

    protected void visitExpressionStartsWithNonPath(Operand lvalue, String path) {
        buf.append('(');
        visitExpressionEqOrIn(lvalue, Operator.EQ, new StringLiteral(path));
        visitOperator(Operator.OR);
        String pathPattern = escapeLike(path) + PATH_SEP + '%';
        visitExpressionLikeOrIlike(lvalue, Operator.LIKE, new StringLiteral(pathPattern));
        buf.append(')');
    }

    protected static String escapeLike(String string) {
        return string.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    protected void visitExpressionEcmPath(Expression node) {
        if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_PATH + " requires = or <> operator");
        }
        if (!(node.rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_PATH + " requires literal path as right argument");
        }
        String path = ((StringLiteral) node.rvalue).value;
        if (path.length() > 1 && path.endsWith(PATH_SEP)) {
            path = path.substring(0, path.length() - PATH_SEP.length());
        }
        String id = pathResolver.getIdForPath(path);
        if (id == null) {
            // no such path, always return false
            // TODO remove the expression more intelligently from the parse tree
            buf.append("false");
        } else {
            PGColumn col = keyToColumn.get(KEY_ID);
            buf.append(col.name);
            visitOperator(node.operator);
            visitId(id);
        }
    }

    protected void visitExpressionAncestorId(Expression node) {
        if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires = or <> operator");
        }
        if (!(node.rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires literal id as right argument");
        }
        boolean not = node.operator == Operator.NOTEQ;
        String id = ((StringLiteral) node.rvalue).value;
        if (not) {
            buf.append("(NOT (");
        }
        PGColumn col = keyToColumn.get(KEY_ANCESTOR_IDS);
        buf.append(col.name);
        buf.append(" @> ?");
        values.add(new PGTypeAndValue(col.type, new Object[] { id }));
        if (not) {
            buf.append("))");
        }
    }

    protected void visitExpressionWhereFalseIsNull(Expression node) {
        String name = ((Reference) node.lvalue).name;
        boolean bool = getBooleanRValue(name, node);
        node.lvalue.accept(this);
        if (!bool) {
            buf.append(" IS NULL");
        }
    }

    protected void visitExpressionWhereFalseMayBeNull(Expression node) {
        String name = ((Reference) node.lvalue).name;
        boolean bool = getBooleanRValue(name, node);
        if (bool) {
            node.lvalue.accept(this);
        } else {
            buf.append("(NOT ");
            node.lvalue.accept(this);
            buf.append(" OR ");
            node.lvalue.accept(this);
            buf.append(" IS NULL)");
        }
    }

    protected void visitExpressionIsTrashed(Expression node) {
        TrashService trashService = Framework.getService(TrashService.class);
        if (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            visitExpressionIsTrashedOnLifeCycle(node);
        } else if (trashService.hasFeature(TRASHED_STATE_IN_MIGRATION)) {
            visitExpressionIsTrashedOnLifeCycle(node);
            buf.append(" OR ");
            visitExpressionWhereFalseMayBeNull(node);
        } else if (trashService.hasFeature(TRASHED_STATE_IS_DEDICATED_PROPERTY)) {
            visitExpressionWhereFalseMayBeNull(node);
        } else {
            throw new UnsupportedOperationException("TrashService is in an unknown state");
        }
    }

    @SuppressWarnings("deprecation")
    protected void visitExpressionIsTrashedOnLifeCycle(Expression node) {
        String name = ((Reference) node.lvalue).name;
        boolean bool = getBooleanRValue(name, node);
        Operator op = bool ? Operator.EQ : Operator.NOTEQ;
        visitReference(new Reference(NXQL.ECM_LIFECYCLESTATE));
        visitOperator(op);
        visitString(LifeCycleConstants.DELETED_STATE);
    }

    protected boolean getBooleanRValue(String name, Expression node) {
        if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
            throw new QueryParseException(name + " requires = or <> operator");
        }
        long v;
        if (!(node.rvalue instanceof IntegerLiteral) || ((v = ((IntegerLiteral) node.rvalue).value) != 0 && v != 1)) {
            throw new QueryParseException(name + " requires literal 0 or 1 as right argument");
        }
        return node.operator == Operator.EQ ^ v == 0;
    }

    protected void visitExpressionFulltext(Expression node, String name) {
        Operator op = node.operator;
        Operand rvalue = node.rvalue;
        if (op != Operator.EQ && op != Operator.LIKE) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires = or LIKE operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires literal string as right argument");
        }
        if (fulltextSearchDisabled) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        String fulltextQuery = ((StringLiteral) rvalue).value;
        if (name.equals(NXQL.ECM_FULLTEXT)) {
            // standard fulltext query
            hasFulltext = true;
            String ft = getMongoDBFulltextQuery(fulltextQuery);
            if (ft == null) {
                // empty query, matches nothing
                buf.append("false");
                return;
            }
            Document textSearch = new Document();
            textSearch.put(QueryOperators.SEARCH, ft);
            // TODO language?
            return new Document(QueryOperators.TEXT, textSearch);
        } else {

        }

        String[] nameref = new String[] { name };
        boolean useIndex = findFulltextIndexOrField(model, nameref);
        name = nameref[0];
        if (useIndex) {
            // use actual fulltext query using a dedicated index
            DialectPostgreSQL dialect = new DialectPostgreSQL(null, null);
            fulltextQuery = dialect.getDialectFulltextQuery(fulltextQuery);
            FulltextMatchInfo info = dialect.getFulltextScoredMatchInfo(fulltextQuery, name, ftJoinNumber, mainColumn,
                    model, database);
            ftMatchInfo = info;
            buf.append(info.whereExpr);
            if (info.whereExprParam != null) {
                values.add(info.whereExprParam);
            }
        } else {
            // single field matched with ILIKE
            log.warn("No fulltext index configured for field " + name + ", falling back on LIKE query");
            String value = ((StringLiteral) node.rvalue).value;

            // fulltext translation into pseudo-LIKE syntax
            Set<String> words = FullTextUtils.parseFullText(value, false);
            if (words.isEmpty()) {
                // only stop words or empty
                value = "DONTMATCHANYTHINGFOREMPTYQUERY";
            } else {
                value = "%" + StringUtils.join(new ArrayList<>(words), "%") + "%";
            }

            Reference ref = new Reference(name);
            visitReference(ref);
            buf.append(" ILIKE ");
            visitString(value);
        }
    }

    protected static boolean findFulltextIndexOrField(Model model, String[] nameref) {
        boolean useIndex;
        String name = nameref[0];
        if (name.equals(NXQL.ECM_FULLTEXT)) {
            name = Model.FULLTEXT_DEFAULT_INDEX;
            useIndex = true;
        } else {
            // ecm:fulltext_indexname
            // ecm:fulltext.field
            char sep = name.charAt(NXQL.ECM_FULLTEXT.length());
            if (sep != '.' && sep != '_') {
                throw new QueryParseException("Unknown field: " + name);
            }
            useIndex = sep == '_';
            name = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
            if (useIndex) {
                if (!model.getFulltextConfiguration().indexNames.contains(name)) {
                    throw new QueryParseException("No such fulltext index: " + name);
                }
            } else {
                // find if there's an index holding just that field
                String index = model.getFulltextConfiguration().fieldToIndexName.get(name);
                if (index != null) {
                    name = index;
                    useIndex = true;
                }
            }
        }
        nameref[0] = name;
        return useIndex;
    }

    protected static Serializable getSerializableLiteral(Literal literal) {
        Serializable value;
        if (literal instanceof BooleanLiteral) {
            value = Boolean.valueOf(((BooleanLiteral) literal).value);
        } else if (literal instanceof DateLiteral) {
            DateLiteral dLit = (DateLiteral) literal;
            value = dLit.onlyDate ? dLit.toSqlDate() : dLit.toCalendar();
        } else if (literal instanceof DoubleLiteral) {
            value = Double.valueOf(((DoubleLiteral) literal).value);
        } else if (literal instanceof IntegerLiteral) {
            value = Long.valueOf(((IntegerLiteral) literal).value);
        } else if (literal instanceof StringLiteral) {
            value = ((StringLiteral) literal).value;
        } else {
            throw new QueryParseException("type of literal in list is not recognized: " + literal.getClass());
        }
        return value;
    }

    protected static List<Serializable> getSerializableLiterals(Operand rvalue) {
        List<Serializable> params;
        if (rvalue instanceof Literal) {
            Serializable param = getSerializableLiteral((Literal) rvalue);
            params = Collections.singletonList(param);
        } else {
            LiteralList list = (LiteralList) rvalue;
            if (list.isEmpty()) {
                throw new QueryParseException("Empty list: " + rvalue);
            }
            params = new ArrayList<>(list.size());
            for (Literal literal : list) {
                params.add(getSerializableLiteral(literal));
            }
        }
        return params;
    }

    protected static Set<String> getStringLiterals(Operand rvalue) {
        Set<String> set;
        if (rvalue instanceof StringLiteral) {
            String param = ((StringLiteral) rvalue).value;
            set = Collections.singleton(param);
        } else if (rvalue instanceof LiteralList) {
            LiteralList list = (LiteralList) rvalue;
            if (list.isEmpty()) {
                throw new QueryParseException("Empty list: " + rvalue);
            }
            set = new LinkedHashSet<>(list.size());
            for (Literal literal : list) {
                set.add(((StringLiteral) literal).value);
            }
        } else {
            throw new QueryParseException("Not a list of strings: " + rvalue);
        }
        return set;
    }

    protected void visitExpressionEqOrIn(Operand lvalue, Operator op, Operand rvalue) {
        lvalue.accept(this);
        if (currentType.isListType()) {
            String name = unvisitReference();
            List<Serializable> params = getSerializableLiterals(rvalue);
            if (currentPGType == TYPE_JSON) {
                visitExpressionEqOrInJSONArray(name, op, params);
            } else {
                visitExpressionEqOrInArray(name, op, params);
            }
        } else {
            // regular SQL
            op.accept(this);
            rvalue.accept(this);
        }
    }

    // ===== for strings only =====
    // dc:contributors = 'foo'
    // => doc->'dc:contributors' ? 'foo'
    // dc:contributors IN ('foo','bar')
    // => doc->'dc:contributors' ?| ARRAY['foo','bar']
    // ===== for other types =====
    // my:values = 1
    // => doc->'my:values' @> '[1]'::jsonb
    // my:values IN (1,2)
    // => doc->'my:values' @> '[1]'::jsonb OR doc->'my:values' @> '[2]'::jsonb
    // ===== for negation =====
    // my:values <> 1
    // => NOT(doc->'my:values' @> '[1]'::jsonb) OR doc->'my:values' IS NULL
    protected void visitExpressionEqOrInJSONArray(String name, Operator op, List<Serializable> params) {
        boolean not = op == Operator.NOTEQ || op == Operator.NOTIN;
        if (not) {
            buf.append("(NOT(");
        }
        if (currentType == StringType.INSTANCE) {
            // for strings we can use jsonb existence operators
            if (params.size() == 1) {
                Serializable param = params.get(0);
                // col ? 'key'
                buf.append(name);
                buf.append(" ?? ?"); // double ? for escaping
                if (currentCast != null) {
                    // DATE cast
                    buf.append("::");
                    buf.append(currentCast);
                    buf.append("[]");
                }
                values.add(new PGTypeAndValue(PGType.from(currentType, false), param));
            } else {
                // col ?| ARRAY[x, y, z]
                buf.append(name);
                buf.append(" ??| ?"); // double ? for escaping
                values.add(new PGTypeAndValue(PGType.from(currentType, true), params.toArray()));
            }
        } else {
            // generic containment operator
            for (Iterator<Serializable> it = params.iterator(); it.hasNext();) {
                Serializable param = it.next();
                // col @> ARRAY[x]
                buf.append(name);
                buf.append(" @> ?::jsonb");
                values.add(new PGTypeAndValue(TYPE_JSON, new Object[] { param }));
                if (it.hasNext()) {
                    buf.append(" OR ");
                }
            }
        }
        if (not) {
            buf.append(") OR ");
            buf.append(name);
            buf.append(" IS NULL)");
        }
    }

    // arraycol = 'foo'
    // => 'foo' = ANY(arraycol)
    // arraycol IN ('foo', 'bar')
    // => arraycol && ARRAY['foo','bar']
    // ===== for negation =====
    // arraycol <> 'foo'
    // => NOT(....) OR arraycol IS NULL
    protected void visitExpressionEqOrInArray(String name, Operator op, Collection<? extends Serializable> params) {
        boolean not = op == Operator.NOTEQ || op == Operator.NOTIN;
        if (not) {
            buf.append("(NOT(");
        }
        if (params.size() == 1) {
            Serializable param = params.iterator().next();
            // ? = ANY(column)
            buf.append("? = ANY(");
            buf.append(name);
            if (currentCast != null) {
                // DATE cast
                buf.append("::");
                buf.append(currentCast);
                buf.append("[]");
            }
            buf.append(")");
            values.add(new PGTypeAndValue(currentPGType.baseType, param));
        } else {
            // column && ARRAY[x, y, z]
            buf.append(name);
            buf.append(" && ?");
            values.add(new PGTypeAndValue(currentPGType, params.toArray()));
        }
        if (not) {
            buf.append(") OR ");
            buf.append(name);
            buf.append(" IS NULL)");
        }
    }

    // ecm:mixinTypes IN ('Foo', 'Bar')
    // => primarytype IN (... types with Foo or Bar ...) OR mixintypes && ARRAY['Foo', 'Bar']
    //
    // ecm:mixinTypes NOT IN ('Foo', 'Bar')
    // => primarytype IN (... types without Foo nor Bar ...) AND NOT (mixintypes && ARRAY['Foo', 'Bar'])
    protected void visitExpressionEqOrInMixins(Operator op, Operand rvalue) {
        Set<String> mixins = getStringLiterals(rvalue);
        boolean include = op == Operator.EQ || op == Operator.IN;

        // primary types that match
        Set<String> matchPrimaryTypes;
        if (include) {
            matchPrimaryTypes = new LinkedHashSet<>();
            for (String mixin : mixins) {
                matchPrimaryTypes.addAll(getMixinDocumentTypes(mixin));
            }
        } else {
            matchPrimaryTypes = new LinkedHashSet<>(getDocumentTypes());
            for (String mixin : mixins) {
                matchPrimaryTypes.removeAll(getMixinDocumentTypes(mixin));
            }
        }
        // instance mixins that match
        Set<String> matchMixinTypes = new LinkedHashSet<>();
        for (String mixin : mixins) {
            if (!isNeverPerInstanceMixin(mixin)) {
                matchMixinTypes.add(mixin);
            }
        }
        // query generation
        boolean both = !matchPrimaryTypes.isEmpty() && !matchMixinTypes.isEmpty();
        if (both) {
            buf.append('(');
        }
        if (!matchPrimaryTypes.isEmpty()) {
            buf.append(COL_PRIMARY_TYPE);
            buf.append(" IN ");
            visitStrings(matchPrimaryTypes);
        }
        if (both) {
            buf.append(include ? " OR " : " AND ");
        }
        if (!matchMixinTypes.isEmpty()) {
            if (!include) {
                buf.append("NOT (");
            }
            currentPGType = TYPE_STRING_ARRAY; // TODO find a better way
            visitExpressionEqOrInArray(COL_MIXIN_TYPES, Operator.IN, matchMixinTypes);
            if (!include) {
                buf.append(')');
            }
        }
        if (both) {
            buf.append(')');
        }
    }

    protected Set<String> getMixinDocumentTypes(String mixin) {
        Set<String> types = schemaManager.getDocumentTypeNamesForFacet(mixin);
        return types == null ? Collections.emptySet() : types;
    }

    protected List<String> getDocumentTypes() {
        // TODO precompute in SchemaManager
        if (documentTypes == null) {
            documentTypes = new ArrayList<>();
            for (DocumentType docType : schemaManager.getDocumentTypes()) {
                documentTypes.add(docType.getName());
            }
        }
        return documentTypes;
    }

    protected boolean isNeverPerInstanceMixin(String mixin) {
        return schemaManager.getNoPerDocumentQueryFacets().contains(mixin);
    }

    protected void visitExpressionLikeOrIlike(Expression node) {
        visitExpressionLikeOrIlike(node.lvalue, node.operator, node.rvalue);
    }

    protected void visitExpressionLikeOrIlike(Operand lvalue, Operator op, Operand rvalue) {
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(new Expression(lvalue, op, rvalue).toString());
        }
        lvalue.accept(this);
        String param = ((StringLiteral) rvalue).value;
        if (currentType.isListType()) {
            String name = unvisitReference();
            if (currentPGType == TYPE_JSON) {
                visitExpressionLikeOrIlikeJSONArray(name, op, param);
            } else {
                visitExpressionLikeOrIlikeArray(name, op, param);
            }
        } else if (currentPGType == TYPE_JSON) {
            String name = unvisitReference();
            visitExpressionLikeOrIlikeJSON(name, op, param);
        } else {
            // regular SQL
            op.accept(this);
            rvalue.accept(this);
        }
    }

    protected void visitExpressionLikeOrIlikeJSONArray(String name, Operator op, String param) {
        boolean not = op == Operator.NOTLIKE || op == Operator.NOTILIKE;
        String positiveOp = (op == Operator.LIKE || op == Operator.NOTLIKE) ? "LIKE" : "ILIKE";
        if (not) {
            buf.append("NOT(");
        }
        buf.append("EXISTS (SELECT 1 FROM jsonb_array_elements_text(");
        buf.append(name);
        buf.append(") v WHERE v ");
        buf.append(positiveOp);
        buf.append(" ?)");
        if (not) {
            buf.append(")");
        }
        values.add(new PGTypeAndValue(TYPE_STRING, param));
    }

    protected void visitExpressionLikeOrIlikeArray(String name, Operator op, String param) {
        boolean not = op == Operator.NOTLIKE || op == Operator.NOTILIKE;
        String positiveOp = (op == Operator.LIKE || op == Operator.NOTLIKE) ? "LIKE" : "ILIKE";
        if (not) {
            buf.append("NOT(");
        }
        buf.append("EXISTS (SELECT 1 FROM UNNEST(");
        buf.append(name);
        buf.append(") v WHERE v ");
        buf.append(positiveOp);
        buf.append(" ?)");
        if (not) {
            buf.append(")");
        }
        values.add(new PGTypeAndValue(TYPE_STRING, param));
    }

    // dc:title LIKE 'foo%'
    // => doc->>'dc:title' LIKE 'foo%'
    protected void visitExpressionLikeOrIlikeJSON(String name, Operator op, String param) {
        // replace last -> with ->>
        int i = name.lastIndexOf("->");
        if (i < 0) {
            throw new QueryParseException();
        }
        name = name.substring(0, i) + "->>" + name.substring(i + 2);
        buf.append(name);
        op.accept(this);
        buf.append('?');
        values.add(new PGTypeAndValue(TYPE_STRING, param));
    }

    @Override
    public void visitOperator(Operator node) {
        if (node != Operator.NOT) {
            buf.append(' ');
        }
        buf.append(node.toString());
        buf.append(' ');
    }

    /**
     * Canonicalizes a Nuxeo-xpath.
     * <p>
     * Replaces {@code a/foo[123]/b} with {@code a/123/b}
     * <p>
     * A star or a star followed by digits can be used instead of just the digits as well.
     */
    public static String canonicalXPath(String xpath) {
        while (xpath.length() > 0 && xpath.charAt(0) == '/') {
            xpath = xpath.substring(1);
        }
        if (xpath.indexOf('[') == -1) {
            return xpath;
        } else {
            return NON_CANON_INDEX.matcher(xpath).replaceAll("$1");
        }
    }

    @Override
    public void visitReference(Reference node) {
        String name = node.name;
        String cast = node.cast;
        currentReferenceStart = buf.length();
        currentCast = cast;
        if (NXQL.ECM_FULLTEXT_SCORE.equals(name)) {
            visitScore();
        } else if (name.startsWith(NXQL.ECM_ACL + "/")) {
            parseACP(name);
        } else {
            visitReference(name, cast);
        }
    }

    protected String unvisitReference() {
        String name = buf.substring(currentReferenceStart);
        buf.setLength(currentReferenceStart);
        currentReferenceStart = -1;
        return name;
    }

    protected void visitReference(String xpath, String cast) {
        xpath = canonicalXPath(xpath);
        String[] segments = xpath.split("/");

        PGType pgType = null; // PG column type
        TypesMap tm = null; // Nuxeo type

        if (cast != null) {
            // only DATE cast for now
            // DATE(x) is more amenable to being indexed than a CAST
            buf.append("DATE(");
        }

        // name
        // mixintypes[1]

        // doc->dc:title
        // doc->dc:contributors->0
        // doc->file:file->name
        // doc->files:files->0->name

        // ecm:acl/*1/name = 'local' AND ecm:acl/*1/principal = 'bob'
        // acp->0->name
        // acp->0->acl->0->perm

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            // is it an index?
            boolean isIndex = INDEX.matcher(segment).matches();
            if (isIndex) {
                boolean wildcard = false;
                if (segment.equals("*")) {
                    wildcard = true;
                }
                if (segment.startsWith("*")) {
                    throw new QueryParseException("TODO wildcards");
                }
            }

            if (pgType == null) {
                // first segment
                // find key and type
                String key;
                if (segment.startsWith(NXQL.ECM_PREFIX)) {
                    key = DBSSession.convToInternal(segment); // throws if unknown
                    Type type = DBSSession.getType(key);
                    if (type == null) {
                        throw new QueryParseException(xpath);
                    }
                    tm = new TypesMap(key, type); // XXX
                } else {
                    tm = typesMap.get(segment);
                    if (tm == null) {
                        throw new QueryParseException(xpath);
                    }
                    key = tm.name; // canonical name
                }
                // find first column
                PGColumn column = keyToColumn.get(key);
                if (column == null) {
                    column = jsonDocColumn;
                    buf.append(column.name);
                    buf.append("->'");
                    buf.append(key);
                    buf.append("'");
                } else {
                    buf.append(column.name);
                }
                pgType = column.type;
            } else if (pgType == TYPE_JSON) {
                tm = tm.get(segment);
                if (tm == null) {
                    throw new QueryParseException(xpath);
                }
                String key = tm.name; // canonical name
                buf.append("->");
                if (!isIndex) {
                    buf.append("'");
                }
                buf.append(key);
                if (!isIndex) {
                    buf.append("'");
                }
                // pgtype still TYPE_JSON
            } else if (pgType.isArray()) {
                tm = tm.get(TypesMap.ARRAY_ELEM);
                if (tm == null) {
                    throw new QueryParseException(xpath);
                }
                if (!isIndex) {
                    throw new QueryParseException(xpath);
                }
                buf.append("[");
                buf.append(Integer.parseInt(segment) + 1); // arrays indexes start at 1
                buf.append("]");
                pgType = pgType.baseType;
            } else {
                throw new QueryParseException(xpath);
            }
        }

        if (cast != null) {
            if (DATE_CAST.equals(cast) && pgType != TYPE_TIMESTAMP) {
                throw new QueryParseException("Cannot cast to " + cast + ": " + xpath);
            }
            buf.append(')');
        }
        currentPGType = pgType;
        currentType = tm.type;
    }

    protected void parseACP(String xpath) {
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitLiteralList(LiteralList node) {
        buf.append('(');
        for (Iterator<Literal> it = node.iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append(')');
    }

    protected void visitStrings(Collection<String> strings) {
        buf.append('(');
        for (Iterator<String> it = strings.iterator(); it.hasNext();) {
            String string = it.next();
            visitString(string);
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append(')');
    }

    @Override
    public void visitDateLiteral(DateLiteral node) {
        if (DATE_CAST.equals(currentCast)) {
            if (!node.onlyDate) {
                throw new QueryParseException("DATE() cast must be used with DATE literal, not TIMESTAMP: " + node);
            }
        }
        Calendar cal;
        if (node.onlyDate) {
            cal = node.toCalendar();
            throw new UnsupportedOperationException("TODO");
        } else {
            cal = node.toCalendar();
        }
        visitValue(cal);
    }

    @Override
    public void visitStringLiteral(StringLiteral node) {
        visitString(node.value);
    }

    protected void visitId(String id) {
        visitString(id);
    }

    public void visitString(String string) {
        visitValue(string);
    }

    @Override
    public void visitDoubleLiteral(DoubleLiteral node) {
        visitValue(Double.valueOf(node.value));
    }

    @Override
    public void visitIntegerLiteral(IntegerLiteral node) {
        Object value;
        if (currentPGType == TYPE_BOOLEAN) {
            value = getBooleanValue(node.value);
        } else {
            value = Long.valueOf(node.value);
        }
        visitValue(value);
    }

    // boolean literals are expressed as integers in NXQL but actual booleans in PostgreSQL
    protected Boolean getBooleanValue(long value) {
        if (value != 0 && value != 1) {
            throw new QueryParseException("Boolean expressions require literal 0 or 1 as right argument");
        }
        return Boolean.valueOf(value == 1);
    }

    @Override
    public void visitBooleanLiteral(BooleanLiteral node) {
        visitValue(Boolean.valueOf(node.value));
    }

    protected void visitValue(Object value) {
        PGTypeAndValue typeAndValue = new PGTypeAndValue(currentPGType, value);
        values.add(typeAndValue);
        buf.append('?');
        if (currentPGType == TYPE_JSON) {
            buf.append("::jsonb");
        }
    }

    @Override
    public void visitFunction(Function node) {
        String func = node.name.toUpperCase();
        Reference ref = (Reference) node.args.get(0);
        ref.accept(this); // whatColumns / whatKeys for column

        // replace column info with aggregate
        PGColumn col = whatColumns.removeLast();
        String key = whatKeys.removeLast();
        final String aggFQN = func + "(" + col.getFullQuotedName() + ")";
        final ColumnType aggType = getAggregateType(func, col.getType());
        final int aggJdbcType = dialect.getJDBCTypeAndString(aggType).jdbcType;
        PGColumn cc = new PGColumn(col, col.getTable()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getFullQuotedName() {
                return aggFQN;
            }

            @Override
            public ColumnType getType() {
                return aggType;
            }

            @Override
            public int getJdbcType() {
                return aggJdbcType;
            }
        };
        whatColumns.add(cc);
        whatKeys.add(func + "(" + key + ")");
    }

    protected void visitScore() {
        if (inSelect) {
            PGColumn col = new PGColumn(hierTable, null, ColumnType.DOUBLE, null);
            whatColumns.add(col);
            whatKeys.add(NXQL.ECM_FULLTEXT_SCORE);
        } else {
            buf.append(aliasesByName.get(NXQL.ECM_FULLTEXT_SCORE));
        }
    }

    protected ColumnType getAggregateType(String func, ColumnType arg) {
        if (COUNT_FUNCTION.equals(func)) {
            return ColumnType.LONG;
        }
        if (AVG_FUNCTION.equals(func)) {
            return ColumnType.DOUBLE;
        }
        // SUM, MIN, MAX
        return arg;
    }

    @Override
    public void visitOrderByList(OrderByList node) {
        inOrderBy = true;
        for (OrderByExpr obe : node) {
            if (buf.length() != 0) {
                // we can do this because we generate in an initially empty buffer
                buf.append(", ");
            }
            obe.accept(this);
        }
        inOrderBy = false;
    }

    public void visitOrderByPosColumns() {
        inOrderBy = true;
        for (Entry<String, PGColumn> es : posColumns.entrySet()) {
            PGColumn col = es.getValue();
            if (posColumnsInOrderBy.contains(col)) {
                continue;
            }
            if (buf.length() != 0) {
                buf.append(", ");
            }
            int length = buf.length();
            visitReference(col);
            if (aliasOrderByColumns) {
                // but don't use generated values
                // make the ORDER BY clause uses the aliases instead
                buf.setLength(length);
                String alias = aliasesByName.get(es.getKey());
                buf.append(alias);
            }
        }
        inOrderBy = false;
    }

    @Override
    public void visitOrderByExpr(OrderByExpr node) {
        int length = buf.length();
        // generates needed joins
        super.visitOrderByExpr(node); // visit reference
        if (aliasOrderByColumns) {
            // but don't use generated values
            // make the ORDER BY clause uses the aliases instead
            buf.setLength(length);
            buf.append(aliasesByName.get(node.reference.name));
        }
        if (node.isDescending) {
            buf.append(dialect.getDescending());
        }
    }

}

/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.mongodb;

import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IN_MIGRATION;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDICATED_PROPERTY;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.FACETED_TAG;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.FACETED_TAG_LABEL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SCORE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MIXIN_TYPES;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_MINOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_UID_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_UID_MINOR_VERSION;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_META;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_TEXT_SCORE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;
import org.nuxeo.ecm.core.storage.QueryOptimizer.PrefixInfo;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.mongodb.QueryOperators;

/**
 * Query builder for a MongoDB query of the repository from an {@link Expression}.
 *
 * @since 5.9.4
 */

public class MongoDBRepositoryQueryBuilder extends MongoDBAbstractQueryBuilder {

    private static final Logger log = LogManager.getLogger(MongoDBRepositoryQueryBuilder.class);

    protected final SchemaManager schemaManager;

    protected final String idKey;

    protected List<String> documentTypes;

    protected final SelectClause selectClause;

    protected final OrderByClause orderByClause;

    protected final PathResolver pathResolver;

    public boolean hasFulltext;

    public boolean sortOnFulltextScore;

    protected Document orderBy;

    protected Document projection;

    protected Map<String, String> propertyKeys;

    boolean projectionHasWildcard;

    private boolean fulltextSearchDisabled;

    public MongoDBRepositoryQueryBuilder(MongoDBRepository repository, Expression expression, SelectClause selectClause,
            OrderByClause orderByClause, PathResolver pathResolver, boolean fulltextSearchDisabled) {
        super(repository.converter, expression);
        schemaManager = Framework.getService(SchemaManager.class);
        idKey = repository.idKey;
        this.selectClause = selectClause;
        this.orderByClause = orderByClause;
        this.pathResolver = pathResolver;
        this.fulltextSearchDisabled = fulltextSearchDisabled;
        this.propertyKeys = new HashMap<>();
        likeAnchored = !Framework.getService(ConfigurationService.class).isBooleanPropertyFalse(LIKE_ANCHORED_PROP);
    }

    @Override
    public void walk() {
        super.walk(); // computes hasFulltext
        walkOrderBy(); // computes sortOnFulltextScore
        walkProjection(); // needs hasFulltext and sortOnFulltextScore
        if (hasFulltext) {
            log.debug("Fulltext search on MongoDB: {}", () -> expression,
                    () -> new Throwable("Please consider using Elastic (NXP-31003)"));
        }
    }

    public Document getOrderBy() {
        return orderBy;
    }

    public Document getProjection() {
        return projection;
    }

    public boolean hasProjectionWildcard() {
        return projectionHasWildcard;
    }

    protected void walkOrderBy() {
        sortOnFulltextScore = false;
        if (orderByClause == null) {
            orderBy = null;
        } else {
            orderBy = new Document();
            for (OrderByExpr ob : orderByClause.elements) {
                Reference ref = ob.reference;
                boolean desc = ob.isDescending;
                String field = walkReference(ref).queryField;
                if (!orderBy.containsKey(field)) {
                    Object value;
                    if (KEY_FULLTEXT_SCORE.equals(field)) {
                        if (!desc) {
                            throw new QueryParseException("Cannot sort by " + NXQL.ECM_FULLTEXT_SCORE + " ascending");
                        }
                        sortOnFulltextScore = true;
                        value = new Document(MONGODB_META, MONGODB_TEXT_SCORE);
                    } else {
                        value = desc ? MINUS_ONE : ONE;
                    }
                    orderBy.put(field, value);
                }
            }
            if (sortOnFulltextScore && orderBy.size() > 1) {
                throw new QueryParseException("Cannot sort by " + NXQL.ECM_FULLTEXT_SCORE + " and other criteria");
            }
        }
    }

    protected void walkProjection() {
        projection = new Document();
        boolean projectionOnFulltextScore = false;
        for (Operand op : selectClause.getSelectList().values()) {
            if (!(op instanceof Reference)) {
                throw new QueryParseException("Projection not supported: " + op);
            }
            FieldInfo fieldInfo = walkReference((Reference) op);
            String propertyField = fieldInfo.prop;
            if (!propertyField.equals(NXQL.ECM_UUID) //
                    && !propertyField.equals(fieldInfo.projectionField) //
                    && !propertyField.contains("/")) {
                propertyKeys.put(fieldInfo.projectionField, propertyField);
            }
            projection.put(fieldInfo.projectionField, ONE);
            if (propertyField.contains("*")) {
                projectionHasWildcard = true;
            }
            if (fieldInfo.projectionField.equals(KEY_FULLTEXT_SCORE)) {
                projectionOnFulltextScore = true;
            }
        }
        if (projectionOnFulltextScore || sortOnFulltextScore) {
            if (!hasFulltext) {
                throw new QueryParseException(NXQL.ECM_FULLTEXT_SCORE + " cannot be used without " + NXQL.ECM_FULLTEXT);
            }
            projection.put(KEY_FULLTEXT_SCORE, new Document(MONGODB_META, MONGODB_TEXT_SCORE));
        }
    }

    @Override
    public Document walkExpression(Expression expr) {
        Operator op = expr.operator;
        Operand lvalue = expr.lvalue;
        Operand rvalue = expr.rvalue;
        Reference ref = lvalue instanceof Reference ? (Reference) lvalue : null;
        String name = ref != null ? ref.name : null;
        if (op == Operator.STARTSWITH) {
            return walkStartsWith(lvalue, rvalue);
        } else if (NXQL.ECM_PATH.equals(name)) {
            return walkEcmPath(op, rvalue);
        } else if (NXQL.ECM_ANCESTORID.equals(name)) {
            return walkAncestorId(op, rvalue);
        } else if (NXQL.ECM_ISTRASHED.equals(name)) {
            return walkIsTrashed(op, rvalue);
        } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT) && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
            return walkEcmFulltext(name, op, rvalue);
        } else {
            return super.walkExpression(expr);
        }
    }

    protected Document walkEcmPath(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_PATH + " requires = or <> operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_PATH + " requires literal path as right argument");
        }
        String path = ((StringLiteral) rvalue).value;
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String id = pathResolver.getIdForPath(path);
        if (id == null) {
            // no such path
            // TODO XXX do better
            return new Document(MONGODB_ID, "__nosuchid__");
        }
        Object bsonId = converter.serializableToBson(KEY_ID, id);
        if (op == Operator.EQ) {
            return new Document(idKey, bsonId);
        } else {
            return new Document(idKey, new Document(QueryOperators.NE, bsonId));
        }
    }

    protected Document walkAncestorId(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires = or <> operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires literal id as right argument");
        }
        String ancestorId = ((StringLiteral) rvalue).value;
        Object bsonAncestorId = converter.serializableToBson(KEY_ANCESTOR_IDS, ancestorId);
        if (op == Operator.EQ) {
            return new Document(KEY_ANCESTOR_IDS, bsonAncestorId);
        } else {
            return new Document(KEY_ANCESTOR_IDS, new Document(QueryOperators.NE, bsonAncestorId));
        }
    }

    protected Document walkEcmFulltext(String name, Operator op, Operand rvalue) {
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
                return new Document(MONGODB_ID, "__nosuchid__");
            }
            Document textSearch = new Document();
            textSearch.put(QueryOperators.SEARCH, ft);
            // TODO language?
            return new Document(QueryOperators.TEXT, textSearch);
        } else {
            // secondary index match with explicit field
            // do a regexp on the field
            if (name.charAt(NXQL.ECM_FULLTEXT.length()) != '.') {
                throw new QueryParseException(name + " has incorrect syntax" + " for a secondary fulltext index");
            }
            String prop = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
            String ft = fulltextQuery.replace(" ", "%");
            rvalue = new StringLiteral(ft);
            return walkLike(new Reference(prop), rvalue, true, true);
        }
    }

    protected Document walkIsTrashed(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ISTRASHED + " requires = or <> operator");
        }
        TrashService trashService = Framework.getService(TrashService.class);
        if (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            return walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
        } else if (trashService.hasFeature(TRASHED_STATE_IN_MIGRATION)) {
            Document lifeCycleTrashed = walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
            Document propertyTrashed = walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue,
                    new BooleanLiteral(true));
            return new Document(QueryOperators.OR, new ArrayList<>(Arrays.asList(lifeCycleTrashed, propertyTrashed)));
        } else if (trashService.hasFeature(TRASHED_STATE_IS_DEDICATED_PROPERTY)) {
            return walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue, new BooleanLiteral(true));
        } else {
            throw new UnsupportedOperationException("TrashService is in an unknown state");
        }
    }

    protected Document walkIsTrashed(Reference ref, Operator op, Operand initialRvalue, Literal deletedRvalue) {
        long v;
        if (!(initialRvalue instanceof IntegerLiteral)
                || ((v = ((IntegerLiteral) initialRvalue).value) != 0 && v != 1)) {
            throw new QueryParseException(NXQL.ECM_ISTRASHED + " requires literal 0 or 1 as right argument");
        }
        boolean equalsDeleted = op == Operator.EQ ^ v == 0;
        if (equalsDeleted) {
            return walkEq(ref, deletedRvalue);
        } else {
            return walkNotEq(ref, deletedRvalue);
        }
    }

    // public static for tests
    public static String getMongoDBFulltextQuery(String query) {
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        if (ft == null) {
            return null;
        }
        // translate into MongoDB syntax
        return translateFulltext(ft, false);
    }

    /**
     * Transforms the NXQL fulltext syntax into MongoDB syntax.
     * <p>
     * The MongoDB fulltext query syntax is badly documented, but is actually the following:
     * <ul>
     * <li>a term is a word,
     * <li>a phrase is a set of spaced-separated words enclosed in double quotes,
     * <li>negation is done by prepending a -,
     * <li>the query is a space-separated set of terms, negated terms, phrases, or negated phrases.
     * <li>all the words of non-negated phrases are also added to the terms.
     * </ul>
     * <p>
     * The matching algorithm is (excluding stemming and stop words):
     * <ul>
     * <li>filter out documents with the negative terms, the negative phrases, or missing the phrases,
     * <li>then if any term is present in the document then it's a match.
     * </ul>
     */
    protected static String translateFulltext(FulltextQuery ft, boolean and) {
        List<String> buf = new ArrayList<>();
        translateFulltext(ft, buf, and);
        return StringUtils.join(buf, ' ');
    }

    protected static void translateFulltext(FulltextQuery ft, List<String> buf, boolean and) {
        if (ft.op == Op.OR) {
            for (FulltextQuery term : ft.terms) {
                // don't quote words for OR
                translateFulltext(term, buf, false);
            }
        } else if (ft.op == Op.AND) {
            for (FulltextQuery term : ft.terms) {
                // quote words for AND
                translateFulltext(term, buf, true);
            }
        } else {
            String neg;
            if (ft.op == Op.NOTWORD) {
                neg = "-";
            } else { // Op.WORD
                neg = "";
            }
            String word = ft.word.toLowerCase();
            if (ft.isPhrase() || and) {
                buf.add(neg + '"' + word + '"');
            } else {
                buf.add(neg + word);
            }
        }
    }

    @Override
    public Document walkEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        if (isMixinTypes(fieldInfo)) {
            Object right = walkOperand(fieldInfo, rvalue);
            if (!(right instanceof String)) {
                throw new QueryParseException("Invalid EQ rhs: " + rvalue);
            }
            return walkMixinTypes(Collections.singletonList((String) right), true);
        }
        return super.walkEq(fieldInfo, rvalue);
    }

    @Override
    public Document walkNotEq(Operand lvalue, Operand rvalue) {
        FieldInfo fieldInfo = walkReference(lvalue);
        if (isMixinTypes(fieldInfo)) {
            Object right = walkOperand(fieldInfo, rvalue);
            if (!(right instanceof String)) {
                throw new QueryParseException("Invalid NE rhs: " + rvalue);
            }
            return walkMixinTypes(Collections.singletonList((String) right), false);
        }
        return super.walkNotEq(fieldInfo, rvalue);
    }

    @Override
    public Document walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        FieldInfo fieldInfo = walkReference(lvalue);
        if (isMixinTypes(fieldInfo)) {
            Object right = walkOperand(fieldInfo, rvalue);
            if (!(right instanceof List)) {
                throw new QueryParseException("Invalid IN, right hand side must be a list: " + rvalue);
            }
            return walkMixinTypes((List<String>) right, positive);
        }
        return super.walkIn(fieldInfo, rvalue, positive);
    }

    public Document walkStartsWith(Operand lvalue, Operand rvalue) {
        if (!(lvalue instanceof Reference)) {
            throw new QueryParseException("Invalid STARTSWITH query, left hand side must be a property: " + lvalue);
        }
        String name = ((Reference) lvalue).name;
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(
                    "Invalid STARTSWITH query, right hand side must be a literal path: " + rvalue);
        }
        String path = ((StringLiteral) rvalue).value;
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (NXQL.ECM_PATH.equals(name)) {
            return walkStartsWithPath(path);
        } else {
            return walkStartsWithNonPath(lvalue, path);
        }
    }

    protected Document walkStartsWithPath(String path) {
        // resolve path
        String ancestorId = pathResolver.getIdForPath(path);
        if (ancestorId == null) {
            // no such path
            // TODO XXX do better
            return new Document(MONGODB_ID, "__nosuchid__");
        }
        Object bsonAncestorId = converter.serializableToBson(KEY_ANCESTOR_IDS, ancestorId);
        return new Document(KEY_ANCESTOR_IDS, bsonAncestorId);
    }

    protected Document walkStartsWithNonPath(Operand lvalue, String path) {
        FieldInfo fieldInfo = walkReference(lvalue);
        Document eq = newDocumentWithField(fieldInfo, path);
        // escape except alphanumeric and others not needing escaping
        String regex = path.replaceAll("([^a-zA-Z0-9 /])", "\\\\$1");
        Pattern pattern = Pattern.compile(regex + "/.*");
        Document like = newDocumentWithField(fieldInfo, pattern);
        return new Document(QueryOperators.OR, Arrays.asList(eq, like));
    }

    // non-canonical index syntax, for replaceAll
    protected final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+|\\*|\\*\\d+)\\]" // index in brackets
    );

    /**
     * Canonicalizes a Nuxeo-xpath.
     * <p>
     * Replaces {@code a/foo[123]/b} with {@code a/123/b}
     * <p>
     * A star or a star followed by digits can be used instead of just the digits as well.
     *
     * @param xpath the xpath
     * @return the canonicalized xpath.
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

    /**
     * {@inheritDoc}
     * <p>
     * Also strips prefix from unprefixed schemas.
     *
     * <pre>{@code
     * files:files/*1 -> files.
     * }</pre>
     */
    @Override
    protected String getMongoDBPrefix(String prefix) {
        String mongoPrefix = super.getMongoDBPrefix(prefix);
        String first = mongoPrefix.split("\\.")[0];
        int i = first.indexOf(':');
        if (i > 0) {
            // there is a prefix
            Field field = schemaManager.getField(first);
            if (field != null) {
                Type type = field.getDeclaringType();
                if (type instanceof Schema) {
                    Schema schema = (Schema) type;
                    if (StringUtils.isBlank(schema.getNamespace().prefix)) {
                        // schema without prefix, strip it
                        mongoPrefix = mongoPrefix.substring(i + 1);
                    }
                }
            }
        }
        return mongoPrefix;
    }

    @Override
    protected FieldInfo walkReference(String name) {
        String prop = canonicalXPath(name);
        String[] parts = prop.split("/");
        if (prop.startsWith(NXQL.ECM_PREFIX)) {
            if (prop.startsWith(NXQL.ECM_ACL + "/")) {
                return parseACP(prop, parts);
            }
            if (prop.startsWith(NXQL.ECM_TAG)) {
                String queryField = FACETED_TAG + "." + FACETED_TAG_LABEL;
                queryField = stripElemMatchPrefix(queryField);
                return new FieldInfo(prop, prop, queryField, queryField, StringType.INSTANCE);
            }
            // simple field
            String field = DBSSession.convToInternal(prop);
            Type type = DBSSession.getType(field);
            String queryField = converter.keyToBson(field);
            queryField = stripElemMatchPrefix(queryField);
            return new FieldInfo(prop, field, queryField, queryField, type);
        } else {
            String first = parts[0];
            Field field = schemaManager.getField(first);
            if (field == null) {
                if (first.indexOf(':') > -1) {
                    throw new QueryParseException("No such property: " + name);
                }
                // check without prefix
                // TODO precompute this in SchemaManagerImpl
                for (Schema schema : schemaManager.getSchemas()) {
                    if (!StringUtils.isBlank(schema.getNamespace().prefix)) {
                        // schema with prefix, do not consider as candidate
                        continue;
                    }
                    field = schema.getField(first);
                    if (field != null) {
                        break;
                    }
                }
                if (field == null) {
                    throw new QueryParseException("No such property: " + name);
                }
            }
            Type type = field.getType();
            if (PROP_UID_MAJOR_VERSION.equals(prop) || PROP_UID_MINOR_VERSION.equals(prop)
                    || PROP_MAJOR_VERSION.equals(prop) || PROP_MINOR_VERSION.equals(prop)) {
                String fieldName = DBSSession.convToInternal(prop);
                return new FieldInfo(prop, fieldName, fieldName, fieldName, type);
            }

            // canonical name
            parts[0] = field.getName().getPrefixedName();
            // are there wildcards or list indexes?
            List<String> queryFieldParts = new LinkedList<>(); // field for query
            List<String> projectionFieldParts = new LinkedList<>(); // field for projection
            boolean firstPart = true;
            for (String part : parts) {
                if (NumberUtils.isDigits(part)) {
                    // explicit list index
                    queryFieldParts.add(part);
                    type = ((ListType) type).getFieldType();
                } else if (!part.startsWith("*")) {
                    // complex sub-property
                    queryFieldParts.add(part);
                    projectionFieldParts.add(part);
                    if (!firstPart) {
                        // we already computed the type of the first part
                        if (!(type instanceof ComplexType)) {
                            throw new QueryParseException("No such property: " + name);
                        }
                        field = ((ComplexType) type).getField(part);
                        if (field == null) {
                            throw new QueryParseException("No such property: " + name);
                        }
                        type = field.getType();
                    }
                } else {
                    // wildcard
                    if (!(type instanceof ListType)) {
                        throw new QueryParseException("No such property: " + name);
                    }
                    type = ((ListType) type).getFieldType();
                }
                firstPart = false;
            }
            String queryField = StringUtils.join(queryFieldParts, '.');
            String projectionField = StringUtils.join(projectionFieldParts, '.');
            queryField = stripElemMatchPrefix(queryField);
            return new FieldInfo(prop, prop, queryField, projectionField, type);
        }
    }

    protected FieldInfo parseACP(String prop, String[] parts) {
        if (parts.length != 3) {
            throw new QueryParseException("No such property: " + prop);
        }
        String wildcard = parts[1];
        if (NumberUtils.isDigits(wildcard)) {
            throw new QueryParseException("Cannot use explicit index in ACLs: " + prop);
        }
        String last = parts[2];
        String queryField;
        if (NXQL.ECM_ACL_NAME.equals(last)) {
            queryField = KEY_ACP + "." + KEY_ACL_NAME;
        } else {
            String fieldLast = DBSSession.convToInternalAce(last);
            if (fieldLast == null) {
                throw new QueryParseException("No such property: " + prop);
            }
            queryField = KEY_ACP + "." + KEY_ACL + "." + fieldLast;
        }
        Type type = DBSSession.getType(last);
        queryField = stripElemMatchPrefix(queryField);
        return new FieldInfo(prop, prop, queryField, queryField, type);
    }

    protected boolean isMixinTypes(FieldInfo fieldInfo) {
        return fieldInfo.queryField.equals(KEY_MIXIN_TYPES);
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

    /**
     * Matches the mixin types against a list of values.
     * <p>
     * Used for:
     * <ul>
     * <li>ecm:mixinTypes = 'Foo'
     * <li>ecm:mixinTypes != 'Foo'
     * <li>ecm:mixinTypes IN ('Foo', 'Bar')
     * <li>ecm:mixinTypes NOT IN ('Foo', 'Bar')
     * </ul>
     * <p>
     * ecm:mixinTypes IN ('Foo', 'Bar')
     *
     * <pre>
     * { "$or" : [ { "ecm:primaryType" : { "$in" : [ ... types with Foo or Bar ...]}} ,
     *             { "ecm:mixinTypes" : { "$in" : [ "Foo" , "Bar]}}]}
     * </pre>
     *
     * ecm:mixinTypes NOT IN ('Foo', 'Bar')
     * <p>
     *
     * <pre>
     * { "$and" : [ { "ecm:primaryType" : { "$in" : [ ... types without Foo nor Bar ...]}} ,
     *              { "ecm:mixinTypes" : { "$nin" : [ "Foo" , "Bar]}}]}
     * </pre>
     */
    public Document walkMixinTypes(List<String> mixins, boolean include) {
        /*
         * Primary types that match.
         */
        Set<String> matchPrimaryTypes;
        if (include) {
            matchPrimaryTypes = new HashSet<>();
            for (String mixin : mixins) {
                matchPrimaryTypes.addAll(getMixinDocumentTypes(mixin));
            }
        } else {
            matchPrimaryTypes = new HashSet<>(getDocumentTypes());
            for (String mixin : mixins) {
                matchPrimaryTypes.removeAll(getMixinDocumentTypes(mixin));
            }
        }
        /*
         * Instance mixins that match.
         */
        Set<String> matchMixinTypes = new HashSet<>();
        for (String mixin : mixins) {
            if (!isNeverPerInstanceMixin(mixin)) {
                matchMixinTypes.add(mixin);
            }
        }
        /*
         * MongoDB query generation.
         */
        // match on primary type
        Document p = new Document(KEY_PRIMARY_TYPE, new Document(QueryOperators.IN, matchPrimaryTypes));
        // match on mixin types
        // $in/$nin with an array matches if any/no element of the array matches
        String innin = include ? QueryOperators.IN : QueryOperators.NIN;
        Document m = new Document(KEY_MIXIN_TYPES, new Document(innin, matchMixinTypes));
        // and/or between those
        String op = include ? QueryOperators.OR : QueryOperators.AND;
        return new Document(op, Arrays.asList(p, m));
    }

}

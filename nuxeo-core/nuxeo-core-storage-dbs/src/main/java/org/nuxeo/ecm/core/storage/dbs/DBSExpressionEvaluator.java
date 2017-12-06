/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MIXIN_TYPES;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_READ_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.PROP_MINOR_VERSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.SelectList;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.runtime.api.Framework;

/**
 * Expression evaluator for a {@link DBSDocument} state.
 *
 * @since 5.9.4
 */
public class DBSExpressionEvaluator extends ExpressionEvaluator {

    private static final Log log = LogFactory.getLog(DBSExpressionEvaluator.class);

    private static final Long ZERO = Long.valueOf(0);

    private static final Long ONE = Long.valueOf(1);

    protected final SelectClause selectClause;

    protected final Expression expression;

    protected final OrderByClause orderByClause;

    protected SchemaManager schemaManager;

    protected List<String> documentTypes;

    protected State state;

    protected boolean parsing;

    /** Info about a value and how to compute it from the toplevel state or an iterator's state. */
    protected static final class ValueInfo {

        /**
         * Traversed steps to compute this value from a state. Traversal steps can be:
         * <ul>
         * <li>String: a map key.
         * <li>Integer: a list element.
         * </ul>
         */
        // also used to temporarily hold the full parsed reference
        public List<Serializable> steps;

        // original NXQL name in query
        public final String nxqlProp;

        public final String canonRef;

        public Type type;

        public boolean isTrueOrNullBoolean;

        public boolean isDateCast;

        /** The value computed for this reference. */
        public Object value;

        public ValueInfo(List<Serializable> steps, String nxqlProp, String canonRef) {
            this.steps = steps;
            this.nxqlProp = nxqlProp;
            this.canonRef = canonRef;
        }

        public Object getValueForEvaluation() {
            if (type instanceof BooleanType) {
                // boolean evaluation is like 0 / 1
                if (isTrueOrNullBoolean) {
                    return TRUE.equals(value) ? ONE : ZERO;
                } else {
                    return value == null ? null : (((Boolean) value).booleanValue() ? ONE : ZERO);
                }
            } else if (isDateCast) {
                if (value == null) {
                    return null;
                } else if (value instanceof Calendar) {
                    return castToDate((Calendar) value);
                } else { // array
                    Object[] array = (Object[]) value;
                    List<Calendar> dates = new ArrayList<>(array.length);
                    for (Object v : array) {
                        v = v instanceof Calendar ? castToDate((Calendar) v) : null;
                        dates.add((Calendar) v);
                    }
                    return dates.toArray();
                }
            } else if (value == null && type instanceof ListType && ((ListType) type).isArray()) {
                // don't use null, as list-based matches don't use ternary logic
                return new Object[0];
            } else {
                return value;
            }
        }

        protected Calendar castToDate(Calendar date) {
            date.set(Calendar.HOUR_OF_DAY, 0);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);
            return date;
        }

        @Override
        public String toString() {
            return "ValueInfo(" + canonRef + " " + steps + " = " + value + ")";
        }
    }

    /**
     * Info about an iterator and how to compute it from a state.
     * <p>
     * The iterator iterates over a list of states or scalars and can be reset to a new list.
     * <p>
     * Also contains information about dependent values and iterators.
     */
    protected static final class IterInfo implements Iterator<Object> {

        /**
         * Traversed steps to compute this iterator list from a state. Traversal steps can be:
         * <ul>
         * <li>String: a map key.
         * <li>Integer: a list element.
         * </ul>
         */
        public final List<Serializable> steps;

        public final List<ValueInfo> dependentValueInfos = new ArrayList<>(2);

        public final List<IterInfo> dependentIterInfos = new ArrayList<>(2);

        protected List<Object> list;

        protected Iterator<Object> it;

        public IterInfo(List<Serializable> steps) {
            this.steps = steps;
        }

        public void setList(Object list) {
            if (list == null) {
                this.list = Collections.emptyList();
            } else if (list instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> stateList = (List<Object>) list;
                this.list = stateList;
            } else {
                this.list = Arrays.asList((Object[]) list);
            }
            reset();
        }

        public void reset() {
            it = list.iterator();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Object next() {
            return it.next();
        }

        @Override
        public String toString() {
            return "IterInfo(" + System.identityHashCode(this) + "," + steps + ")";
        }
    }

    protected static class DBSPathResolver implements PathResolver {
        protected final DBSSession session;

        public DBSPathResolver(DBSSession session) {
            this.session = session;
        }

        @Override
        public String getIdForPath(String path) {
            return session.getDocumentIdByPath(path);
        }
    }

    /** For each encountered reference in traversal order, the corresponding value info. */
    protected List<ValueInfo> referenceValueInfos;

    /** Map of canonical reference to value info. */
    protected Map<String, ValueInfo> canonicalReferenceValueInfos;

    /** Map of canonical reference prefix to iterator. */
    protected Map<String, IterInfo> canonicalPrefixIterInfos;

    /** List of all iterators, in reversed order. */
    protected List<IterInfo> allIterInfos;

    /** The toplevel iterators. */
    protected List<IterInfo> toplevelIterInfos;

    /** The toplevel values, computed without wildcards. */
    protected List<ValueInfo> toplevelValueInfos;

    // correlation to use for each uncorrelated wildcard (negative to avoid collisions with correlated ones)
    protected int uncorrelatedCounter;

    // did we find a wildcard in the SELECT projection or WHERE expression
    protected boolean hasWildcard;

    // which reference index is being visited, reset / updated during each pass
    protected int refCount;

    public DBSExpressionEvaluator(DBSSession session, SelectClause selectClause, Expression expression,
            OrderByClause orderByClause, String[] principals, boolean fulltextSearchDisabled) {
        super(new DBSPathResolver(session), principals, fulltextSearchDisabled);
        this.selectClause = selectClause;
        this.expression = expression;
        this.orderByClause = orderByClause;
    }

    public SelectClause getSelectClause() {
        return selectClause;
    }

    public Expression getExpression() {
        return expression;
    }

    public OrderByClause getOrderByClause() {
        return orderByClause;
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

    protected Set<String> getMixinDocumentTypes(String mixin) {
        Set<String> types = schemaManager.getDocumentTypeNamesForFacet(mixin);
        return types == null ? Collections.emptySet() : types;
    }

    protected boolean isNeverPerInstanceMixin(String mixin) {
        return schemaManager.getNoPerDocumentQueryFacets().contains(mixin);
    }

    /**
     * Initializes parsing datastructures.
     */
    public void parse() {
        schemaManager = Framework.getService(SchemaManager.class);

        referenceValueInfos = new ArrayList<>();
        canonicalReferenceValueInfos = new HashMap<>();
        allIterInfos = new ArrayList<>();
        toplevelIterInfos = new ArrayList<>();
        toplevelValueInfos = new ArrayList<>();
        canonicalPrefixIterInfos = new HashMap<>();

        uncorrelatedCounter = -1;
        hasWildcard = false;

        // we do parsing using the ExpressionEvaluator to be sure that references
        // are visited in the same order as when we'll do actual expression evaluation
        parsing = true;
        walkAll();
        parsing = false;

        // we use all iterators in reversed ordered to increment them lexicographically from the end
        Collections.reverse(allIterInfos);
    }

    /**
     * Returns the projection matches for a given state.
     */
    public List<Map<String, Serializable>> matches(State state) {
        if (!checkSecurity(state)) {
            return Collections.emptyList();
        }
        this.state = state; // needed for mixin types evaluation

        // initializes values and wildcards
        initializeValuesAndIterators(state);

        List<Map<String, Serializable>> matches = new ArrayList<>();
        for (;;) {
            Map<String, Serializable> projection = walkAll();
            if (projection != null) {
                matches.add(projection);
            }
            if (!hasWildcard) {
                // all projections will be the same, get at most one
                break;
            }
            boolean finished = incrementIterators();
            if (finished) {
                break;
            }
        }
        return matches;
    }

    protected boolean checkSecurity(State state) {
        if (principals == null) {
            return true;
        }
        String[] racl = (String[]) state.get(KEY_READ_ACL);
        if (racl == null) {
            log.error("NULL racl for " + state.get(KEY_ID));
            return false;
        }
        for (String user : racl) {
            if (principals.contains(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does one walk of the expression, using the wildcardIndexes currently defined.
     */
    protected Map<String, Serializable> walkAll() {
        refCount = 0;
        Map<String, Serializable> projection = walkSelectClauseAndOrderBy(selectClause, orderByClause);
        Object res = walkExpression(expression);
        if (TRUE.equals(res)) {
            // returns one match
            return projection;
        } else {
            return null;
        }
    }

    /**
     * Walks the select clause and order by clause, and returns the projection.
     */
    public Map<String, Serializable> walkSelectClauseAndOrderBy(SelectClause selectClause,
            OrderByClause orderByClause) {
        Map<String, Serializable> projection = new HashMap<>();
        boolean projectionOnFulltextScore = false;
        boolean sortOnFulltextScore = false;
        SelectList elements = selectClause.getSelectList();
        for (int i = 0; i < elements.size(); i++) {
            Operand op = elements.get(i);
            if (op instanceof Reference) {
                Reference ref = (Reference) op;
                if (ref.name.equals(NXQL.ECM_FULLTEXT_SCORE)) {
                    projectionOnFulltextScore = true;
                }
                addProjection(ref, projection);
            }
        }
        if (orderByClause != null) {
            for (OrderByExpr obe : orderByClause.elements) {
                Reference ref = obe.reference;
                if (ref.name.equals(NXQL.ECM_FULLTEXT_SCORE)) {
                    sortOnFulltextScore = true;
                }
                addProjection(ref, projection);
            }
        }
        if (projectionOnFulltextScore || sortOnFulltextScore) {
            if (!parsing) {
                if (!hasFulltext) {
                    throw new QueryParseException(
                            NXQL.ECM_FULLTEXT_SCORE + " cannot be used without " + NXQL.ECM_FULLTEXT);
                }
                projection.put(NXQL.ECM_FULLTEXT_SCORE, Double.valueOf(1));
            }
        }
        return projection;
    }

    protected void addProjection(Reference ref, Map<String, Serializable> projection) {
        String name = ref.name;
        if (name.equals(NXQL.ECM_PATH)) {
            // ecm:path is special, computed and not stored in database
            if (!parsing) {
                // to compute PATH we need NAME, ID and PARENT_ID for all states
                projection.put(NXQL.ECM_NAME, state.get(KEY_NAME));
                projection.put(NXQL.ECM_UUID, state.get(KEY_ID));
                projection.put(NXQL.ECM_PARENTID, state.get(KEY_PARENT_ID));
            }
            return;
        }
        ValueInfo valueInfo = walkReferenceGetValueInfo(ref);
        if (!parsing) {
            projection.put(valueInfo.nxqlProp, (Serializable) valueInfo.value);
        }
    }

    public boolean hasWildcardProjection() {
        SelectList elements = selectClause.getSelectList();
        for (int i = 0; i < elements.size(); i++) {
            Operand op = elements.get(i);
            if (op instanceof Reference) {
                if (((Reference) op).name.contains("*")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Object walkReference(Reference ref) {
        return walkReferenceGetValueInfo(ref).getValueForEvaluation();
    }

    protected ValueInfo walkReferenceGetValueInfo(Reference ref) {
        if (parsing) {
            ValueInfo valueInfo = parseReference(ref);
            referenceValueInfos.add(valueInfo);
            return valueInfo;
        } else {
            return referenceValueInfos.get(refCount++);
        }
    }

    /**
     * Parses and computes value and iterator information for a reference.
     */
    protected ValueInfo parseReference(Reference ref) {
        ValueInfo parsed = parseReference(ref.name);
        if (DATE_CAST.equals(ref.cast)) {
            Type type = parsed.type;
            if (!(type instanceof DateType
                    || (type instanceof ListType && ((ListType) type).getFieldType() instanceof DateType))) {
                throw new QueryParseException("Cannot cast to " + ref.cast + ": " + ref.name);
            }
            parsed.isDateCast = true;
        }

        ValueInfo valueInfo = canonicalReferenceValueInfos.computeIfAbsent(parsed.canonRef, k -> {
            List<IterInfo> iterInfos = toplevelIterInfos;
            List<ValueInfo> valueInfos = toplevelValueInfos;
            List<String> prefix = new ArrayList<>(3); // canonical prefix
            List<Serializable> steps = new ArrayList<>(1);
            for (Serializable step : parsed.steps) {
                if (step instanceof String) {
                    // complex sub-property
                    prefix.add((String) step);
                    steps.add(step);
                    continue;
                }
                if (step instanceof Integer) {
                    // explicit list index
                    prefix.add(step.toString());
                    steps.add(step);
                    continue;
                }
                // wildcard
                hasWildcard = true;
                prefix.add("*" + step);
                String canonPrefix = StringUtils.join(prefix, '/');
                IterInfo iter = canonicalPrefixIterInfos.get(canonPrefix);
                if (iter == null) {
                    // first time we see this wildcard prefix, use a new iterator
                    iter = new IterInfo(steps);
                    canonicalPrefixIterInfos.put(canonPrefix, iter);
                    allIterInfos.add(iter);
                    iterInfos.add(iter);
                }
                iterInfos = iter.dependentIterInfos;
                valueInfos = iter.dependentValueInfos;
                // reset traversal for next cycle
                steps = new ArrayList<>();
            }
            // truncate traversal to steps since last wildcard, may be empty if referencing wildcard list directly
            parsed.steps = steps;
            valueInfos.add(parsed);
            return parsed;
        });
        return valueInfo;
    }

    /**
     * Gets the canonical reference and parsed reference for this reference name.
     * <p>
     * The parsed reference is a list of components to traverse to get the value:
     * <ul>
     * <li>String = map key
     * <li>Integer = list element
     * <li>Long = wildcard correlation number (pos/neg)
     * </ul>
     *
     * @return the canonical reference (with resolved uncorrelated wildcards)
     */
    protected ValueInfo parseReference(String name) {
        String[] parts = name.split("/");

        // convert first part to internal representation, and canonicalize prefixed schema
        String prop = parts[0];
        Type type;
        boolean isTrueOrNullBoolean;
        if (prop.startsWith(NXQL.ECM_PREFIX)) {
            prop = DBSSession.convToInternal(prop);
            if (prop.equals(KEY_ACP)) {
                return parseACP(parts, name);
            }
            type = DBSSession.getType(prop);
            isTrueOrNullBoolean = true;
        } else {
            Field field = schemaManager.getField(prop);
            if (field == null) {
                if (prop.indexOf(':') > -1) {
                    throw new QueryParseException("No such property: " + name);
                }
                // check without prefix
                // TODO precompute this in SchemaManagerImpl
                for (Schema schema : schemaManager.getSchemas()) {
                    if (!StringUtils.isBlank(schema.getNamespace().prefix)) {
                        // schema with prefix, do not consider as candidate
                        continue;
                    }
                    if (schema != null) {
                        field = schema.getField(prop);
                        if (field != null) {
                            break;
                        }
                    }
                }
                if (field == null) {
                    throw new QueryParseException("No such property: " + name);
                }
            }
            type = field.getType();
            isTrueOrNullBoolean = false;
            prop = field.getName().getPrefixedName();
        }
        parts[0] = prop;

        // canonical prefix used to find shared values (foo/*1 referenced twice always uses the same value)
        List<String> canonParts = new ArrayList<>(parts.length);
        List<Serializable> steps = new ArrayList<>(parts.length);
        boolean firstPart = true;
        for (String part : parts) {
            int c = part.indexOf('[');
            if (c >= 0) {
                // compat xpath foo[123] -> 123
                part = part.substring(c + 1, part.length() - 1);
            }
            Serializable step;
            if (NumberUtils.isDigits(part)) {
                // explicit list index
                step = Integer.valueOf(part);
                type = ((ListType) type).getFieldType();
            } else if (!part.startsWith("*")) {
                // complex sub-property
                step = part;
                if (firstPart) {
                    if (PROP_MAJOR_VERSION.equals(part) || PROP_MINOR_VERSION.equals(part)) {
                        step = DBSSession.convToInternal(part);
                    }
                    // we already computed the type of the first part
                } else {
                    Field field = ((ComplexType) type).getField(part);
                    if (field == null) {
                        throw new QueryParseException("No such property: " + name);
                    }
                    type = field.getType();
                }
            } else {
                // wildcard
                int corr;
                if (part.length() == 1) {
                    // uncorrelated wildcard
                    corr = uncorrelatedCounter--; // negative
                    part = "*" + corr; // unique correlation
                } else {
                    // correlated wildcard, use correlation number
                    String digits = part.substring(1);
                    if (!NumberUtils.isDigits(digits)) {
                        throw new QueryParseException("Invalid wildcard (" + part + ") in property: " + name);
                    }
                    corr = Integer.parseInt(digits);
                    if (corr < 0) {
                        throw new QueryParseException("Invalid wildcard (" + part + ") in property: " + name);
                    }
                }
                step = Long.valueOf(corr);
                type = ((ListType) type).getFieldType();
            }
            canonParts.add(part);
            steps.add(step);
            firstPart = false;
        }
        String canonRef = StringUtils.join(canonParts, '/');
        ValueInfo valueInfo = new ValueInfo(steps, name, canonRef);
        valueInfo.type = type;
        valueInfo.isTrueOrNullBoolean = isTrueOrNullBoolean;
        return valueInfo;
    }

    protected ValueInfo parseACP(String[] parts, String name) {
        if (parts.length != 3) {
            throw new QueryParseException("No such property: " + name);
        }

        String wildcard = parts[1];
        if (NumberUtils.isDigits(wildcard)) {
            throw new QueryParseException("Cannot use explicit index in ACLs: " + name);
        }
        int corr;
        if (wildcard.length() == 1) {
            // uncorrelated wildcard
            corr = uncorrelatedCounter--; // negative
            wildcard = "*" + corr; // unique correlation
        } else {
            // correlated wildcard, use correlation number
            String digits = wildcard.substring(1);
            if (!NumberUtils.isDigits(digits)) {
                throw new QueryParseException("Invalid wildcard (" + wildcard + ") in property: " + name);
            }
            corr = Integer.parseInt(digits);
            if (corr < 0) {
                throw new QueryParseException("Invalid wildcard (" + wildcard + ") in property: " + name);
            }
        }

        String subPart = DBSSession.convToInternalAce(parts[2]);
        if (subPart == null) {
            throw new QueryParseException("No such property: " + name);
        }
        List<Serializable> steps;
        String canonRef;
        if (subPart.equals(KEY_ACL_NAME)) {
            steps = new ArrayList<>(Arrays.asList(KEY_ACP, Long.valueOf(corr), KEY_ACL_NAME));
            canonRef = KEY_ACP + '/' + wildcard + '/' + KEY_ACL_NAME;
        } else {
            // for the second iterator we want a correlation number tied to the first one
            int corr2 = corr * 1000000;
            String wildcard2 = "*" + corr2;
            steps = new ArrayList<>(Arrays.asList(KEY_ACP, Long.valueOf(corr), KEY_ACL, Long.valueOf(corr2), subPart));
            canonRef = KEY_ACP + '/' + wildcard + '/' + KEY_ACL + '/' + wildcard2 + '/' + subPart;
        }
        ValueInfo valueInfo = new ValueInfo(steps, name, canonRef);
        valueInfo.type = DBSSession.getType(subPart);
        valueInfo.isTrueOrNullBoolean = false; // TODO check ok
        return valueInfo;
    }

    /**
     * Initializes toplevel values and iterators for a given state.
     */
    protected void initializeValuesAndIterators(State state) {
        init(state, toplevelValueInfos, toplevelIterInfos);
    }

    /**
     * Initializes values and iterators for a given state.
     */
    protected void init(Object state, List<ValueInfo> valueInfos, List<IterInfo> iterInfos) {
        for (ValueInfo valueInfo : valueInfos) {
            valueInfo.value = traverse(state, valueInfo.steps);
        }
        for (IterInfo iterInfo : iterInfos) {
            Object value = traverse(state, iterInfo.steps);
            iterInfo.setList(value);
            Object iterState = iterInfo.hasNext() ? iterInfo.next() : null;
            init(iterState, iterInfo.dependentValueInfos, iterInfo.dependentIterInfos);
        }
    }

    /**
     * Traverses an object in a series of steps.
     */
    protected Object traverse(Object value, List<Serializable> steps) {
        for (Serializable step : steps) {
            value = traverse(value, step);
        }
        return value;
    }

    /**
     * Traverses a single step.
     */
    protected Object traverse(Object value, Serializable step) {
        if (step instanceof String) {
            // complex sub-property
            if (value != null && !(value instanceof State)) {
                throw new QueryParseException("Invalid property " + step + " (no State but " + value.getClass() + ")");
            }
            return value == null ? null : ((State) value).get((String) step);
        } else if (step instanceof Integer) {
            // explicit list index
            int index = ((Integer) step).intValue();
            if (value == null) {
                return null;
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Serializable> list = (List<Serializable>) value;
                if (index >= list.size()) {
                    return null;
                } else {
                    return list.get(index);
                }
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                if (index >= array.length) {
                    return null;
                } else {
                    return array[index];
                }
            } else {
                throw new QueryParseException(
                        "Invalid property " + step + " (no List/array but " + value.getClass() + ")");
            }
        } else {
            throw new QueryParseException("Invalid step " + step + " (unknown class " + step.getClass() + ")");
        }
    }

    /**
     * Increments iterators lexicographically.
     * <p>
     * Returns {@code true} when all iterations are finished.
     */
    protected boolean incrementIterators() {
        // we iterate on a pre-reversed allIterInfos list as this ensure that
        // dependent iterators are incremented before those that control them
        boolean more = false;
        for (IterInfo iterInfo : allIterInfos) {
            more = iterInfo.hasNext();
            if (!more) {
                // end of this iterator, reset and !more will carry to next one
                iterInfo.reset();
            }
            // get the current value, if any
            Object state = iterInfo.hasNext() ? iterInfo.next() : null;
            // recompute dependent stuff
            init(state, iterInfo.dependentValueInfos, iterInfo.dependentIterInfos);
            if (more) {
                break;
            }
        }
        return !more;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ecm:mixinTypes IN ('Foo', 'Bar')
     * <p>
     * primarytype IN (... types with Foo or Bar ...) OR mixintypes LIKE '%Foo%' OR mixintypes LIKE '%Bar%'
     * <p>
     * ecm:mixinTypes NOT IN ('Foo', 'Bar')
     * <p>
     * primarytype IN (... types without Foo nor Bar ...) AND (mixintypes NOT LIKE '%Foo%' AND mixintypes NOT LIKE
     * '%Bar%' OR mixintypes IS NULL)
     */
    @Override
    public Boolean walkMixinTypes(List<String> mixins, boolean include) {
        if (parsing) {
            return null;
        }
        /*
         * Primary types that match.
         */
        Set<String> matchPrimaryTypes;
        if (include) {
            matchPrimaryTypes = new HashSet<String>();
            for (String mixin : mixins) {
                matchPrimaryTypes.addAll(getMixinDocumentTypes(mixin));
            }
        } else {
            matchPrimaryTypes = new HashSet<String>(getDocumentTypes());
            for (String mixin : mixins) {
                matchPrimaryTypes.removeAll(getMixinDocumentTypes(mixin));
            }
        }
        /*
         * Instance mixins that match.
         */
        Set<String> matchMixinTypes = new HashSet<String>();
        for (String mixin : mixins) {
            if (!isNeverPerInstanceMixin(mixin)) {
                matchMixinTypes.add(mixin);
            }
        }
        /*
         * Evaluation.
         */
        String primaryType = (String) state.get(KEY_PRIMARY_TYPE);
        Object[] mixinTypesArray = (Object[]) state.get(KEY_MIXIN_TYPES);
        List<Object> mixinTypes = mixinTypesArray == null ? Collections.emptyList() : Arrays.asList(mixinTypesArray);
        if (include) {
            // primary types
            if (matchPrimaryTypes.contains(primaryType)) {
                return TRUE;
            }
            // mixin types
            matchMixinTypes.retainAll(mixinTypes); // intersection
            return Boolean.valueOf(!matchMixinTypes.isEmpty());
        } else {
            // primary types
            if (!matchPrimaryTypes.contains(primaryType)) {
                return FALSE;
            }
            // mixin types
            matchMixinTypes.retainAll(mixinTypes); // intersection
            return Boolean.valueOf(matchMixinTypes.isEmpty());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(selectClause);
        sb.append(" WHERE ");
        if (expression instanceof MultiExpression) {
            for (Iterator<Operand> it = ((MultiExpression) expression).values.iterator(); it.hasNext();) {
                Operand operand = it.next();
                sb.append(operand.toString());
                if (it.hasNext()) {
                    sb.append(" AND ");
                }
            }
        } else {
            sb.append(expression);
        }
        if (orderByClause != null) {
            sb.append(" ORDER BY ");
            sb.append(orderByClause);
        }
        return sb.toString();
    }

}

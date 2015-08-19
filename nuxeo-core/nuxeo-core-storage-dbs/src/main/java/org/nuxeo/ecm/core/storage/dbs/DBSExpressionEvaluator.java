/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
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

    protected final SchemaManager schemaManager;

    protected List<String> documentTypes;

    protected State state;

    public DBSExpressionEvaluator(DBSSession session, SelectClause selectClause, Expression expression, String[] principals) {
        super(new DBSPathResolver(session), principals);
        this.selectClause = selectClause;
        this.expression = expression;
        schemaManager = Framework.getLocalService(SchemaManager.class);
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
        return schemaManager.getDocumentTypeNamesForFacet(mixin);
    }

    protected boolean isNeverPerInstanceMixin(String mixin) {
        return schemaManager.getNoPerDocumentQueryFacets().contains(mixin);
    }

    public boolean matches(State state) {
        this.state = state;
        // security check
        if (principals != null) {
            String[] racl = (String[]) walkReference(new Reference(NXQL_ECM_READ_ACL));
            if (racl == null) {
                log.error("NULL racl for " + state.get(DBSDocument.KEY_ID));
            } else {
                boolean allowed = false;
                for (String user : racl) {
                    if (principals.contains(user)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    return false;
                }
            }
        }
        return TRUE.equals(walkExpression(expression));
    }

    public boolean matches(DBSDocumentState docState) {
        return matches(docState.getState());
    }

    @Override
    public Object walkReference(Reference ref) {
        return evaluateReference(ref, state);
    }

    /**
     * Evaluates a reference over the given state.
     *
     * @param ref the reference
     * @param map the state representation
     */
    protected Object evaluateReference(Reference ref, State state) {
        String name = ref.name;
        String[] split = name.split("/");
        String prop = split[0];
        boolean isArray;
        boolean isBoolean;
        boolean isTrueOrNullBoolean;
        if (name.startsWith(NXQL.ECM_PREFIX)) {
            prop = DBSSession.convToInternal(name);
            isArray = DBSSession.isArray(prop);
            isBoolean = DBSSession.isBoolean(prop);
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
            prop = field.getName().getPrefixedName();
            Type type = field.getType();
            isArray = type instanceof ListType && ((ListType) type).isArray();
            isBoolean = type instanceof BooleanType;
            isTrueOrNullBoolean = false;
            if (isArray && split[split.length - 1].startsWith("*")) {
                split = Arrays.copyOfRange(split, 0, split.length - 1);
            }
        }
        Serializable value = state.get(prop);
        for (int i = 1; i < split.length; i++) {
            if (value == null) {
                return null;
            }
            if (!(value instanceof State)) {
                throw new QueryParseException("No such property (no State): " + name);
            }
            value = ((State) value).get(split[i]);
        }
        if (value == null && isArray) {
            // don't use null, as list-based matches don't use ternary logic
            value = new Object[0];
        }
        if (isBoolean) {
            // boolean evaluation is like 0 / 1
            if (isTrueOrNullBoolean) {
                value = TRUE.equals(value) ? ONE : ZERO;
            } else {
                value = value == null ? null : (((Boolean) value).booleanValue() ? ONE : ZERO);
            }
        }
        return value;
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
        String primaryType = (String) state.get(DBSDocument.KEY_PRIMARY_TYPE);
        Object[] mixinTypesArray = (Object[]) state.get(DBSDocument.KEY_MIXIN_TYPES);
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

    public static class OrderByComparator implements Comparator<State> {

        protected final OrderByClause orderByClause;

        protected DBSExpressionEvaluator evaluator;

        public OrderByComparator(OrderByClause orderByClause, DBSExpressionEvaluator evaluator) {
            // replace ecm:path with ecm:__path for evaluation
            // (we don't want to allow ecm:path to be usable anywhere else
            // and resolve to a null value)
            OrderByList obl = new OrderByList(null); // stupid constructor
            obl.clear();
            for (OrderByExpr ob : orderByClause.elements) {
                if (ob.reference.name.equals(NXQL.ECM_PATH)) {
                    ob = new OrderByExpr(new Reference(NXQL_ECM_PATH), ob.isDescending);
                }
                obl.add(ob);
            }
            this.orderByClause = new OrderByClause(obl);
            this.evaluator = evaluator;
        }

        @Override
        public int compare(State s1, State s2) {
            for (OrderByExpr ob : orderByClause.elements) {
                Reference ref = ob.reference;
                boolean desc = ob.isDescending;
                int sign = desc ? -1 : 1;
                Object v1 = evaluator.evaluateReference(ref, s1);
                Object v2 = evaluator.evaluateReference(ref, s2);
                if (v1 == null) {
                    return v2 == null ? 0 : -sign;
                } else if (v2 == null) {
                    return sign;
                } else {
                    if (!(v1 instanceof Comparable)) {
                        throw new QueryParseException("Not a comparable: " + v1);
                    }
                    int cmp = ((Comparable<Object>) v1).compareTo(v2);
                    return desc ? -cmp : cmp;
                }
            }
            return 0;
        }
    }

}

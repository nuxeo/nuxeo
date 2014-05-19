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
import java.util.Comparator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker.QueryMakerException;
import org.nuxeo.runtime.api.Framework;

/**
 * Expression evaluator for a {@link DBSDocument} state.
 *
 * @since 5.9.4
 */
public class DBSExpressionEvaluator extends ExpressionEvaluator {

    private static final Long ZERO = Long.valueOf(0);

    private static final Long ONE = Long.valueOf(1);

    protected final DBSSession session;

    protected final Expression expr;

    protected final SchemaManager schemaManager;

    protected Map<String, Serializable> map;

    public DBSExpressionEvaluator(DBSSession session, Expression expr) {
        super(new DBSPathResolver(session));
        this.session = session;
        this.expr = expr;
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

    public boolean matches(Map<String, Serializable> map) {
        this.map = map;
        return TRUE.equals(walkExpression(expr));
    }

    public boolean matches(DBSDocumentState state) {
        return matches(state.getMap());
    }

    @Override
    public Object walkExpression(Expression expr) {
        Operand lvalue = expr.lvalue;
        Reference ref = lvalue instanceof Reference ? (Reference) lvalue : null;
        String name = ref == null ? null : ref.name;
        if (NXQL.ECM_ISPROXY.equals(name)) {
            return walkExpressionIsProxy(expr);
        } else {
            return super.walkExpression(expr);
        }
    }

    protected Boolean walkExpressionIsProxy(Expression node) {
        boolean bool = getBooleanRValue(NXQL.ECM_ISPROXY, node);
        // TODO XXX no proxies for now
        return Boolean.valueOf(bool);
    }

    protected boolean getBooleanRValue(String name, Expression node) {
        if (node.operator != Operator.EQ && node.operator != Operator.NOTEQ) {
            throw new QueryMakerException(name + " requires = or <> operator");
        }
        long v;
        if (!(node.rvalue instanceof IntegerLiteral)
                || ((v = ((IntegerLiteral) node.rvalue).value) != 0 && v != 1)) {
            throw new QueryMakerException(name
                    + " requires literal 0 or 1 as right argument");
        }
        boolean bool = node.operator == Operator.EQ ^ v == 0;
        return bool;
    }

    @Override
    public Object walkReference(Reference ref) {
        return evaluateReference(ref, map);
    }

    @Override
    public Object evaluateReference(Reference ref, Map<String, Serializable> map) {
        String name = ref.name;
        String[] split = name.split("/");
        String prop = split[0];
        boolean isArray;
        if (name.startsWith(NXQL.ECM_PREFIX)) {
            prop = DBSSession.convToInternal(name);
            isArray = DBSSession.isArray(prop);
        } else {
            Field field = schemaManager.getField(prop);
            if (field == null) {
                if (prop.indexOf(':') > -1) {
                    throw new RuntimeException("Unkown property: " + name);
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
                    throw new RuntimeException("Unkown property: " + name);
                }
            }
            prop = field.getName().getPrefixedName();
            isArray = field.getType() instanceof ListType
                    && ((ListType) field.getType()).isArray();
        }
        Serializable value = map.get(prop);
        for (int i = 1; i < split.length; i++) {
            if (value == null) {
                return null;
            }
            if (!(value instanceof Map)) {
                throw new RuntimeException("Unkown property (no map): " + name);
            }
            value = ((Map<String, Serializable>) value).get(split[i]);
        }
        if (value == null && isArray) {
            // don't use null, as list-based matches don't use ternary logic
            value = new Object[0];
        }
        if (value instanceof Boolean) {
            // boolean evaluation is like 0 / 1
            value = ((Boolean) value).booleanValue() ? ONE : ZERO;
        }
        return value;
    }

    public static class OrderByComparator implements
            Comparator<Map<String, Serializable>> {

        protected final OrderByClause orderByClause;

        protected ExpressionEvaluator matcher;

        public OrderByComparator(OrderByClause orderByClause,
                ExpressionEvaluator matcher) {
            this.orderByClause = orderByClause;
            this.matcher = matcher;
        }

        @Override
        public int compare(Map<String, Serializable> m1,
                Map<String, Serializable> m2) {
            for (OrderByExpr ob : orderByClause.elements) {
                Reference ref = ob.reference;
                boolean desc = ob.isDescending;
                int sign = desc ? -1 : 1;
                Object v1 = matcher.evaluateReference(ref, m1);
                Object v2 = matcher.evaluateReference(ref, m2);
                if (v1 == null) {
                    return v2 == null ? 0 : -sign;
                } else if (v2 == null) {
                    return sign;
                } else {
                    if (!(v1 instanceof Comparable)) {
                        throw new RuntimeException("Not a comparable: " + v1);
                    }
                    int cmp = ((Comparable<Object>) v1).compareTo(v2);
                    return desc ? -cmp : cmp;
                }
            }
            return 0;
        }
    }

}

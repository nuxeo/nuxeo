/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.memory;

import static java.lang.Boolean.TRUE;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator;

/**
 * Evaluates an expression on a map.
 *
 * @since 10.3
 */
public class MapExpressionEvaluator extends ExpressionEvaluator {

    protected static final Long ZERO = Long.valueOf(0);

    protected static final Long ONE = Long.valueOf(1);

    protected Map<String, Object> map;

    public MapExpressionEvaluator() {
        super(null, null, true);
    }

    public boolean matchesEntry(Expression expression, Map<String, Object> map) {
        this.map = map;
        Object result = walkExpression(expression);
        return TRUE.equals(result);
    }

    protected QueryParseException unknownProperty(String name) {
        return new QueryParseException("No column: " + name);
    }

    @Override
    public Boolean walkMixinTypes(List<String> mixins, boolean include) {
        throw unknownProperty(NXQL.ECM_MIXINTYPE);
    }

    @Override
    protected Boolean walkEcmFulltext(String name, Operator op, Operand rvalue) {
        throw unknownProperty(NXQL.ECM_FULLTEXT);
    }

    @Override
    protected Boolean walkEcmPath(Operator op, Operand rvalue) {
        throw unknownProperty(NXQL.ECM_PATH);
    }

    @Override
    public Boolean walkStartsWith(Operand lvalue, Operand rvalue) {
        throw new QueryParseException("Cannot use operator: " + Operator.STARTSWITH);
    }

    @Override
    protected Boolean walkAncestorId(Operator op, Operand rvalue) {
        throw unknownProperty(NXQL.ECM_ANCESTORID);
    }

    @Override
    protected Boolean walkIsTrashed(Operator op, Operand rvalue) {
        throw unknownProperty(NXQL.ECM_ISTRASHED);
    }

    @Override
    public Object walkReference(Reference ref) {
        if (ref.cast != null) {
            throw new QueryParseException("Cannot use cast: " + ref);
        }
        Object value = map.get(ref.name);
        if (value instanceof Boolean) {
            value = (((Boolean) value).booleanValue() ? ONE : ZERO);
        }
        return value;
    }

}

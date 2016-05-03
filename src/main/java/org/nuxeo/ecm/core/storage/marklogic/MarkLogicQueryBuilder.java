/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.storage.ExpressionEvaluator.PathResolver;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;

import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;

/**
 * Query builder for a MarkLogic query from an {@link Expression}.
 *
 * @since 8.3
 */
class MarkLogicQueryBuilder {

    private static final Long ZERO = 0L;

    private static final Long ONE = 1L;

    // non-canonical index syntax, for replaceAll
    private final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+|\\*|\\*\\d+)\\]" // index in brackets
    );

    /** Splits foo.*.bar into foo, *, bar and split foo.*1.bar into foo, *1, bar with the last bar part optional */
    protected final static Pattern WILDCARD_SPLIT = Pattern.compile("([^*]*)\\.\\*(\\d*)(?:\\.(.*))?");

    private final StructuredQueryBuilder sqb;

    private final Expression expression;

    private final SelectClause selectClause;

    private final OrderByClause orderByClause;

    private final PathResolver pathResolver;

    private final boolean fulltextSearchDisabled;

    private Document document;

    public MarkLogicQueryBuilder(QueryManager queryManager, Expression expression, SelectClause selectClause,
            OrderByClause orderByClause, PathResolver pathResolver, boolean fulltextSearchDisabled) {
        this.sqb = queryManager.newStructuredQueryBuilder();
        this.expression = expression;
        this.selectClause = selectClause;
        this.orderByClause = orderByClause;
        this.pathResolver = pathResolver;
        this.fulltextSearchDisabled = fulltextSearchDisabled;
    }

    public boolean hasProjectionWildcard() {
        for (int i= 0; i < selectClause.elements.size(); i++) {
            Operand op = selectClause.elements.get(i);
            if (!(op instanceof Reference)) {
                throw new QueryParseException("Projection not supported: " + op);
            }
            if (walkReference(op).hasWildcard()) {
                return true;
            }
        }
        return false;
    }


    public RawQueryDefinition buildQuery() {
        return sqb.build(walkExpression(expression));
    }

    private StructuredQueryDefinition walkExpression(Expression expression) {
        Operator op = expression.operator;
        Operand lvalue = expression.lvalue;
        Operand rvalue = expression.rvalue;
        // TODO handle ref and date cast

        if (op == Operator.STARTSWITH) {
            // walkStartsWith(lvalue, rvalue);
            // } else if (NXQL.ECM_PATH.equals(name)) {
            // walkEcmPath(op, rvalue);
            // } else if (NXQL.ECM_ANCESTORID.equals(name)) {
            // walkAncestorId(op, rvalue);
            // } else if (name != null && name.startsWith(NXQL.ECM_FULLTEXT) && !NXQL.ECM_FULLTEXT_JOBID.equals(name)) {
            // walkEcmFulltext(name, op, rvalue);
        } else if (op == Operator.SUM) {
            throw new UnsupportedOperationException("SUM");
        } else if (op == Operator.SUB) {
            throw new UnsupportedOperationException("SUB");
        } else if (op == Operator.MUL) {
            throw new UnsupportedOperationException("MUL");
        } else if (op == Operator.DIV) {
            throw new UnsupportedOperationException("DIV");
        } else if (op == Operator.LT) {
            // walkLt(lvalue, rvalue);
        } else if (op == Operator.GT) {
            // walkGt(lvalue, rvalue);
        } else if (op == Operator.EQ) {
            return walkEq(lvalue, rvalue);
        } else if (op == Operator.NOTEQ) {
            return walkNotEq(lvalue, rvalue);
        } else if (op == Operator.LTEQ) {
            // walkLtEq(lvalue, rvalue);
        } else if (op == Operator.GTEQ) {
            // walkGtEq(lvalue, rvalue);
        } else if (op == Operator.AND) {
            if (expression instanceof MultiExpression) {
                return walkMultiExpression((MultiExpression) expression);
            } else {
                return walkAnd(lvalue, rvalue);
            }
        } else if (op == Operator.NOT) {
            // walkNot(lvalue);
        } else if (op == Operator.OR) {
            return walkOr(lvalue, rvalue);
        } else if (op == Operator.LIKE) {
            // walkLike(lvalue, rvalue, true, false);
        } else if (op == Operator.ILIKE) {
            // walkLike(lvalue, rvalue, true, true);
        } else if (op == Operator.NOTLIKE) {
            // walkLike(lvalue, rvalue, false, false);
        } else if (op == Operator.NOTILIKE) {
            // walkLike(lvalue, rvalue, false, true);
        } else if (op == Operator.IN) {
            return walkIn(lvalue, rvalue, true);
        } else if (op == Operator.NOTIN) {
            return walkIn(lvalue, rvalue, false);
        } else if (op == Operator.ISNULL) {
            // walkIsNull(lvalue);
        } else if (op == Operator.ISNOTNULL) {
            // walkIsNotNull(lvalue);
        } else if (op == Operator.BETWEEN) {
            // walkBetween(lvalue, rvalue, true);
        } else if (op == Operator.NOTBETWEEN) {
            // walkBetween(lvalue, rvalue, false);
        }
        throw new QueryParseException("Unknown operator: " + op);
    }

    private StructuredQueryDefinition walkEq(Operand lvalue, Operand rvalue) {
        FieldInfo leftInfo = walkReference(lvalue);
        if (leftInfo.isMixinTypes()) {
            if (!(rvalue instanceof StringLiteral)) {
                throw new QueryParseException("Invalid EQ rhs: " + rvalue);
            }
            // TODO walk mixin types.
        }
        return leftInfo.eq((Literal) rvalue);
    }

    private StructuredQueryDefinition walkNotEq(Operand lvalue, Operand rvalue) {
        StructuredQueryDefinition eq = walkEq(lvalue, rvalue);
        return sqb.not(eq);
    }

    private StructuredQueryDefinition walkMultiExpression(MultiExpression expression) {
        return walkAnd(expression.values);
    }

    private StructuredQueryDefinition walkAnd(Operand lvalue, Operand rvalue) {
        return walkAnd(Arrays.asList(lvalue, rvalue));
    }

    private StructuredQueryDefinition walkAnd(List<Operand> values) {
        List<StructuredQueryDefinition> queries = walkOperandToExpression(values);
        if (queries.size() == 1) {
            return queries.get(0);
        }
        return sqb.and(queries.toArray(new StructuredQueryDefinition[queries.size()]));
    }

    private StructuredQueryDefinition walkOr(Operand lvalue, Operand rvalue) {
        return walkOr(Arrays.asList(lvalue, rvalue));
    }

    private StructuredQueryDefinition walkOr(List<Operand> values) {
        List<StructuredQueryDefinition> queries = walkOperandToExpression(values);
        if (queries.size() == 1) {
            return queries.get(0);
        }
        return sqb.or(queries.toArray(new StructuredQueryDefinition[queries.size()]));
    }

    private StructuredQueryDefinition walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        if (!(rvalue instanceof LiteralList)) {
            throw new QueryParseException("Invalid IN, right hand side must be a list: " + rvalue);
        }
        StructuredQueryDefinition[] queries = ((LiteralList) rvalue).stream()
                                                                    .map(literal -> walkEq(lvalue, literal))
                                                                    .toArray(StructuredQueryDefinition[]::new);
        StructuredQueryDefinition orQuery = sqb.or(queries);
        if (positive) {
            return orQuery;
        }
        return sqb.not(orQuery);
    }

    /**
     * Method used to walk on a list of {@link Expression} typed as {@link Operand}.
     */
    private List<StructuredQueryDefinition> walkOperandToExpression(List<Operand> operands) {
        List<StructuredQueryDefinition> queries = new ArrayList<>(operands.size());
        for (Operand operand : operands) {
            if (!(operand instanceof Expression)) {
                throw new IllegalArgumentException("Operand " + operand + "is not an Expression.");
            }
            queries.add(walkExpression((Expression) operand));
        }
        return queries;
    }

    private FieldInfo walkReference(Operand value) {
        if (!(value instanceof Reference)) {
            throw new QueryParseException("Invalid query, left hand side must be a property: " + value);
        }
        return walkReference((Reference) value);
    }

    private FieldInfo walkReference(Reference reference) {
        String name = reference.name;
        String prop = canonicalXPath(name);
        String[] parts = prop.split("/");
        if (prop.startsWith(NXQL.ECM_PREFIX)) {
            if (prop.startsWith(NXQL.ECM_ACL + "/")) {
                // return parseACP(prop, parts);
            }
            String field = DBSSession.convToInternal(prop);
            return new FieldInfo(prop, field);
        }
        throw new IllegalStateException("Not implemented yet");
    }

    /**
     * Canonicalizes a Nuxeo-xpath. Replaces {@code a/foo[123]/b} with {@code a/123/b} A star or a star followed by
     * digits can be used instead of just the digits as well.
     *
     * @param xpath the xpath
     * @return the canonicalized xpath.
     */
    private String canonicalXPath(String xpath) {
        while (xpath.length() > 0 && xpath.charAt(0) == '/') {
            xpath = xpath.substring(1);
        }
        if (xpath.indexOf('[') == -1) {
            return xpath;
        } else {
            return NON_CANON_INDEX.matcher(xpath).replaceAll("$1");
        }
    }

    private class FieldInfo {

        /** NXQL property. */
        private final String prop;

        /** MarkLogic field including wildcards. */
        private final String fullField;

        private final Type type;

        /** Boolean system properties only use TRUE or NULL, not FALSE, so queries must be updated accordingly. */
        private final boolean isTrueOrNullBoolean;

        private final boolean hasWildcard;

        public FieldInfo(String prop, String field) {
            this(prop, field, true);
        }

        public FieldInfo(String prop, String field, boolean isTrueOrNullBoolean) {
            this(prop, field, DBSSession.getType(field), isTrueOrNullBoolean);
        }

        public FieldInfo(String prop, String fullField, Type type, boolean isTrueOrNullBoolean) {
            this.prop = prop;
            this.fullField = fullField;
            this.type = type;
            this.isTrueOrNullBoolean = isTrueOrNullBoolean;
            hasWildcard = WILDCARD_SPLIT.matcher(fullField).matches();
        }

        public boolean isBoolean() {
            return type instanceof BooleanType;
        }

        public boolean isMixinTypes() {
            return fullField.equals(DBSDocument.KEY_MIXIN_TYPES);
        }

        public boolean hasWildcard() {
            return hasWildcard;
        }

        public StructuredQueryDefinition eq(Literal literal) {
            String serializedKey = MarkLogicHelper.serializeKey(fullField);
            Object value = getLiteral(literal);
            String serializedValue = MarkLogicStateSerializer.serializeValue(value);
            return sqb.value(sqb.element(serializedKey), serializedValue);
        }

        private Object getLiteral(Literal literal) {
            Object result;
            if (literal instanceof BooleanLiteral) {
                result = ((BooleanLiteral) literal).value;
            } else if (literal instanceof DateLiteral) {
                result = ((DateLiteral) literal).value;
            } else if (literal instanceof DoubleLiteral) {
                result = ((DoubleLiteral) literal).value;
            } else if (literal instanceof IntegerLiteral) {
                result = ((IntegerLiteral) literal).value;
                if (isBoolean()) {
                    if (ZERO.equals(result)) {
                        result = isTrueOrNullBoolean ? null : false;
                    } else if (ONE.equals(result)) {
                        result = true;
                    } else {
                        throw new QueryParseException("Invalid boolean: " + result);
                    }
                }
            } else if (literal instanceof StringLiteral) {
                result = ((StringLiteral) literal).value;
            } else {
                throw new QueryParseException("Unknown literal: " + literal);
            }
            return result;
        }

    }

}

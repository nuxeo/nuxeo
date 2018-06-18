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
package org.nuxeo.ecm.core.storage;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IN_MIGRATION;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDICATED_PROPERTY;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.BooleanLiteral;
import org.nuxeo.ecm.core.query.sql.model.DateLiteral;
import org.nuxeo.ecm.core.query.sql.model.DoubleLiteral;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.Function;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * Evaluator for an {@link Expression}.
 *
 * @since 5.9.4
 */
public abstract class ExpressionEvaluator {

    /** pseudo NXQL to resolve ancestor ids. */
    public static final String NXQL_ECM_ANCESTOR_IDS = "ecm:__ancestorIds";

    /** pseudo NXQL to resolve internal path. */
    public static final String NXQL_ECM_PATH = "ecm:__path";

    /** pseudo NXQL to resolve read acls. */
    public static final String NXQL_ECM_READ_ACL = "ecm:__read_acl";

    public static final String NXQL_ECM_FULLTEXT_SIMPLE = "ecm:__fulltextSimple";

    public static final String NXQL_ECM_FULLTEXT_BINARY = "ecm:__fulltextBinary";

    protected static final String DATE_CAST = "DATE";

    protected static final String PHRASE_QUOTE = "\"";

    protected static final String NEG_PHRASE_QUOTE = "-\"";

    protected static final String OR = "or";

    /**
     * Interface for a class that knows how to resolve a path into an id.
     */
    public interface PathResolver {
        /**
         * Returns the id for a given path.
         *
         * @param path the path
         * @return the id, or {@code null} if not found
         */
        String getIdForPath(String path);
    }

    public final PathResolver pathResolver;

    public final Set<String> principals;

    public final boolean fulltextSearchDisabled;

    public boolean hasFulltext;

    public ExpressionEvaluator(PathResolver pathResolver, String[] principals, boolean fulltextSearchDisabled) {
        this.pathResolver = pathResolver;
        this.principals = principals == null ? null : new HashSet<>(Arrays.asList(principals));
        this.fulltextSearchDisabled = fulltextSearchDisabled;
    }

    public Object walkExpression(Expression expr) {
        Operator op = expr.operator;
        Operand lvalue = expr.lvalue;
        Operand rvalue = expr.rvalue;
        Reference ref = lvalue instanceof Reference ? (Reference) lvalue : null;
        String name = ref != null ? ref.name : null;
        String cast = ref != null ? ref.cast : null;
        if (DATE_CAST.equals(cast)) {
            checkDateLiteralForCast(rvalue, name);
        }
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
        } else if (op == Operator.SUM) {
            throw new UnsupportedOperationException("SUM");
        } else if (op == Operator.SUB) {
            throw new UnsupportedOperationException("SUB");
        } else if (op == Operator.MUL) {
            throw new UnsupportedOperationException("MUL");
        } else if (op == Operator.DIV) {
            throw new UnsupportedOperationException("DIV");
        } else if (op == Operator.LT) {
            return walkLt(lvalue, rvalue);
        } else if (op == Operator.GT) {
            return walkGt(lvalue, rvalue);
        } else if (op == Operator.EQ) {
            return walkEq(lvalue, rvalue);
        } else if (op == Operator.NOTEQ) {
            return walkNotEq(lvalue, rvalue);
        } else if (op == Operator.LTEQ) {
            return walkLtEq(lvalue, rvalue);
        } else if (op == Operator.GTEQ) {
            return walkGtEq(lvalue, rvalue);
        } else if (op == Operator.AND) {
            if (expr instanceof MultiExpression) {
                return walkMultiExpression((MultiExpression) expr);
            } else {
                return walkAnd(lvalue, rvalue);
            }
        } else if (op == Operator.NOT) {
            return walkNot(lvalue);
        } else if (op == Operator.OR) {
            return walkOr(lvalue, rvalue);
        } else if (op == Operator.LIKE) {
            return walkLike(lvalue, rvalue, true, false);
        } else if (op == Operator.ILIKE) {
            return walkLike(lvalue, rvalue, true, true);
        } else if (op == Operator.NOTLIKE) {
            return walkLike(lvalue, rvalue, false, false);
        } else if (op == Operator.NOTILIKE) {
            return walkLike(lvalue, rvalue, false, true);
        } else if (op == Operator.IN) {
            return walkIn(lvalue, rvalue, true);
        } else if (op == Operator.NOTIN) {
            return walkIn(lvalue, rvalue, false);
        } else if (op == Operator.ISNULL) {
            return walkIsNull(lvalue);
        } else if (op == Operator.ISNOTNULL) {
            return walkIsNotNull(lvalue);
        } else if (op == Operator.BETWEEN) {
            return walkBetween(lvalue, rvalue, true);
        } else if (op == Operator.NOTBETWEEN) {
            return walkBetween(lvalue, rvalue, false);
        } else {
            throw new QueryParseException("Unknown operator: " + op);
        }
    }

    protected void checkDateLiteralForCast(Operand value, String name) {
        if (value instanceof DateLiteral && !((DateLiteral) value).onlyDate) {
            throw new QueryParseException("DATE() cast must be used with DATE literal, not TIMESTAMP: " + name);
        }
    }

    protected Boolean walkEcmPath(Operator op, Operand rvalue) {
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
        Object right = walkReference(new Reference(NXQL.ECM_UUID));
        if (id == null) {
            return FALSE;
        }
        Boolean eq = eq(id, right);
        return op == Operator.EQ ? eq : not(eq);
    }

    protected Boolean walkAncestorId(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires = or <> operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_ANCESTORID + " requires literal id as right argument");
        }
        String ancestorId = ((StringLiteral) rvalue).value;
        Object[] ancestorIds = (Object[]) walkReference(new Reference(NXQL_ECM_ANCESTOR_IDS));
        boolean eq = op == Operator.EQ ? true : false;
        if (ancestorIds == null) {
            // placeless
            return eq ? FALSE : TRUE;
        }
        for (Object id : ancestorIds) {
            if (ancestorId.equals(id)) {
                return eq ? TRUE : FALSE;
            }
        }
        return eq ? FALSE : TRUE;
    }

    protected Boolean walkEcmFulltext(String name, Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.LIKE) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires = or LIKE operator");
        }
        if (!(rvalue instanceof StringLiteral)) {
            throw new QueryParseException(NXQL.ECM_FULLTEXT + " requires literal string as right argument");
        }
        if (fulltextSearchDisabled) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        String query = ((StringLiteral) rvalue).value;
        if (name.equals(NXQL.ECM_FULLTEXT)) {
            // standard fulltext query
            hasFulltext = true;
            String simple = (String) walkReference(new Reference(NXQL_ECM_FULLTEXT_SIMPLE));
            String binary = (String) walkReference(new Reference(NXQL_ECM_FULLTEXT_BINARY));
            return fulltext(simple, binary, query);
        } else {
            // secondary index match with explicit field
            // do a regexp on the field
            if (name.charAt(NXQL.ECM_FULLTEXT.length()) != '.') {
                throw new QueryParseException(name + " has incorrect syntax for a secondary fulltext index");
            }
            String prop = name.substring(NXQL.ECM_FULLTEXT.length() + 1);
            String ft = query.replace(" ", "%");
            rvalue = new StringLiteral(ft);
            return walkLike(new Reference(prop), rvalue, true, true);
        }
    }

    protected Boolean walkIsTrashed(Operator op, Operand rvalue) {
        if (op != Operator.EQ && op != Operator.NOTEQ) {
            throw new QueryParseException(NXQL.ECM_ISTRASHED + " requires = or <> operator");
        }
        TrashService trashService = Framework.getService(TrashService.class);
        if (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            return walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
        } else if (trashService.hasFeature(TRASHED_STATE_IN_MIGRATION)) {
            Boolean lifeCycleTrashed = walkIsTrashed(new Reference(NXQL.ECM_LIFECYCLESTATE), op, rvalue,
                    new StringLiteral(LifeCycleConstants.DELETED_STATE));
            Boolean propertyTrashed = walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue,
                    new IntegerLiteral(1L));
            return or(lifeCycleTrashed, propertyTrashed);
        } else if (trashService.hasFeature(TRASHED_STATE_IS_DEDICATED_PROPERTY)) {
            return walkIsTrashed(new Reference(NXQL.ECM_ISTRASHED), op, rvalue, new IntegerLiteral(1L));
        } else {
            throw new UnsupportedOperationException("TrashService is in an unknown state");
        }
    }

    protected Boolean walkIsTrashed(Reference ref, Operator op, Operand initialRvalue, Literal deletedRvalue) {
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

    public Boolean walkNot(Operand value) {
        return not(bool(walkOperand(value)));
    }

    public Boolean walkIsNull(Operand value) {
        return Boolean.valueOf(walkOperand(value) == null);
    }

    public Boolean walkIsNotNull(Operand value) {
        return Boolean.valueOf(walkOperand(value) != null);
    }

    // ternary logic
    public Boolean walkMultiExpression(MultiExpression expr) {
        Boolean res = TRUE;
        for (Operand value : expr.values) {
            Boolean bool = bool(walkOperand(value));
            // don't short-circuit on null, we want to walk all references deterministically
            res = and(res, bool);
        }
        return res;
    }

    public Boolean walkAnd(Operand lvalue, Operand rvalue) {
        Boolean left = bool(walkOperand(lvalue));
        Boolean right = bool(walkOperand(rvalue));
        return and(left, right);
    }

    public Boolean walkOr(Operand lvalue, Operand rvalue) {
        Boolean left = bool(walkOperand(lvalue));
        Boolean right = bool(walkOperand(rvalue));
        return or(left, right);
    }

    public Boolean walkEq(Operand lvalue, Operand rvalue) {
        Object right = walkOperand(rvalue);
        if (isMixinTypes(lvalue)) {
            if (!(right instanceof String)) {
                throw new QueryParseException("Invalid EQ rhs: " + rvalue);
            }
            return walkMixinTypes(Collections.singletonList((String) right), true);
        }
        Object left = walkOperand(lvalue);
        return eqMaybeList(left, right);
    }

    public Boolean walkNotEq(Operand lvalue, Operand rvalue) {
        if (isMixinTypes(lvalue)) {
            Object right = walkOperand(rvalue);
            if (!(right instanceof String)) {
                throw new QueryParseException("Invalid NE rhs: " + rvalue);
            }
            return walkMixinTypes(Collections.singletonList((String) right), false);
        }
        return not(walkEq(lvalue, rvalue));
    }

    public Boolean walkLt(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp < 0;
    }

    public Boolean walkGt(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp > 0;
    }

    public Boolean walkLtEq(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp <= 0;
    }

    public Boolean walkGtEq(Operand lvalue, Operand rvalue) {
        Integer cmp = cmp(lvalue, rvalue);
        return cmp == null ? null : cmp >= 0;
    }

    public Object walkBetween(Operand lvalue, Operand rvalue, boolean positive) {
        LiteralList l = (LiteralList) rvalue;
        Predicate va = new Predicate(lvalue, Operator.GTEQ, l.get(0));
        Predicate vb = new Predicate(lvalue, Operator.LTEQ, l.get(1));
        Predicate pred = new Predicate(va, Operator.AND, vb);
        if (!positive) {
            pred = new Predicate(pred, Operator.NOT, null);
        }
        return walkExpression(pred);
    }

    public Boolean walkIn(Operand lvalue, Operand rvalue, boolean positive) {
        Object right = walkOperand(rvalue);
        if (!(right instanceof List)) {
            throw new QueryParseException("Invalid IN rhs: " + rvalue);
        }
        if (isMixinTypes(lvalue)) {
            return walkMixinTypes((List<String>) right, positive);
        }
        Object left = walkOperand(lvalue);
        Boolean in = inMaybeList(left, (List<Object>) right);
        return positive ? in : not(in);
    }

    public Object walkOperand(Operand op) {
        if (op instanceof Literal) {
            return walkLiteral((Literal) op);
        } else if (op instanceof LiteralList) {
            return walkLiteralList((LiteralList) op);
        } else if (op instanceof Function) {
            return walkFunction((Function) op);
        } else if (op instanceof Expression) {
            return walkExpression((Expression) op);
        } else if (op instanceof Reference) {
            return walkReference((Reference) op);
        } else {
            throw new QueryParseException("Unknown operand: " + op);
        }
    }

    public Object walkLiteral(Literal lit) {
        if (lit instanceof BooleanLiteral) {
            return walkBooleanLiteral((BooleanLiteral) lit);
        } else if (lit instanceof DateLiteral) {
            return walkDateLiteral((DateLiteral) lit);
        } else if (lit instanceof DoubleLiteral) {
            return walkDoubleLiteral((DoubleLiteral) lit);
        } else if (lit instanceof IntegerLiteral) {
            return walkIntegerLiteral((IntegerLiteral) lit);
        } else if (lit instanceof StringLiteral) {
            return walkStringLiteral((StringLiteral) lit);
        } else {
            throw new QueryParseException("Unknown literal: " + lit);
        }
    }

    public Boolean walkBooleanLiteral(BooleanLiteral lit) {
        return Boolean.valueOf(lit.value);
    }

    public Calendar walkDateLiteral(DateLiteral lit) {
        if (lit.onlyDate) {
            Calendar date = lit.toCalendar();
            if (date != null) {
                date.set(Calendar.HOUR_OF_DAY, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
            }
            return date;
        } else {
            return lit.toCalendar();
        }
    }

    public Double walkDoubleLiteral(DoubleLiteral lit) {
        return Double.valueOf(lit.value);
    }

    public Long walkIntegerLiteral(IntegerLiteral lit) {
        return Long.valueOf(lit.value);
    }

    public String walkStringLiteral(StringLiteral lit) {
        return lit.value;
    }

    public List<Object> walkLiteralList(LiteralList litList) {
        List<Object> list = new ArrayList<>(litList.size());
        for (Literal lit : litList) {
            list.add(walkLiteral(lit));
        }
        return list;
    }

    public Boolean walkLike(Operand lvalue, Operand rvalue, boolean positive, boolean caseInsensitive) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        if (!(right instanceof String)) {
            throw new QueryParseException("Invalid LIKE rhs: " + rvalue);
        }
        return likeMaybeList(left, (String) right, positive, caseInsensitive);
    }

    public Object walkFunction(Function func) {
        throw new UnsupportedOperationException("Function");
    }

    public Boolean walkStartsWith(Operand lvalue, Operand rvalue) {
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

    protected Boolean walkStartsWithPath(String path) {
        // resolve path
        String ancestorId = pathResolver.getIdForPath(path);
        // don't return early on null ancestorId, we want to walk all references deterministically
        Object[] ancestorIds = (Object[]) walkReference(new Reference(NXQL_ECM_ANCESTOR_IDS));
        if (ancestorId == null) {
            // no such path
            return FALSE;
        }
        if (ancestorIds == null) {
            // placeless
            return FALSE;
        }
        for (Object id : ancestorIds) {
            if (ancestorId.equals(id)) {
                return TRUE;
            }
        }
        return FALSE;
    }

    protected Boolean walkStartsWithNonPath(Operand lvalue, String path) {
        Object left = walkReference((Reference) lvalue);
        // exact match
        Boolean bool = eqMaybeList(left, path);
        if (TRUE.equals(bool)) {
            return TRUE;
        }
        // prefix match TODO escape % chars
        String pattern = path + "/%";
        return likeMaybeList(left, pattern, true, false);
    }

    /**
     * Evaluates a reference over the context state.
     *
     * @param ref the reference
     */
    public abstract Object walkReference(Reference ref);

    protected boolean isMixinTypes(Operand op) {
        if (!(op instanceof Reference)) {
            return false;
        }
        return ((Reference) op).name.equals(NXQL.ECM_MIXINTYPE);
    }

    protected Boolean bool(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean)) {
            throw new QueryParseException("Not a boolean: " + value);
        }
        return (Boolean) value;
    }

    // ternary logic
    protected Boolean not(Boolean value) {
        if (value == null) {
            return null;
        }
        return !value;
    }

    // ternary logic
    protected Boolean and(Boolean left, Boolean right) {
        if (TRUE.equals(left)) {
            return right;
        } else {
            return left;
        }
    }

    // ternary logic
    protected Boolean or(Boolean left, Boolean right) {
        if (TRUE.equals(left)) {
            return left;
        } else {
            return right;
        }
    }

    // ternary logic
    protected Boolean eq(Object left, Object right) {
        if (left == null || right == null) {
            return null;
        }
        if (left instanceof Calendar && right instanceof Calendar) {
            // avoid timezone issues (NXP-20260)
            return ((Calendar) left).getTimeInMillis() == ((Calendar) right).getTimeInMillis();
        }
        return left.equals(right);
    }

    // ternary logic
    protected Boolean in(Object left, List<Object> right) {
        if (left == null) {
            return null;
        }
        boolean hasNull = false;
        for (Object r : right) {
            if (r == null) {
                hasNull = true;
            } else if (left.equals(r)) {
                return TRUE;
            }
        }
        return hasNull ? null : FALSE;
    }

    protected Integer cmp(Operand lvalue, Operand rvalue) {
        Object left = walkOperand(lvalue);
        Object right = walkOperand(rvalue);
        return cmp(left, right);
    }

    // ternary logic
    protected Integer cmp(Object left, Object right) {
        if (left == null || right == null) {
            return null;
        }
        if (!(left instanceof Comparable)) {
            throw new QueryParseException("Not a comparable: " + left);
        }
        return ((Comparable<Object>) left).compareTo(right);
    }

    // ternary logic
    protected Boolean like(Object left, String right, boolean caseInsensitive) {
        if (left == null || right == null) {
            return null;
        }
        if (!(left instanceof String)) {
            throw new QueryParseException("Invalid LIKE lhs: " + left);
        }
        String value = (String) left;
        if (caseInsensitive) {
            value = value.toLowerCase();
            right = right.toLowerCase();
        }
        String regex = likeToRegex(right);
        boolean match = Pattern.matches(regex.toString(), value);
        return match;
    }

    /**
     * Turns a NXQL LIKE pattern into a regex.
     * <p>
     * % and _ are standard wildcards, and \ escapes them.
     *
     * @since 7.4
     */
    public static String likeToRegex(String like) {
        StringBuilder regex = new StringBuilder();
        char[] chars = like.toCharArray();
        boolean escape = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            boolean escapeNext = false;
            switch (c) {
            case '%':
                if (escape) {
                    regex.append(c);
                } else {
                    regex.append(".*");
                }
                break;
            case '_':
                if (escape) {
                    regex.append(c);
                } else {
                    regex.append(".");
                }
                break;
            case '\\':
                if (escape) {
                    regex.append("\\\\"); // backslash escaped for regexp
                } else {
                    escapeNext = true;
                }
                break;
            default:
                // escape mostly everything just in case
                if (!CharUtils.isAsciiAlphanumeric(c)) {
                    regex.append("\\");
                }
                regex.append(c);
                break;
            }
            escape = escapeNext;
        }
        if (escape) {
            // invalid string terminated by escape character, ignore
        }
        return regex.toString();
    }

    // if list, use EXIST (SELECT 1 FROM left WHERE left.item = right)
    protected Boolean eqMaybeList(Object left, Object right) {
        if (left instanceof Object[]) {
            for (Object l : ((Object[]) left)) {
                Boolean eq = eq(l, right);
                if (TRUE.equals(eq)) {
                    return TRUE;
                }
            }
            return FALSE;
        } else {
            return eq(left, right);
        }
    }

    // if list, use EXIST (SELECT 1 FROM left WHERE left.item IN right)
    protected Boolean inMaybeList(Object left, List<Object> right) {
        if (left instanceof Object[]) {
            for (Object l : ((Object[]) left)) {
                Boolean in = in(l, right);
                if (TRUE.equals(in)) {
                    return TRUE;
                }
            }
            return FALSE;
        } else {
            return in(left, right);
        }
    }

    protected Boolean likeMaybeList(Object left, String right, boolean positive, boolean caseInsensitive) {
        if (left instanceof Object[]) {
            for (Object l : ((Object[]) left)) {
                Boolean like = like(l, right, caseInsensitive);
                if (TRUE.equals(like)) {
                    return Boolean.valueOf(positive);
                }
            }
            return Boolean.valueOf(!positive);
        } else {
            Boolean like = like(left, right, caseInsensitive);
            return positive ? like : not(like);
        }
    }

    /**
     * Matches the mixin types against a list of values.
     * <p>
     * Used for:
     * <ul>
     * <li>ecm:mixinTypes = 'foo'
     * <li>ecm:mixinTypes != 'foo'
     * <li>ecm:mixinTypes IN ('foo', 'bar')
     * <li>ecm:mixinTypes NOT IN ('foo', 'bar')
     * </ul>
     *
     * @param mixins the mixin(s) to match
     * @param include {@code true} for = and IN
     * @since 7.4
     */
    public abstract Boolean walkMixinTypes(List<String> mixins, boolean include);

    /*
     * ----- simple parsing, don't try to be exhaustive -----
     */

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\s\\p{Punct}]+");

    private static final String UNACCENTED = "aaaaaaaceeeeiiii\u00f0nooooo\u00f7ouuuuy\u00fey";

    private static final String STOP_WORDS_STR = "a an are and as at be by for from how " //
            + "i in is it of on or that the this to was what when where who will with " //
            + "car donc est il ils je la le les mais ni nous or ou pour tu un une vous " //
            + "www com net org";

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(StringUtils.split(STOP_WORDS_STR, ' ')));

    /**
     * Checks if the fulltext combination of string1 and string2 matches the query expression.
     */
    protected static Boolean fulltext(String string1, String string2, String queryString) {
        if (queryString == null || (string1 == null && string2 == null)) {
            return null;
        }
        // query
        List<String> query = new ArrayList<>();
        String phrase = null;
        int phraseWordCount = 1;
        int maxPhraseWordCount = 1; // maximum number of words in a phrase
        for (String word : StringUtils.split(queryString.toLowerCase(), ' ')) {
            if (WORD_PATTERN.matcher(word).matches()) {
                continue;
            }
            if (phrase != null) {
                if (word.endsWith(PHRASE_QUOTE)) {
                    phrase += " " + word.substring(0, word.length() - 1);
                    query.add(phrase);
                    phraseWordCount++;
                    if (maxPhraseWordCount < phraseWordCount) {
                        maxPhraseWordCount = phraseWordCount;
                    }
                    phrase = null;
                    phraseWordCount = 1;
                } else {
                    phrase += " " + word;
                    phraseWordCount++;
                }
            } else {
                if (word.startsWith(PHRASE_QUOTE)) {
                    phrase = word.substring(1);
                } else if (word.startsWith(NEG_PHRASE_QUOTE)) {
                    phrase = "-" + word.substring(2);
                } else {
                    if (word.startsWith("+")) {
                        word = word.substring(1);
                    }
                    query.add(word);
                }
            }
        }
        if (query.isEmpty()) {
            return FALSE;
        }
        // fulltext
        Set<String> fulltext = new HashSet<>();
        fulltext.addAll(parseFullText(string1, maxPhraseWordCount));
        fulltext.addAll(parseFullText(string2, maxPhraseWordCount));

        return Boolean.valueOf(fulltext(fulltext, query));
    }

    private static Set<String> parseFullText(String string, int phraseSize) {
        if (string == null) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        Deque<String> phraseWords = new LinkedList<>();
        for (String word : WORD_PATTERN.split(string)) {
            word = parseWord(word);
            if (word != null) {
                word = word.toLowerCase();
                set.add(word);
                if (phraseSize > 1) {
                    phraseWords.addLast(word);
                    if (phraseWords.size() > 1) {
                        if (phraseWords.size() > phraseSize) {
                            phraseWords.removeFirst();
                        }
                        addPhraseWords(set, phraseWords);
                    }
                }
            }
        }
        while (phraseWords.size() > 2) {
            phraseWords.removeFirst();
            addPhraseWords(set, phraseWords);
        }
        return set;
    }

    /**
     * Adds to the set all the sub-phrases from the start of the phraseWords.
     */
    private static void addPhraseWords(Set<String> set, Deque<String> phraseWords) {
        String[] array = phraseWords.toArray(new String[0]);
        for (int len = 2; len <= array.length; len++) {
            String phrase = StringUtils.join(array, ' ', 0, len);
            set.add(phrase);
        }
    }

    private static String parseWord(String string) {
        int len = string.length();
        if (len < 3) {
            return null;
        }
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(string.charAt(i));
            if (c == '\u00e6') {
                buf.append("ae");
            } else if (c >= '\u00e0' && c <= '\u00ff') {
                buf.append(UNACCENTED.charAt((c) - 0xe0));
            } else if (c == '\u0153') {
                buf.append("oe");
            } else {
                buf.append(c);
            }
        }
        // simple heuristic to remove plurals
        int l = buf.length();
        if (l > 3 && buf.charAt(l - 1) == 's') {
            buf.setLength(l - 1);
        }
        String word = buf.toString();
        if (STOP_WORDS.contains(word)) {
            return null;
        }
        return word;
    }

    // matches "foo OR bar baz" as "foo OR (bar AND baz)"
    protected static boolean fulltext(Set<String> fulltext, List<String> query) {
        boolean andMatch = true;
        for (PeekingIterator<String> it = Iterators.peekingIterator(query.iterator()); it.hasNext();) {
            String word = it.next();
            boolean match;
            if (word.endsWith("*") || word.endsWith("%")) {
                // prefix match
                match = false;
                String prefix = word.substring(0, word.length() - 2);
                for (String candidate : fulltext) {
                    if (candidate.startsWith(prefix)) {
                        match = true;
                        break;
                    }
                }
            } else {
                if (word.startsWith("-")) {
                    word = word.substring(1);//
                    match = !fulltext.contains(word);
                } else {
                    match = fulltext.contains(word);
                }
            }
            if (!match) {
                andMatch = false;
            }
            if (it.hasNext() && it.peek().equals(OR)) {
                // end of AND group
                // swallow OR
                it.next();
                // return if the previous AND group matched
                if (andMatch) {
                    return true;
                }
                // else start next AND group
                andMatch = true;
            }
        }
        return andMatch;
    }

    // matches "foo OR bar baz" as "(foo OR bar) AND baz"
    protected static boolean fulltext1(Set<String> fulltext, List<String> query) {
        boolean inOr = false; // if we're in a OR group
        boolean orMatch = false; // value of the OR group
        for (PeekingIterator<String> it = Iterators.peekingIterator(query.iterator()); it.hasNext();) {
            String word = it.next();
            if (it.hasNext() && it.peek().equals(OR)) {
                inOr = true;
                orMatch = false;
            }
            boolean match;
            if (word.endsWith("*") || word.endsWith("%")) {
                // prefix match
                match = false;
                String prefix = word.substring(0, word.length() - 2);
                for (String candidate : fulltext) {
                    if (candidate.startsWith(prefix)) {
                        match = true;
                        break;
                    }
                }
            } else {
                if (word.startsWith("-")) {
                    word = word.substring(1);//
                    match = !fulltext.contains(word);
                } else {
                    match = fulltext.contains(word);
                }
            }
            if (inOr) {
                if (match) {
                    orMatch = true;
                }
                if (it.hasNext() && it.peek().equals(OR)) {
                    // swallow OR and keep going in OR group
                    it.next();
                    continue;
                }
                // finish OR group
                match = orMatch;
                inOr = false;
            }
            if (!match) {
                return false;
            }
        }
        if (inOr) {
            // trailing OR, ignore and finish previous group
            if (!orMatch) {
                return false;
            }
        }
        return true;
    }

}

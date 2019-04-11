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
package org.nuxeo.ecm.directory.multi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.IdentityQueryTransformer;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.directory.multi.MultiDirectorySession.SourceInfo;
import org.nuxeo.ecm.directory.multi.MultiDirectorySession.SubDirectoryInfo;

/**
 * Evaluator for an {@link Expression} in the context of the various subdirectories of a MultiDirectory's source.
 * <p>
 * The result is a set of entry ids.
 * <p>
 * The strategy for evaluation is to delegate as much as possible of the evaluation of expressions to subdirectories
 * themselves.
 * <p>
 * We do a depth-first evaluation of expressions, delaying actual evaluation while an expression's references all fall
 * into the same subdirectory.
 *
 * @since 10.3
 */
public class MultiDirectoryExpressionEvaluator {

    /** The result of an evaluation of an expression. */
    public interface Result {
    }

    /** Result is a set of entry ids. */
    public static class IdsResult implements Result {
        public final Set<String> ids;

        public IdsResult(Set<String> ids) {
            this.ids = ids;
        }
    }

    /** Result is an operand associated to at most one subdirectory. */
    public static class OperandResult implements Result {

        public final Operand operand;

        public final boolean hasId;

        public final String dir; // may be null

        public OperandResult(Operand operand, boolean hasId, String dir) {
            this.operand = operand;
            this.hasId = hasId;
            this.dir = dir;
        }
    }

    protected final List<SubDirectoryInfo> dirInfos;

    protected final String idField;

    protected final String dirName; // for error messages

    public MultiDirectoryExpressionEvaluator(SourceInfo sourceInfo, String idField, String dirName) {
        dirInfos = sourceInfo.subDirectoryInfos;
        this.idField = idField;
        this.dirName = dirName;

    }

    /**
     * Evaluates an expression and returns the set of matching ids.
     */
    public Set<String> eval(Expression expr) {
        return evaluate(evalExpression(expr));
    }

    protected Result evalExpression(Expression expr) {
        Operator op = expr.operator;
        if (expr instanceof MultiExpression) {
            return evalMultiExpression((MultiExpression) expr);
        } else if (op == Operator.AND || op == Operator.OR) {
            return evalAndOr(expr);
        } else {
            return evalSimpleExpression(expr);
        }
    }

    protected Result evalSimpleExpression(Expression expr) {
        Result left = evalOperand(expr.lvalue);
        Result right = evalOperand(expr.rvalue);

        // case where we can return a single-dir operand to the caller
        if (left instanceof OperandResult && right instanceof OperandResult) {
            // check id and subdirectories
            OperandResult lop = (OperandResult) left;
            OperandResult rop = (OperandResult) right;
            if (lop.dir == null || rop.dir == null || lop.dir.equals(rop.dir)) {
                // still one subdirectory
                String dir = lop.dir == null ? rop.dir : lop.dir;
                return new OperandResult(expr, lop.hasId || rop.hasId, dir);
            }
        }

        // else for a simple expression we have no way of doing manual evaluation
        throw new QueryParseException("Invalid expression for multidirectory: " + expr);
    }

    protected Result evalOperand(Operand op) {
        if (op instanceof Expression) {
            return evalExpression((Expression) op);
        } else if (op instanceof Reference) {
            return evalReference((Reference) op);
        } else { // Literal / LiteralList / Function
            return new OperandResult(op, false, null);
        }
    }

    protected Result evalReference(Reference ref) {
        String name = ref.name;
        if (name.equals(idField)) {
            return new OperandResult(ref, true, null);
        }
        for (SubDirectoryInfo dirInfo : dirInfos) {
            if (dirInfo.fromSource.containsKey(name)) {
                return new OperandResult(ref, false, dirInfo.dirName);
            }
        }
        throw new QueryParseException("No column: " + name + " for directory: " + dirName);
    }

    protected Result evalAndOr(Expression expr) {
        List<Predicate> predicates = Arrays.asList((Predicate) expr.lvalue, (Predicate) expr.rvalue);
        return evalMultiExpression(new MultiExpression(expr.operator, predicates));
    }

    protected Result evalMultiExpression(MultiExpression expr) {
        boolean and = expr.operator == Operator.AND;
        List<Predicate> predicates = expr.predicates;
        Iterator<Predicate> it = predicates.iterator();
        if (!it.hasNext()) {
            // empty multiexpression
            return new OperandResult(expr, false, null);
        }
        Result previous = evalExpression(it.next());
        while (it.hasNext()) {
            if (and && previous instanceof IdsResult && ((IdsResult) previous).ids.isEmpty()) {
                // optimization, no need to do more work
                return previous;
            }
            Result next = evalExpression(it.next());

            // check if we can keep a single-dir operand
            if (previous instanceof OperandResult && next instanceof OperandResult) {
                // check id and subdirectories
                OperandResult prv = (OperandResult) previous;
                OperandResult nxt = (OperandResult) next;
                if (prv.dir == null || nxt.dir == null || prv.dir.equals(nxt.dir)) {
                    // still one subdirectory
                    String dir = prv.dir == null ? nxt.dir : prv.dir;
                    previous = new OperandResult(expr, prv.hasId || nxt.hasId, dir);
                    continue;
                }
            }

            // turn everything into ids and do intersection/union
            Set<String> previousIds = evaluate(previous);
            if (and && previousIds.isEmpty()) {
                // optimization, no need to do more work
                return new IdsResult(previousIds);
            }
            Set<String> nextIds = evaluate(next);
            Set<String> ids = and ? intersection(previousIds, nextIds) : union(previousIds, nextIds);
            previous = new IdsResult(ids);
        }
        return previous;
    }

    /**
     * Evaluates a result and returns the set of matching ids.
     */
    protected Set<String> evaluate(Result result) {
        if (result instanceof IdsResult) {
            return ((IdsResult) result).ids;
        } else {
            return evaluate((OperandResult) result);
        }
    }

    /**
     * Evaluates an operand associated to a single directory and returns the set of matching ids.
     */
    protected Set<String> evaluate(OperandResult opr) {
        // find subdirectory to use
        SubDirectoryInfo subDirInfo = null;
        for (SubDirectoryInfo dirInfo : dirInfos) {
            if (opr.dir != null) {
                if (opr.dir.equals(dirInfo.dirName)) {
                    subDirInfo = dirInfo;
                    break;
                }
            } else {
                // expression without any reference (except maybe id), pick any non-optional directory
                if (!dirInfo.isOptional) {
                    subDirInfo = dirInfo;
                    break;
                }
            }
        }
        if (subDirInfo == null) {
            throw new QueryParseException(
                    "Configuration error: no non-optional subdirectory for multidirectory: " + dirName);
        }
        // map field names from multidirectory to subdirectory
        Predicate predicate = new ReferenceRenamer(subDirInfo.fromSource).transform((Predicate) opr.operand);
        QueryBuilder queryBuilder = new QueryBuilder();
        if (predicate instanceof MultiExpression) {
            queryBuilder.filter((MultiExpression) predicate);
        } else {
            queryBuilder.predicate(predicate);
        }
        return new HashSet<>(subDirInfo.getSession().queryIds(queryBuilder));
    }

    /**
     * Renames the references according to a map.
     *
     * @since 10.3
     */
    public static class ReferenceRenamer extends IdentityQueryTransformer {

        protected final Map<String, String> map;

        public ReferenceRenamer(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public Reference transform(Reference node) {
            String name = node.name;
            String newName = map.getOrDefault(name, name);
            if (newName.equals(name)) {
                return node;
            } else {
                return new Reference(newName, node.cast, node.esHint);
            }
        }
    }

    /**
     * Set union.
     */
    protected static Set<String> union(Set<String> a, Set<String> b) {
        if (a.isEmpty()) {
            return Collections.emptySet();
        } else if (b.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<String> set = new HashSet<>(a);
            if (set.addAll(b)) {
                return set;
            } else {
                return a; // optimization, don't return a new set if there was no change
            }
        }
    }

    /**
     * Set intersection.
     */
    protected static Set<String> intersection(Set<String> a, Set<String> b) {
        if (a.isEmpty()) {
            return Collections.emptySet();
        } else if (b.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<String> set = new HashSet<>(a);
            if (set.retainAll(b)) {
                return set;
            } else {
                return a; // optimization, don't return a new set if there was no change
            }
        }
    }

}

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
package org.nuxeo.ecm.core.storage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.FromClause;
import org.nuxeo.ecm.core.query.sql.model.FromList;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.LiteralList;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Generic optimizer for a NXQL query.
 *
 * @since 5.9.4
 */
public class QueryOptimizer {

    public static final String TYPE_ROOT = "Root";

    public static final String TYPE_DOCUMENT = "Document";

    public static final String TYPE_RELATION = "Relation";

    protected final SchemaManager schemaManager;

    protected final Set<String> neverPerInstanceMixins;

    protected final LinkedList<Operand> toplevelOperands;

    /** Do we match only relations? */
    protected boolean onlyRelations;

    public QueryOptimizer() {
        schemaManager = Framework.getLocalService(SchemaManager.class);
        neverPerInstanceMixins = new HashSet<String>(
                schemaManager.getNoPerDocumentQueryFacets());
        toplevelOperands = new LinkedList<Operand>();
    }

    public MultiExpression getOptimizedQuery(SQLQuery query,
            FacetFilter facetFilter) {
        // SELECT * -> SELECT ecm:uuid
        boolean selectStar = query.select.isEmpty();
        if (selectStar) {
            query.select.add(new Reference(NXQL.ECM_UUID));
        }
        if (facetFilter != null) {
            addFacetFilterClauses(facetFilter);
        }
        visitFromClause(query.from); // for primary types
        if (query.where != null) {
            analyzeToplevelOperands(query.where.predicate);
        }
        simplifyToplevelOperands();
        return new MultiExpression(Operator.AND, toplevelOperands);
    }

    protected void addFacetFilterClauses(FacetFilter facetFilter) {
        for (String mixin : facetFilter.required) {
            // every facet is required, not just any of them,
            // so do them one by one
            Expression expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE),
                    Operator.EQ, new StringLiteral(mixin));
            toplevelOperands.add(expr);
        }
        if (!facetFilter.excluded.isEmpty()) {
            LiteralList list = new LiteralList();
            for (String mixin : facetFilter.excluded) {
                list.add(new StringLiteral(mixin));
            }
            Expression expr = new Expression(new Reference(NXQL.ECM_MIXINTYPE),
                    Operator.NOTIN, list);
            toplevelOperands.add(expr);
        }
    }

    /**
     * Finds all the types to take into account (all concrete types being a
     * subtype of the passed types) based on the FROM list.
     * <p>
     * Adds them as a ecm:primaryType match in the toplevel operands.
     */
    protected void visitFromClause(FromClause node) {
        onlyRelations = true;
        Set<String> fromTypes = new HashSet<String>();
        FromList elements = node.elements;
        for (int i = 0; i < elements.size(); i++) {
            String typeName = elements.get(i);
            if (TYPE_DOCUMENT.equalsIgnoreCase(typeName)) {
                typeName = TYPE_DOCUMENT;
            }

            Set<String> subTypes = schemaManager.getDocumentTypeNamesExtending(typeName);
            if (subTypes == null) {
                throw new RuntimeException("Unknown type: " + typeName);
            }
            fromTypes.addAll(subTypes);
            boolean isRelation = false;
            do {
                if (TYPE_RELATION.equals(typeName)) {
                    isRelation = true;
                    break;
                }
                Type t = schemaManager.getDocumentType(typeName);
                if (t != null) {
                    t = t.getSuperType();
                }
                typeName = t == null ? null : t.getName();
            } while (typeName != null);
            onlyRelations = onlyRelations && isRelation;
        }
        fromTypes.remove(TYPE_ROOT);
        LiteralList list = new LiteralList();
        for (String type : fromTypes) {
            list.add(new StringLiteral(type));
        }
        toplevelOperands.add(new Expression(
                new Reference(NXQL.ECM_PRIMARYTYPE), Operator.IN, list));
    }

    /**
     * Expand toplevel ANDed operands into simple list.
     */
    protected void analyzeToplevelOperands(Operand node) {
        if (node instanceof Expression) {
            Expression expr = (Expression) node;
            Operator op = expr.operator;
            if (op == Operator.AND) {
                analyzeToplevelOperands(expr.lvalue);
                analyzeToplevelOperands(expr.rvalue);
                return;
            }
        }
        toplevelOperands.add(node);
    }

    /**
     * Simplify ecm:primaryType positive references, and non-per-instance mixin
     * types.
     */
    protected void simplifyToplevelOperands() {
        Set<String> primaryTypes = null; // if defined, required
        for (Iterator<Operand> it = toplevelOperands.iterator(); it.hasNext();) {
            // whenever we don't know how to optimize the expression,
            // we just continue the loop
            Operand node = it.next();
            if (!(node instanceof Expression)) {
                continue;
            }
            Expression expr = (Expression) node;
            if (!(expr.lvalue instanceof Reference)) {
                continue;
            }
            String name = ((Reference) expr.lvalue).name;
            Operator op = expr.operator;
            Operand rvalue = expr.rvalue;
            if (NXQL.ECM_PRIMARYTYPE.equals(name)) {
                if (op != Operator.EQ && op != Operator.IN) {
                    continue;
                }
                Set<String> set;
                if (op == Operator.EQ) {
                    if (!(rvalue instanceof StringLiteral)) {
                        continue;
                    }
                    String primaryType = ((StringLiteral) rvalue).value;
                    set = new HashSet<String>(
                            Collections.singleton(primaryType));
                } else { // Operator.IN
                    if (!(rvalue instanceof LiteralList)) {
                        continue;
                    }
                    set = getStringLiterals((LiteralList) rvalue);
                }
                if (primaryTypes == null) {
                    primaryTypes = set;
                } else {
                    primaryTypes.retainAll(set);
                }
                it.remove(); // expression simplified into primaryTypes set
            } else if (NXQL.ECM_MIXINTYPE.equals(name)) {
                if (op != Operator.EQ && op != Operator.NOTEQ) {
                    continue;
                }
                if (!(rvalue instanceof StringLiteral)) {
                    continue;
                }
                String mixin = ((StringLiteral) rvalue).value;
                if (!neverPerInstanceMixins.contains(mixin)) {
                    // mixin per instance -> primary type checks not enough
                    continue;
                }
                Set<String> set = schemaManager.getDocumentTypeNamesForFacet(mixin);
                if (set == null) {
                    // unknown mixin
                    set = Collections.emptySet();
                }
                if (primaryTypes == null) {
                    if (op == Operator.EQ) {
                        primaryTypes = new HashSet<String>(set); // copy
                    } else {
                        continue; // unknown positive, no optimization
                    }
                } else {
                    if (op == Operator.EQ) {
                        primaryTypes.retainAll(set);
                    } else {
                        primaryTypes.removeAll(set);
                    }
                }
                it.remove(); // expression simplified into primaryTypes set
            }
        }
        // readd the simplified primary types constraints
        if (primaryTypes != null) {
            if (primaryTypes.isEmpty()) {
                // TODO better removal
                primaryTypes.add("__NOSUCHTYPE__");
            }
            Expression expr;
            if (primaryTypes.size() == 1) {
                String pt = primaryTypes.iterator().next();
                expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE),
                        Operator.EQ, new StringLiteral(pt));
            } else { // primaryTypes.size() > 1
                LiteralList list = new LiteralList();
                for (String pt : primaryTypes) {
                    list.add(new StringLiteral(pt));
                }
                expr = new Expression(new Reference(NXQL.ECM_PRIMARYTYPE),
                        Operator.IN, list);
            }
            toplevelOperands.addFirst(expr);
        }
    }

    protected static Set<String> getStringLiterals(LiteralList list) {
        Set<String> set = new HashSet<String>();
        for (Literal literal : list) {
            if (!(literal instanceof StringLiteral)) {
                throw new RuntimeException("requires string literals");
            }
            set.add(((StringLiteral) literal).value);
        }
        return set;
    }
}
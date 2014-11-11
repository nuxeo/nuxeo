/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql.model;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SelectClause extends Clause {

    private static final long serialVersionUID = -3786932682733679665L;

    public final SelectList elements;
    public final boolean distinct;

    public SelectClause() {
        this(new SelectList(), false);
    }

    public SelectClause(boolean distinct) {
        this(new SelectList(), distinct);
    }

    public SelectClause(SelectList elements) {
        this(elements, false);
    }

    public SelectClause(SelectList elements, boolean distinct) {
        super("SELECT");
        this.elements = elements;
        this.distinct = distinct;
    }


    public void add(String alias, Operand element) {
        elements.add(alias, element);
    }

    public void add(Operand element) {
        elements.add(element.toString(), element);
    }

    public Operand get(String alias) {
        return elements.get(alias);
    }

    public Reference getVariable(String alias) {
        return (Reference) elements.get(alias);
    }

    public Literal getLiteral(String alias) {
        return (Literal) elements.get(alias);
    }

    public Function getFunction(String alias) {
        return (Function) elements.get(alias);
    }

    public Expression getExpression(String alias) {
        return (Expression) elements.get(alias);
    }

    public Operand get(int i) {
        return elements.get(i);
    }

    public String getAlias(int i) {
        return elements.getKey(i);
    }

    public Reference getVariable(int i) {
        return (Reference) elements.get(i);
    }

    public Literal getLiteral(int i) {
        return (Literal) elements.get(i);
    }

    public Function getFunction(int i) {
        return (Function) elements.get(i);
    }

    public Expression getExpression(int i) {
        return (Expression) elements.get(i);
    }

    public boolean isDistinct() {
        return distinct;
    }

    public SelectList getSelectList() {
        return elements;
    }

    public int count() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public void accept(IVisitor visitor) {
        visitor.visitSelectClause(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SelectClause) {
            SelectClause sc = (SelectClause) obj;
            return elements.equals(sc.elements) && (distinct == sc.distinct);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = elements.hashCode();
        result = 31 * result + (distinct ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return (distinct ? "DISTINCT " : "") + elements;
    }

}

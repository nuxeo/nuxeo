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
public class FromClause extends Clause {

    private static final long serialVersionUID = 3563271484181691679L;

    public static final int DOCTYPE = 0;
    public static final int LOCATION = 1;

    public final FromList elements;
    public final int type;


    public FromClause() {
        this(new FromList());
    }

    public FromClause(FromList elements) {
        this(DOCTYPE, elements);
    }

    public FromClause(int type, FromList elements) {
        super("FROM");
        this.elements = elements;
        this.type = type;
    }


    public void add(String alias, String element) {
        elements.add(alias, element);
    }

    public void add(String element) {
        elements.add(element, element);
    }

    public String get(int i) {
        return elements.get(i);
    }

    public String get(String alias) {
        return elements.get(alias);
    }

    public String getAlias(int i) {
        return elements.getKey(i);
    }

    public int count() {
        return elements.size();
    }

    public int getType() {
        return type;
    }

    public void accept(IVisitor visitor) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FromClause) {
            FromClause sc = (FromClause) obj;
            return elements.equals(sc.elements);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return elements.toString();
    }

}

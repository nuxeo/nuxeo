/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.query.sql.model;

import com.google.common.collect.Iterables;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
        elements.put(alias, element);
    }

    public void add(String element) {
        elements.put(element, element);
    }

    public String get(int i) {
        return Iterables.get(elements.values(), i);
    }

    public String get(String alias) {
        return elements.get(alias);
    }

    public String getAlias(int i) {
        return Iterables.get(elements.keySet(), i);
    }

    public int count() {
        return elements.size();
    }

    public int getType() {
        return type;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitFromClause(this);
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

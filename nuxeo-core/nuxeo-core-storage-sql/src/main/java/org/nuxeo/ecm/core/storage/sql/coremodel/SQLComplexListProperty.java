/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLComplexListProperty} gives access to a wrapped collection of
 * SQL-level {@link Node}s.
 *
 * @author Florent Guillaume
 */
public class SQLComplexListProperty extends SQLBaseProperty {

    protected final Node node;

    protected final String name;

    protected final SQLSession session;

    protected final ComplexType elementType;

    /**
     * Creates a {@link SQLComplexListProperty} to wrap a collection of
     * {@link Node}s.
     */
    public SQLComplexListProperty(Node node, ListType type, String name,
            SQLSession session, boolean readonly) {
        super(type, readonly);
        this.node = node;
        this.name = name;
        this.session = session;
        elementType = (ComplexType) type.getFieldType();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public String getName() {
        return node.getName();
    }

    public List<Property> getValue() throws DocumentException {
        return session.makeProperties(node, name, type, readonly, -1);
    }

    public void setValue(Object value) throws DocumentException {
        checkWritable();
        if (value instanceof ListDiff) {
            setList((ListDiff) value);
        } else if (value instanceof List) {
            setList((List<?>) value);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported value object for a complex list: " +
                            value.getClass().getName());
        }
    }

    /*
     * ----- internal -----
     */

    public void setList(List<?> list) throws DocumentException {
        // TODO optimize this to keep existing nodes
        // remove previous nodes
        List<Node> nodes = session.getComplexList(node, name);
        for (Node n : nodes) {
            session.remove(n);
        }
        // add new nodes
        List<Property> properties = session.makeProperties(node, name, type,
                readonly, list.size());
        // set values
        int i = 0;
        for (Object value : list) {
            properties.get(i++).setValue(value);
        }
    }

    public void setList(ListDiff list) throws DocumentException {
        if (!list.isDirty()) {
            return;
        }
        for (ListDiff.Entry entry : list.diff()) {
            switch (entry.type) {
            case ListDiff.ADD:
                // add(entry.value);
                break;
            case ListDiff.REMOVE:
                // remove(entry.index);
                break;
            case ListDiff.INSERT:
                // insert(entry.index, entry.value);
                break;
            case ListDiff.MOVE:
                // move(entry.index, (Integer) entry.value);
                break;
            case ListDiff.MODIFY:
                // modify(entry.index, entry.value);
                break;
            case ListDiff.CLEAR:
                // clear();
                break;
            }
        }
        throw new UnsupportedOperationException();
    }

}

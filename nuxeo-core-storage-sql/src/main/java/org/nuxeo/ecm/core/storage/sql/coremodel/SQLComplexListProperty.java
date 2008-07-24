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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLComplexListProperty} gives access to a wrapped collection of
 * SQL-level {@link Node}s.
 *
 * @author Florent Guillaume
 */
public class SQLComplexListProperty implements Property {

    private static final Log log = LogFactory.getLog(SQLComplexListProperty.class);

    protected final Node node;

    protected final ListType type;

    protected final String name;

    protected final SQLSession session;

    protected final ComplexType elementType;

    /**
     * Creates a {@link SQLComplexListProperty} to wrap a collection of
     * {@link Node}s.
     */
    public SQLComplexListProperty(Node node, ListType type, String name,
            SQLSession session) {
        this.node = node;
        this.type = type;
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

    public Type getType() {
        return type;
    }

    public boolean isNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public List<Property> getValue() throws DocumentException {

        // TODO special case if (elType.getName().equals(TypeConstants.CONTENT))
        // return new BlobProperty(this, element, createField(name));

        List<Node> nodes = session.getComplexList(node, name);
        List<Property> properties = new ArrayList<Property>(nodes.size());
        for (Node node : nodes) {
            Property property = session.makeProperty(node, elementType, name);
            properties.add(property);
        }
        return properties;
    }

    public void setValue(Object value) throws DocumentException {
        // TODO
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
     * ----- Property & PropertyContainer -----
     */

    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> getProperties() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- internal -----
     */

    public void setList(ListDiff list) {
        throw new UnsupportedOperationException();
    }

    public void setList(List<?> list) throws DocumentException {
        throw new UnsupportedOperationException();
    }

}

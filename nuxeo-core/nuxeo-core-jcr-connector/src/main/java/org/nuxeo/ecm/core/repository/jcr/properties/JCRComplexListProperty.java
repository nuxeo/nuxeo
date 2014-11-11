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

package org.nuxeo.ecm.core.repository.jcr.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.uuid.UUID;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.repository.jcr.ModelAdapter;
import org.nuxeo.ecm.core.repository.jcr.NodeConstants;
import org.nuxeo.ecm.core.repository.jcr.TypeAdapter;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeRef;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * The list node is unrestricted.
 * <p>
 * Elements are stored as subnodes having a name generated using the UUID class
 * from Jackrabbit.
 * <p>
 * An auxiliar keys array is stored as a multi string property to map list
 * indexes to sub-nodes names.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JCRComplexListProperty implements Property, JCRNodeProxy {

    protected Node node;

    protected final ListType type; // cached to avoid casts

    protected final JCRNodeProxy parent;

    protected final Field field;

    JCRComplexListProperty(JCRNodeProxy parent, Node node, Field field) {
        type = (ListType) field.getType();
        this.parent = parent;
        this.node = node;
        this.field = field;
    }

    public JCRDocument getDocument() {
        if (parent == null) {
            throw new AssertionError(
                    "JCRNodeProxy for properties must have a non null parent");
        }
        return parent.getDocument();
    }

    public Type getType() throws DocumentException {
        return type;
    }

    public String getName() throws DocumentException {
        return field.getName().getPrefixedName();
    }

    public boolean isEmpty() throws DocumentException {
        if (node == null) {
            return true;
        }
        try {
            return node.hasNodes();
        } catch (Exception e) {
            throw new DocumentException("Failed to test if list is empty", e);
        }
    }

    public Object getValue() throws DocumentException {
        if (node == null) {
            return field.getDefaultValue();
        }
        return getList();
    }

    public void setValue(Object value) throws DocumentException {
        if (node == null) {
            connect();
        }
        if (value instanceof ListDiff) {
            setList((ListDiff) value);
        } else if (value instanceof List) {
            setList((List) value);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported value object for a complex list: " + value);
        }
    }

    public void setNull() throws DocumentException {
        if (node == null) {
            return;
        }
        try {
            node.remove();
        } catch (RepositoryException e) {
            throw new DocumentException("failed to remove property "
                    + field.getName(), e);
        }
    }

    public boolean isNull() throws DocumentException {
        return node == null;
    }

    public Collection<Property> getProperties() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    // ----------- JCRNodeProxy ----------

    public Node getNode() throws DocumentException {
        if (node == null) {
            connect();
        }
        return node;
    }

    public Node connect() throws DocumentException {
        if (parent == null) {
            throw new Error("null property have no parent - should be a bug");
        }
        try {
            if (!parent.isConnected()) {
                parent.connect(); // make sure parent exists
            }
            node = ModelAdapter.addPropertyNode(parent.getNode(),
                    field.getName().getPrefixedName(), type.getName());
        } catch (RepositoryException e) {
            throw new DocumentException("failed to create complex property: "
                    + field.getName(), e);
        }
        return node;
    }

    public boolean isConnected() {
        return node != null;
    }

    public Field getField(String name) {
        return createField(name);
    }

    public Field getField(String schema, String name) {
        throw new UnsupportedOperationException(
                "schema are not supported on list types");
    }

    public ComplexType getSchema(String schema) {
        throw new UnsupportedOperationException(
                "schema are not supported on list types");
    }

    public Collection<Field> getFields() {
        throw new UnsupportedOperationException(
                "fields are not supported on list types");
    }

    protected final Field createField(String fieldName) {
        return new FieldImpl(QName.valueOf(fieldName), TypeRef.NULL, type.getFieldType().getRef());
    }

    public void setList(List list) throws DocumentException {
        NodeIterator it;
        try {
            it = node.getNodes();
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to get list nodes", e);
        }
        // remove old nodes
        try {
            while (it.hasNext()) {
                it.nextNode().remove();
            }
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
        // import new nodes
        int i = 0;
        for (Object value : list) {
            Property property = PropertyFactory.getProperty(this,
                    createField(String.valueOf(i)));
            property.setValue(value);
            i++;
        }
    }

    public void setList(ListDiff list) throws DocumentException {
        // import modifications
        if (!list.isDirty()) {
            return;
        }
        List<String> keys = getKeys();
        ListDiff.Entry[] diff = list.diff();
        for (ListDiff.Entry entry : diff) {
            switch (entry.type) {
            case ListDiff.ADD:
                add(entry.value, keys);
                break;
            case ListDiff.REMOVE:
                remove(entry.index, keys);
                break;
            case ListDiff.INSERT:
                insert(entry.index, entry.value, keys);
                break;
            case ListDiff.MOVE:
                move(entry.index, (Integer) entry.value, keys);
                break;
            case ListDiff.MODIFY:
                modify(entry.index, entry.value, keys);
                break;
            case ListDiff.CLEAR:
                clear(keys);
                break;
            }
        }
        // setKeys(keys);
    }

    public List<Object> getList() throws DocumentException {
        List<Object> list = new ArrayList<Object>();
        try {
            NodeIterator it = node.getNodes();
            while (it.hasNext()) {
                Node item = it.nextNode();
                list.add(getProperty(item.getName(), item).getValue());
            }
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to get complex list", e);
        }
        return list;
    }

    protected final Property getProperty(String name, Node element) {
        Type elType = type.getFieldType();
        if (elType.getName().equals(TypeConstants.CONTENT)) {
            return new BlobProperty(this, element, createField(name));
        } else if (elType.isComplexType()) {
            return new JCRComplexProperty(this, element, createField(name));
        } else if (elType.isListType()) {
            return new JCRComplexListProperty(this, element, createField(name));
        } else if (elType.isSimpleType()) {
            // TODO
        }
        throw new UnsupportedOperationException(
                "Unsupported type for list element: " + type);
    }

    protected final List<String> getKeys() throws DocumentException {
        try {
            NodeIterator it = node.getNodes();
            List<String> keys = new ArrayList<String>();
            while (it.hasNext()) {
                Node item = it.nextNode();
                keys.add(item.getName());
            }
            return keys;
        } catch (PathNotFoundException e) {
            return new ArrayList<String>();
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to retrieve list keys", e);
        }
    }

    @Deprecated
    protected final void setKeys(List<String> keys) throws DocumentException {
        try {
            if (keys == null || keys.isEmpty()) {
                try {
                    javax.jcr.Property prop = node.getProperty(NodeConstants.ECM_LIST_KEYS.rawname);
                    prop.remove();
                } catch (PathNotFoundException e) {
                    // / do nothing
                }
            } else {
                String[] arKeys = keys.toArray(new String[keys.size()]);
                node.setProperty(NodeConstants.ECM_LIST_KEYS.rawname, arKeys);
            }
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to set list keys", e);
        }
    }

    // ----------- list methods ------------

    protected static String generateKey() {
        return UUID.randomUUID().toString();
    }

    protected final Node addNode(String name) throws RepositoryException {
        return node.addNode(name,
                TypeAdapter.fieldType2NodeType(type.getFieldType().getName()));
    }

    protected void add(Object value, List<String> keys)
            throws DocumentException {
        try {
            String name = generateKey();
            Node elem = addNode(name);
            Property property = getProperty(name, elem);
            property.setValue(value);
            // update keys array
            keys.add(name);
        } catch (Exception e) {
            throw new DocumentException("Failed to add list element", e);
        }
    }

    protected void insert(int index, Object value, List<String> keys)
            throws DocumentException {
        try {
            String bname = null;
            if (index >= 0 && index < keys.size()) {
                bname = keys.get(index);
            }
            String name = generateKey();
            Node elem = addNode(name);
            node.orderBefore(name, bname);
            Property property = getProperty(name, elem);
            property.setValue(value);
            keys.add(index, name);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to insert list element", e);
        }
    }

    protected void modify(int index, Object value, List<String> keys)
            throws DocumentException {
        if (index < 0 || index >= keys.size()) {
            throw new IndexOutOfBoundsException();
        }
        try {
            // get next node name
            String bname = null;
            if (index >= 0 && index + 1 < keys.size()) {
                bname = keys.get(index + 1);
            }
            String name = keys.get(index);
            Node cnode = node.getNode(name);
            cnode.remove();
            Node elem = addNode(name);
            if (bname != null) {
                node.orderBefore(name, bname);
            }
            Property property = getProperty(name, elem);
            property.setValue(value);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to modify list element", e);
        }
    }

    // TODO
    protected void move(int fromIndex, int toIndex, List<String> keys)
            throws DocumentException {
        if (fromIndex < 0 || fromIndex >= keys.size()) {
            throw new IndexOutOfBoundsException();
        }
        if (fromIndex == toIndex) {
            return;
        }
        if (toIndex > fromIndex) {
            toIndex++;
        }
        try {
            String from = keys.get(fromIndex);
            String to = null;
            if (toIndex >= 0 && toIndex < keys.size()) {
                to = keys.get(toIndex);
            }
            node.orderBefore(from, to);
            keys.remove(fromIndex);
            if (to == null) {
                keys.add(from);
            } else {
                keys.add(toIndex, from);
            }
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to add list element", e);
        }
    }

    protected void remove(int index, List<String> keys)
            throws DocumentException {
        if (index < 0 || index >= keys.size()) {
            throw new IndexOutOfBoundsException();
        }
        try {
            String name = keys.get(index);
            Node cnode = node.getNode(name);
            cnode.remove();
            keys.remove(index);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to remove list element", e);
        }
    }

    protected void clear(List<String> keys) throws DocumentException {
        try {
            NodeIterator it = node.getNodes();
            List<Node> nodes = new ArrayList<Node>();
            while (it.hasNext()) {
                nodes.add((Node) it.next());
            }
            for (Node node : nodes) {
                node.remove();
            }
            keys.clear();
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to clear list", e);
        }
    }

}

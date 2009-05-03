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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentPartWriter {

    private static final Log log = LogFactory.getLog(DocumentPartWriter.class);

    // Utility class.
    private DocumentPartWriter() {
    }

    public static void writeDocumentPart(JCRDocument doc, DocumentPart dp) throws Exception {
        Node parent = doc.getNode();
        Iterator<Property> it = dp.getDirtyChildren();
        while (it.hasNext()) {
            Property p = it.next();
            writeProperty(parent, p);
        }
        dp.clearDirtyFlags();
    }

    /**
     * Writes the property prop into the given parent node.
     *
     * @param prop the property to write
     * @param parent the parent node where to write the property
     * @return the node corresponding to the given property.
     *     If the property is mapped on a JCR property then the parent node is returned
     * @throws Exception
     */
    public static Node writeProperty(Node parent, Property prop) throws Exception {
        Node node = null;
        if (!prop.isContainer()) {
            node = writeScalarProperty(parent, prop);
        } else if (prop.isList()) {
            node = getOrCreatePropertyNode(parent, prop);
            writeListProperty(node, prop);
        } else { // a complex prop
            node = getOrCreatePropertyNode(parent, prop);
            writeComplexProperty(node, prop);
        }
        // clear dirty status flags since rthe property was stored
        prop.clearDirtyFlags();
        return node;
    }

    /**
     * Writes the scalar property into the given parent node.
     *
     * @param parent
     * @param prop
     * @return the node corresponding to the property.
     *     If the property is mapped on a JCR property then the parent node is returned
     * @throws Exception
     */
    public static Node writeScalarProperty(Node parent, Property prop) throws Exception {
        if (prop.isScalar()) {
            writePrimitiveProperty(parent, prop);
            return parent;
        } else if (prop.isList()) { // for compatibility -> remove this if scalar list mapping is fixed
            writeArrayProperty(parent, prop);
            return parent;
        } else {// a structured property - may be a blob
            Node node = getOrCreatePropertyNode(parent, prop);
            writeStructuredProperty(node, prop);
            return node;
        }
    }

    /**
     * Writes the primitive property into the given parent node.
     *
     * @param parent
     * @param prop
     * @throws Exception
     */
    @SuppressWarnings({"ObjectEquality"})
    public static void writePrimitiveProperty(Node parent, Property prop) throws Exception {
        SimpleType type = ((SimpleType) prop.getType()).getPrimitiveType();
        if (type == StringType.INSTANCE) {
            parent.setProperty(prop.getName(), (String) prop.getValue());
        } else if (type == LongType.INSTANCE || type == IntegerType.INSTANCE) {
            Number value = (Number) prop.getValue();
            long v = value == null ? 0 : value.longValue();
            parent.setProperty(prop.getName(), v);
        } else if (type == DoubleType.INSTANCE) {
            Number value = (Number) prop.getValue();
            double v = value == null ? 0 : value.doubleValue();
            parent.setProperty(prop.getName(), v);
        } else if (type == DateType.INSTANCE) {
            parent.setProperty(prop.getName(), (Calendar) prop.getValue());
        } else if (type == BooleanType.INSTANCE) {
            Boolean value = (Boolean) prop.getValue();
            boolean v = value == null ? false : value.booleanValue();
            parent.setProperty(prop.getName(), v);
        } else if (type == BinaryType.INSTANCE) {
            InputStream in = (InputStream) prop.getValue();
            try {
                parent.setProperty(prop.getName(), in);
            } finally {
                in.close();
            }
        }
    }

    /**
     * Writes the array property into the given parent node.
     *
     * @param parent
     * @param prop
     * @throws Exception
     */
    public static void writeArrayProperty(Node parent, Property prop) throws Exception {
        ValueFactory vf = parent.getSession().getValueFactory();
        Value[] values = toValueArray((ListType) prop.getType(), prop.getValue(), vf);
        parent.setProperty(prop.getName(), values);
    }

    /**
     * Writes the structured property into the corresponding node.
     *
     * @param node
     * @param property
     * @throws Exception
     */
    public static void writeStructuredProperty(Node node, Property property) throws Exception {
        if (!StructuredPropertyManager.write(node, property)) {
            // no accessor defined for this structured property.
            // treat the property as a complex one
            writeComplexProperty(node, property);
        }
    }

    /**
     * Writes content of the complex property to the its corresponding node.
     *
     * @param node
     * @param prop
     * @throws Exception
     */
    public static void writeComplexProperty(Node node, Property prop) throws Exception {
        Iterator<Property> it = prop.getDirtyChildren();
        while (it.hasNext()) {
            Property p = it.next();
            if (p.isRemoved()) {
                removePropertyNode(node, getNodeName(p));
                p.clearDirtyFlags();
            } else {
                writeProperty(node, p);
            }
        }
    }

    /**
     * Writes the list property into the corresponding node.
     *
     * @param node
     * @param prop
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void writeListProperty(Node node, Property prop) throws Exception {
        // check if there are removed properties
        List<String> removed = (List<String>) prop.getData("@removed");
        if (removed != null) { // remove them
            for (String key : removed) {
                if (key != null) {
                    removePropertyNode(node, key);
                }
            }
            // remove app. data
            prop.setData("@removed", null);
        }
        Iterator<Property> it = prop.iterator();
        Property waitOrdering = null;
        while (it.hasNext()) {
            Property p = it.next(); // whether we should order this node
            boolean isMoved = p.isMoved();
            if (p.isNew()) {
                isMoved = true; // this node should be ordered since it is a new node
                String name = UUID.randomUUID().toString();
                p.setData(name);
                // TODO there is a little pb because the getOrCreateNode() must not be used -
                // this may udpate an existing item id generated UUID are not unique
                writeProperty(node, p);
            } else if (p.isModified()) {
                writeProperty(node, p);
            }
            if (waitOrdering != null) {
                String name = getNodeName(waitOrdering);
                String beforeName = getNodeName(p);
                node.orderBefore(name, beforeName);
            }
            waitOrdering = isMoved ? p : null;
        }
        if (waitOrdering != null) { // there are new items that were not ordered - append it
            String name = getNodeName(waitOrdering);
            node.orderBefore(name, null);
        }
    }

    /**
     * Get or create the node corresponding to the given property.
     *
     * @param parent
     * @param prop
     * @return
     */
    public static Node getOrCreatePropertyNode(Node parent, Property prop) throws RepositoryException {
        Node node;
        Type type = prop.getType();
        String name = getNodeName(prop);
        try {
            node = parent.getNode(name);
        } catch (PathNotFoundException e) {
            node = ModelAdapter.addPropertyNode(parent,
                    name, type.getName());
            if (type.isComplexType() && ((ComplexType) type).isUnstructured()) {
                ModelAdapter.setUnstructured(node);
            }
        }
        return node;
    }

    public static void removePropertyNode(Node parent, String name) throws RepositoryException {
        if (name == null) {
            log.error("removePropertyNode was given a null name");
            return;
        }
        try {
            parent.getNode(name).remove();
        } catch (PathNotFoundException e) {
            // property doesn't exists -> do nothing
        }
    }

    public static String getNodeName(Property property) {
        String name = (String) property.getData();
        return name == null ? property.getName() : name;
    }

    public static Value[] toValueArray(ListType type, Object array, ValueFactory vf) {
        if (array == null) {
            return null;
        }
        if (array.getClass().isArray()) {
            return arrayToJCRValues(type, array, vf);
        } else if (array instanceof List) {
            return listToJCRValues(type, (List<?>) array, vf);
        }
        throw new IllegalArgumentException("Unsupported type for setting array values "+array);
    }

    @SuppressWarnings({"ObjectEquality"})
    public static Value[] arrayToJCRValues(ListType type, Object array, ValueFactory vf) {
        Type itemType = type.getFieldType();

        Class<?> ctype = JavaTypes.getClass(itemType);
        if (ctype == String.class) {
            String[] ar = (String[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == int.class) {
            int[] ar = (int[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Integer.class) {
            Integer[] ar = (Integer[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].longValue());
            }
            return values;
        } else if (ctype == long.class) {
            long[] ar = (long[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Long.class) {
            Long[] ar = (Long[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == double.class) {
            double[] ar = (double[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Double.class) {
            Double[] ar = (Double[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == float.class) {
            float[] ar = (float[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Calendar.class) {
            Calendar[] ar = (Calendar[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Date.class) {
            Date[] ar = (Date[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(ar[i]);
                values[i] = vf.createValue(cal);
            }
            return values;
        } else if (ctype == boolean.class) {
            boolean[] ar = (boolean[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Boolean.class) {
            Boolean[] ar = (Boolean[]) array;
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        }
        return null;
    }

    @SuppressWarnings({"ObjectEquality"})
    public static Value[] listToJCRValues(ListType type, List<?> list,
            ValueFactory vf) {
        Type itemType = type.getFieldType();

        Class<?> ctype = JavaTypes.getClass(itemType);
        if (ctype == String.class) {
            String[] ar = list.toArray(new String[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Integer.class) {
            Integer[] ar = list.toArray(new Integer[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].longValue());
            }
            return values;
        } else if (ctype == Long.class) {
            Long[] ar = list.toArray(new Long[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].longValue());
            }
            return values;
        } else if (ctype == Double.class) {
            Double[] ar = list.toArray(new Double[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].doubleValue());
            }
            return values;
        } else if (ctype == Float.class) {
            Float[] ar = list.toArray(new Float[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].doubleValue());
            }
            return values;
        } else if (ctype == Calendar.class) {
            Calendar[] ar = list.toArray(new Calendar[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i]);
            }
            return values;
        } else if (ctype == Date.class) {
            Date[] ar = list.toArray(new Date[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(ar[i]);
                values[i] = vf.createValue(cal);
            }
            return values;
        } else if (ctype == Boolean.class) {
            Boolean[] ar = list.toArray(new Boolean[list.size()]);
            Value[] values = new Value[ar.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = vf.createValue(ar[i].booleanValue());
            }
            return values;
        }
        return null;
    }

}

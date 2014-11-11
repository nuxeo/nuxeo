/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ListProperty extends AbstractProperty implements List<Property> {

    private static final long serialVersionUID = 2050498811068422820L;

    /**
     * The corresponding field.
     */
    protected final Field field;

    protected final List<Property> children;


    public ListProperty(Property parent, Field field) {
        super(parent);
        this.field = field;
        children = new ArrayList<Property>();
    }

    public ListProperty(Property parent, Field field, int flags) {
        super(parent, flags);
        this.field = field;
        children = new ArrayList<Property>();
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
    }

    /**
     * TODO FIXME XXX uncommented <code>return true;</code>
     * see NXP-1653.
     * @see DefaultPropertyFactory line 216
     * @see {@link ListProperty#getValue}
     * @see {@link ListProperty#accept}
     */
    public boolean isContainer() {
        //return true; // - this can be uncommented when scalar list will be fixed
        return !getType().isScalarList();
    }

    public Property add(int index, Object value) throws PropertyException {
        Field lfield = getType().getField();
        Property property = getRoot().createProperty(this, lfield, IS_NEW);
        property.setValue(value);
        children.add(index, property);
        return property;
    }

    public Property add(Object value) throws PropertyException {
        Field lfield = getType().getField();
        Property property = getRoot().createProperty(this, lfield, IS_NEW);
        property.setValue(value);
        children.add(property);
        return property;
    }

    public Property add() {
        Field lfield = getType().getField();
        Property property = getRoot().createProperty(this, lfield, 0);
        children.add(property);
        return property;
    }

    public Collection<Property> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    public String getName() {
        return field.getName().getPrefixedName();
    }

    @SuppressWarnings("unchecked")
    public ListType getType() {
        return (ListType)field.getType();
    }

    public Property get(String name) {
        return children.get(Integer.parseInt(name));
    }

    public Property get(int index) {
        return children.get(index);
    }

    public Property set(String name, Object value)
            throws PropertyException {
        return set(Integer.parseInt(name), value);
    }

    @Override
    protected Serializable getDefaultValue() {
        Serializable value = (Serializable) field.getDefaultValue();
        if (value == null) {
            value = new ArrayList<Serializable>();
        }
        return value;
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        if (children.isEmpty()) {
            return new ArrayList<String>();
        }
        //noinspection CollectionDeclaredAsConcreteClass
        ArrayList<Object> list = new ArrayList<Object>(children.size());
        for (Property property : children) {
            list.add(property.getValue());
        }
// TODO XXX FIXME for compatibility - this treats sclar lists as array
// see NXP-1653. remove this when will be fixed
// see also isContainer(), setValue() and accept()
//        if (getType().isScalarList()) {
//            if (list.isEmpty()) return null;
//            Object o = list.get(0);
//            Class<?> type = o.getClass();
//            if (o == null) {
//                // don't know the class of the element
//                // try to use schema information
//                type = JavaTypes.getPrimitiveClass(getType().getFieldType());
//                if (type == null) { // this should be a bug
//                    throw new IllegalStateException("Scalar list type is not known - this should be a bug");
//                }
//            } else {
//                type = o.getClass();
//            }
//            return list.toArray((Object[])Array.newInstance(type, list.size()));
//        }
        // end of compatibility code <--------
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Serializable value)  throws PropertyException {
        if (value == null) { // IGNORE null values - properties will be considered PHANTOMS
            return;
        }
        List<Serializable> list;
        if (value.getClass().isArray()) { // accept also arrays
            list = (List<Serializable>) PrimitiveArrays.toList(value);
        } else {
            list = (List<Serializable>) value;
        }
        children.clear(); // do not use clear() method since it is marking the list as dirty
        Field lfield = getType().getField();
        for (Serializable obj : list) {
            Property property = getRoot().createProperty(this, lfield,
                    isValidating() ? IS_VALIDATING : 0);
            property.init(obj);
            children.add(property);
        }
        clearFlags(IS_PHANTOM);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object value)  throws PropertyException {
        if (isReadOnly()) {
            throw new ReadOnlyPropertyException(getPath());
        }
        if (value == null) {
            List<Property> temp = new ArrayList<Property>(children);
            for (Property p : temp) { // remove all children
                p.remove();
            }
            return;
        }
        Collection<?> col;
        Class<?> klass = value.getClass();
        if (klass == ListDiff.class) { // listdiff support for compatibility
            applyListDiff((ListDiff) value);
            return;
        } else if (klass.isArray()) { // array support
            col = arrayToList(value);
        } else if (value instanceof Collection) { // collection support
            col = (Collection<?>) value;
        } else {
            throw new InvalidPropertyValueException(getPath());
        }
        clear();
        Field lfield = getType().getField();
        for (Object obj : col) {
            Property property = getRoot().createProperty(this, lfield, IS_NEW);
            property.setValue(obj);
            children.add(property);
        }
    }

    public void clear() {
        while (!children.isEmpty()) {
            Property property  = children.remove(0);
            markChildAsRemoved(property);
        }
        setIsModified();
    }

    public Field getField() {
        return field;
    }

    public boolean remove(Property property) {
        if (!children.remove(property)) { // physically remove the property
            return false; //no such item
        }
        // mark the property as removed using the "@removed" application data
        markChildAsRemoved(property);
        setIsModified();
        return true;
    }

    public Property remove(int index) {
        Property property = children.remove(index);
        // mark the property as removed using the "@removed" application data
        markChildAsRemoved(property);
        setIsModified();
        return property;
    }

    @SuppressWarnings("unchecked")
    private void markChildAsRemoved(Property property) {
        List<String> removed = (List<String>) getData("@removed");
        if (removed == null) {
            removed = new ArrayList<String>();
            setData("@removed", removed);
        }
        // use the key as the property data (this is the internal name of
        // the list item property)
        removed.add((String) property.getData());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ListProperty clone = (ListProperty) super.clone();
        return clone;
    }

    public void accept(PropertyVisitor visitor, Object arg)  throws PropertyException {
        arg = visitor.visit(this, arg);
        if (arg != null && isContainer()) {
            for (Property property : children) {
                property.accept(visitor, arg);
            }
        }
    }

    /** ---------------------------- type conversion ------------------------ */

    @Override
    public boolean isNormalized(Object value) {
        return value == null
                || (value instanceof Collection && value instanceof Serializable);
    }

    @Override
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        if (value.getClass().isArray()) {
            return arrayToList(value);
        }
        throw new PropertyConversionException(value.getClass(), List.class);
    }

    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
        if (value == null) {
            return null;
        } else if (toType.isAssignableFrom(value.getClass())) {
            return toType.cast(value);
        }
        if (toType.isArray()) {
            return (T) ((Collection<?>) value).toArray();
        } else if (toType == List.class || toType == Collection.class) {
            // TODO we need this for compatibility with scalar lists
            if (value.getClass().isArray()) {
                if (value.getClass().isPrimitive()) {
                    return (T) PrimitiveArrays.toList(value);
                } else {
                    return (T) Arrays.asList((Object[]) value);
                }
            }
        }
        return super.convertTo(value, toType);
    }

    // Must return ArrayList
    public static ArrayList<?> arrayToList(Object obj) {
        Object[] ar = PrimitiveArrays.toObjectArray(obj);
        ArrayList<Object> list = new ArrayList<Object>(ar.length);
        list.addAll(Arrays.asList(ar));
        return list;
    }

    /**
     * Supports ListDiff for compatibility.
     *
     * @param ld
     */
    public void applyListDiff(ListDiff ld) throws PropertyException {
        for (ListDiff.Entry entry :  ld.diff()) {
            switch (entry.type) {
            case ListDiff.ADD:
                add(entry.value);
                break;
            case ListDiff.INSERT:
                add(entry.index, entry.value);
                break;
            case ListDiff.REMOVE:
                remove(entry.index);
                break;
            case ListDiff.CLEAR:
                clear();
                break;
            case ListDiff.MODIFY:
                get(entry.index).setValue(entry.value);
                break;
            case ListDiff.MOVE:
                int toIndex = (Integer) entry.value;
                int fromIndex = entry.index;
                Property src = children.get(fromIndex);
                src.moveTo(toIndex);
                break;
            }
        }
    }

    public boolean isSameAs(Property property) throws PropertyException {
        if (!(property instanceof ListProperty)) {
            return false;
        }
        ListProperty lp = (ListProperty) property;
        List<Property> c1 = children;
        List<Property> c2 = lp.children;
        if (c1.size() != c2.size()) {
            return false;
        }
        for (int i=0, size=c1.size(); i<size; i++) {
            Property p1 = c1.get(i);
            Property p2 = c2.get(i);
            if (!p1.isSameAs(p2)) {
                return false;
            }
        }
        return true;
    }

    public Iterator<Property> getDirtyChildren() {
        if (!isContainer()) {
            throw new UnsupportedOperationException(
                    "Cannot iterate over children of scalar properties");
        }
        return new DirtyPropertyIterator(children.iterator());
    }

    public int indexOf(Property property) {
        for (int i=0, size=children.size(); i<size; i++) {
            Property p = children.get(i);
            if (p == property) {
                return i;
            }
        }
        return -1;
    }

    boolean moveTo(Property property, int index) {
        if (index < 0 || index > children.size()) {
            throw new IndexOutOfBoundsException(
                    "Index out of bounds: " + index + ". Bounds are: 0 - " + children.size());
        }
        int i = indexOf(property);
        if (i == -1) {
            throw new UnsupportedOperationException(
                    "You are trying to move a property that is not part of a list");
        }
        if (i == index) {
            return false;
        }
        if (i < index) {
            children.add(index+1, property);
            children.remove(i);
        } else {
            children.add(index, property);
            children.remove(i+1);
        }
        return true;
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public void add(int index, Property element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean add(Property o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean addAll(Collection<? extends Property> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean addAll(int index, Collection<? extends Property> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public ListIterator<Property> listIterator() {
        return children.listIterator();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public ListIterator<Property> listIterator(int index) {
        return children.listIterator(index);
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public Property set(int index, Property element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public List<Property> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        return children.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return children.toArray(a);
    }

}

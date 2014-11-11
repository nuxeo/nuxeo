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

package org.nuxeo.ecm.core.schema.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings({ "SuppressionAnnotation" })
public class ListTypeImpl extends AbstractType implements ListType {

    private static final long serialVersionUID = 6443946870580167862L;

    protected static final String DEFAULT_VALUE_SEPARATOR = " ";

    protected final TypeRef<? extends Type> type;

    protected final Field field;

    //TODO: should be removed. use field.defaultvalue instead
    protected String defaultValue;

    protected int minOccurs;

    protected int maxOccurs;

    protected boolean isArray = false;


    public ListTypeImpl(String schema, String name, TypeRef<? extends Type> type,
            String fieldName, String defaultValue, int minOccurs, int maxOccurs) {
        super(null, schema, name);
        if (fieldName == null) {
            isArray = true;
            fieldName = "item";
        }
        this.type = type;
        field = new FieldImpl(QName.valueOf(fieldName), getRef(), type);
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.defaultValue = defaultValue;
    }

    public ListTypeImpl(String schema, String name, Type type) {
        this(schema, name, type.getRef(), null, null, 0, -1);
    }

    public ListTypeImpl(String schema, String name, Type type, String defaultValue) {
        this(schema, name, type.getRef(), null, defaultValue, 0, -1);
    }

    public ListTypeImpl(String schema, String name, Type type, String fieldName,
            String defaultValue, int minOccurs, int maxOccurs) {
        this(schema, name, type.getRef(), fieldName, defaultValue, minOccurs, maxOccurs);
    }

    public ListTypeImpl(String schema, String name, TypeRef<? extends Type> type) {
        this(schema, name, type, null, null, 0, -1);
    }

    public ListTypeImpl(String schema, String name, TypeRef<? extends Type> type,
            String defaultValue) {
        this(schema, name, type, null, defaultValue, 0, -1);
    }


    public void setLimits(int minOccurs, int maxOccurs) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
    }

    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    public String getFieldName() {
        return field.getName().getLocalName();
    }

    public Type getFieldType() {
        return field.getType();
    }

    public Field getField() {
        return field;
    }

    public Object getDefaultValue() {
        return type.get().decode(defaultValue);
    }

    public Type getType() {
        return type.get();
    }

    public int getMinCount() {
        return minOccurs;
    }

    public int getMaxCount() {
        return maxOccurs;
    }

    @Override
    public boolean isListType() {
        return true;
    }

    @Override
    public Object decode(String string) {
        // XXX: OG: I do not really know how this is suppose to work
        // I need this to decode default values and I could
        // not find how XMLSchema defines default values for sequences thus the
        // following naive splitting of the string representation of the default
        // value
        if (string != null) {
            List<Object> decoded = new ArrayList<Object>();
            Type t = type.get();
            for (String component : string.split(DEFAULT_VALUE_SEPARATOR)) {
                decoded.add(t.decode(component));
            }
            return decoded;
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean validate(Object object) throws TypeException {
        if (object == null && isNotNull()) {
            return false;
        }
        if (object == null) {
            return true;
        }
        if (object instanceof Collection) {
            return validateCollection((Collection) object);
        } else if (object.getClass().isArray()) {
            return validateArray((Object[]) object);
        }
        return false;
    }

    protected boolean validateArray(Object[] array) {
        return true; // TODO
    }

    @SuppressWarnings("unchecked")
    protected boolean validateCollection(Collection col) {
        return true; // TODO
    }

    @Override
    public Object newInstance() {
        Object defaultValue = this.defaultValue;
        if (defaultValue != null) {
            return defaultValue;
        } else {
            // XXX AT: maybe use the type to be more specific on list elements
            return new ArrayList<Object>();
        }
    }

    @SuppressWarnings("unchecked")
    public Object convert(Object object) throws TypeException {
        if (object instanceof List) {
            List list = (List) object;
            Type t = type.get();
            for (int i = 0, len = list.size(); i < len; i++) {
                Object value = list.get(i);
                list.set(i, t.convert(value));
            }
            return object;
        }
        throw new TypeException("Incompatible object: " + object.getClass()
                + " for type " + getName());
    }

    @Override
    public TypeRef<ListType> getRef() {
        return new TypeRef<ListType>(schema, name, this);
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isScalarList() {
        return field.getType().isSimpleType();
    }
}

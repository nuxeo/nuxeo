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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ComplexTypeImpl extends AbstractType implements ComplexType {

    public static final int F_UNSTRUCT_DEFAULT = 0;

    public static final int F_UNSTRUCT_FALSE = 1;

    public static final int F_UNSTRUCT_TRUE = 2;

    private static final long serialVersionUID = 294207320373332155L;

    // fields map by their qname
    protected final Map<QName, Field> fields = new HashMap<QName, Field>();

    // the cache used to lookup fields by string keys
    protected final Map<String, Field> fieldsCache = new HashMap<String, Field>();

    protected final Namespace ns;

    protected int unstructured; // 0 - inherit, 1 - structured, 2 - unstructured

    public ComplexTypeImpl(TypeRef<? extends ComplexType> superType,
            String schema, String name, Namespace ns, int struct) {
        super(superType, schema, name);
        ComplexType stype = (ComplexType) getSuperType();
        if (stype != null) {
            Collection<Field> fields = stype.getFields();
            for (Field field : fields) {
                this.fields.put(field.getName(), field);
            }
        }
        unstructured = struct;
        this.ns = ns;
    }

    public ComplexTypeImpl(ComplexType superType, String schema, String name) {
        this(superType == null ? null : superType.getRef(), schema, name,
                Namespace.DEFAULT_NS, F_UNSTRUCT_DEFAULT);
    }

    public ComplexTypeImpl(ComplexType superType, String schema, String name,
            Namespace ns) {
        this(superType == null ? null : superType.getRef(), schema, name, ns,
                F_UNSTRUCT_DEFAULT);
    }

    public ComplexTypeImpl(TypeRef<? extends ComplexType> superType,
            String schema, String name) {
        this(superType, schema, name, Namespace.DEFAULT_NS, F_UNSTRUCT_DEFAULT);
    }

    public ComplexTypeImpl(TypeRef<? extends ComplexType> superType,
            String schema, String name, Namespace ns) {
        this(superType, schema, name, Namespace.DEFAULT_NS, F_UNSTRUCT_DEFAULT);
    }

    public Namespace getNamespace() {
        return ns;
    }

    public boolean isUnstructured() {
        if (unstructured == F_UNSTRUCT_DEFAULT) {
            unstructured = hasFields() ? F_UNSTRUCT_FALSE : F_UNSTRUCT_TRUE;
        }
        return unstructured == F_UNSTRUCT_TRUE;
    }

    public Field getField(String name) {
        Field field = fieldsCache.get(name);
        if (field == null) {
            QName qname = QName.valueOf(name, ns.prefix);
            field = getField(qname);
            if (field != null) {
                fieldsCache.put(name, field);
            }
        }
        return field;
    }

    public Field getField(QName name) {
        return fields.get(name);
    }

    public Collection<Field> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    public int getFieldsCount() {
        return fields.size();
    }

    public Field addField(String name, TypeRef<? extends Type> type) {
        return addField(QName.valueOf(name, ns.prefix), type, null, 0);
    }

    public Field addField(QName name, TypeRef<? extends Type> type) {
        return addField(name, type, null, 0);
    }

    public Field addField(String name, TypeRef<? extends Type> type,
            String defaultValue, int flags) {
        return addField(QName.valueOf(name, ns.prefix), type, defaultValue,
                flags);
    }

    public Field addField(QName name, TypeRef<? extends Type> type,
            String defaultValue, int flags) {
        FieldImpl field = new FieldImpl(name, getRef(), type, defaultValue,
                flags);
        fields.put(name, field);
        return field;
    }

    public boolean hasField(String name) {
        if (fieldsCache.containsKey(name)) {
            return true;
        }
        return null != getField(name);
    }

    public boolean hasField(QName name) {
        return fields.containsKey(name);
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }

    @Override
    public boolean isComplexType() {
        return true;
    }

    @Override
    public boolean validate(Object object) throws TypeException {
        if (object == null && isNotNull()) {
            return false;
        }
        if (object == null) {
            return true;
        }
        if (object instanceof Map) {
            return validateMap((Map) object);
        }
        return false;
    }

    protected boolean validateMap(Map map) {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

    @Override
    public Map<String, Object> newInstance() {
        if (TypeConstants.isContentType(this)) {
            // NXP-912: should return null for a blob. Since there is no
            // pluggable adapter mechanism on types, and since document model
            // properties consider that every complex property named "content"
            // should be dealt with a BlobProperty, this is hardcoded here.
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : fields.values()) {
            Object value;
            Type type = field.getType();
            if (type.isComplexType()) {
                value = type.newInstance();
            } else if (type.isListType()) {
                value = new ArrayList<Object>();
            } else {
                value = field.getDefaultValue();
            }
            map.put(field.getName().getLocalName(), value);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public Object convert(Object object) throws TypeException {
        if (object instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) object;
            for (Map.Entry entry : map.entrySet()) {
                String key = entry.getKey().toString();
                Field field = getField(key);
                if (field == null) {
                    throw new IllegalArgumentException("Field " + key
                            + " is not defined for the complex type "
                            + getName());
                }
                entry.setValue(field.getType().convert(entry.getValue()));
            }
            return object;
        }
        throw new TypeException("Incompatible object: " + object.getClass()
                + " for type " + this);
    }

    @Override
    public TypeRef<? extends ComplexType> getRef() {
        return new TypeRef<ComplexType>(schema, name, this);
    }
}

/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * A Complex Type holds several fields.
 */
public class ComplexTypeImpl extends AbstractType implements ComplexType {

    private static final long serialVersionUID = 1L;

    /** The fields held by this complex type. */
    protected final Map<QName, Field> fields = new HashMap<>();

    /** The map of name or prefixed name to field. */
    protected volatile Map<String, Field> fieldsByName = new HashMap<>();

    protected final Namespace ns;

    public ComplexTypeImpl(ComplexType superType, String schema, String name, Namespace ns) {
        super(superType, schema, name);
        // for composite types, they already include schemas from supertypes
        // if (superType != null) {
        // for (Field field : superType.getFields()) {
        // addField(field);
        // }
        // }
        this.ns = ns;
    }

    public ComplexTypeImpl(ComplexType superType, String schema, String name) {
        this(superType, schema, name, Namespace.DEFAULT_NS);
    }

    // also called by CompositeTypeImpl
    protected void addField(Field field) {
        QName name = field.getName();
        fields.put(name, field);
        fieldsByName.put(name.getLocalName(), field);
        fieldsByName.put(name.getPrefixedName(), field);
    }

    // called by XSDLoader
    @Override
    public Field addField(String name, Type type, String defaultValue, int flags, Collection<Constraint> constraints) {
        QName qname = QName.valueOf(name, ns.prefix);
        FieldImpl field = new FieldImpl(qname, this, type, defaultValue, flags, constraints);
        addField(field);
        return field;
    }

    @Override
    public Namespace getNamespace() {
        return ns;
    }

    @Override
    public Field getField(String name) {
        return fieldsByName.get(name);
    }

    @Override
    public Field getField(QName name) {
        return fields.get(name);
    }

    @Override
    public Collection<Field> getFields() {
        return fields.values();
    }

    @Override
    public int getFieldsCount() {
        return fields.size();
    }

    @Override
    public boolean hasField(String name) {
        return fieldsByName.containsKey(name);
    }

    @Override
    public boolean hasFields() {
        return !fields.isEmpty();
    }

    @Override
    public boolean isComplexType() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean validate(Object object) throws TypeException {
        throw new UnsupportedOperationException("Unimplemented, use DocumentValidationService");
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
        Map<String, Object> map = new HashMap<>();
        for (Field field : fields.values()) {
            Object value;
            Type type = field.getType();
            if (type.isComplexType()) {
                value = type.newInstance();
            } else if (type.isListType()) {
                value = new ArrayList<>();
            } else {
                value = field.getDefaultValue();
            }
            map.put(field.getName().getLocalName(), value);
        }
        return map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convert(Object object) throws TypeException {
        if (object instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) object;
            for (Entry<Object, Object> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                Field field = getField(key);
                if (field == null) {
                    throw new IllegalArgumentException("Field " + key + " is not defined for the complex type "
                            + getName());
                }
                entry.setValue(field.getType().convert(entry.getValue()));
            }
            return object;
        }
        throw new TypeException("Incompatible object: " + object.getClass() + " for type " + this);
    }

    /**
     * Canonicalizes a Nuxeo-xpath.
     * <p>
     * Replaces {@code a/foo[123]/b} with {@code a/123/b}
     * <p>
     * A star can be used instead of the digits as well (for configuration).
     *
     * @param xpath the xpath
     * @return the canonicalized xpath.
     */
    public static String canonicalXPath(String xpath) {
        while (xpath.length() > 0 && xpath.charAt(0) == '/') {
            xpath = xpath.substring(1);
        }
        if (xpath.indexOf('[') == -1) {
            return xpath;
        } else {
            return xpath.replaceAll("[^/\\[\\]]+\\[(\\d+|\\*)\\]", "$1");
        }
    }

}

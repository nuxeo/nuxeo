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
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeTypeImpl extends ComplexTypeImpl implements CompositeType {

    private static final long serialVersionUID = -6935764237520164300L;

    protected final Map<String, TypeRef<Schema>> schemas = new HashMap<String, TypeRef<Schema>>();

    protected transient Map<String, Schema> prefix2schemas;


    public CompositeTypeImpl(CompositeType superType, String schema, String name,
            String[] schemas) {
        this (superType == null ? null : superType.getRef(), schema, name, schemas);
    }

    public CompositeTypeImpl(TypeRef<? extends CompositeType> superType,
            String schema, String name, String[] schemas) {
        super(superType, schema, name);
        CompositeType stype = (CompositeType) this.superType.get();
        if (stype != null) {
            for (String sname : stype.getSchemaNames()) {
                addSchema(sname);
            }
        }
        if (schemas != null) {
            for (String sname : schemas) {
                addSchema(sname);
            }
        }
    }

    @Override
    public final boolean hasSchemas() {
        return !schemas.isEmpty();
    }

    @Override
    public final void addSchema(String schema) {
        schemas.put(schema, new TypeRef<Schema>(SchemaNames.SCHEMAS, schema));
    }

    @Override
    public final void addSchema(Schema schema) {
        schemas.put(schema.getName(), schema.getRef());
    }

    @Override
    public final Schema getSchema(String name) {
        TypeRef<Schema> proxy = schemas.get(name);
        if (proxy != null) {
            return proxy.get();
        }
        return null;
    }

    @Override
    public final Schema getSchemaByPrefix(String prefix) {
        if (prefix2schemas == null) {
            prefix2schemas = new HashMap<String, Schema>();
            for (Schema schema : getSchemas()) {
                prefix2schemas.put(schema.getNamespace().prefix, schema);
            }
        }
        return prefix2schemas.get(prefix);
    }

    @Override
    public final boolean hasSchema(String name) {
        return schemas.containsKey(name);
    }

    @Override
    public final String[] getSchemaNames() {
        return schemas.keySet().toArray(new String[schemas.size()]);
    }

    @Override
    public final Collection<Schema> getSchemas() {
        List<Schema> schemas = new ArrayList<Schema>();
        for (TypeRef<Schema> proxy : this.schemas.values()) {
            schemas.add(proxy.get());
        }
        return Collections.unmodifiableCollection(schemas);
    }

    @Override
    public final Field addField(QName name, TypeRef<? extends Type> type) {
        throw new UnsupportedOperationException(
                "Cannot add fields to a composite type since it is a composition of other complex types");
    }

    @Override
    public final Field getField(String name) {
        Field field = fieldsCache.get(name);
        if (field == null) {
            for (TypeRef<Schema> schema : schemas.values()) {
                field = schema.get().getField(name);
                if (field != null) {
                    fieldsCache.put(name, field);
                    break;
                }
            }
        }
        return field;
    }

    @Override
    public final Field getField(QName name) {
        String pname = name.prefixedName;
        Field field = fieldsCache.get(pname);
        if (field == null) {
            String prefix = name.prefix;
            if (prefix.length() > 0) {
                Schema schema = getSchemaByPrefix(prefix);
                if (schema != null) {
                    field = schema.getField(name);
                    if (field != null) {
                        fieldsCache.put(pname, field);
                    }
                }
            } else { // try each schema until a field with that local name is
                        // found
                for (TypeRef<Schema> schema : schemas.values()) {
                    field = schema.get().getField(name.localName);
                    if (field != null) {
                        fieldsCache.put(pname, field);
                        break;
                    }
                }
            }
        }
        return field;
    }

    @Override
    public final boolean hasField(QName name) {
        if (fieldsCache.containsKey(name.prefixedName)) {
            return true;
        }
        return getField(name) != null;
    }

    @Override
    public final Collection<Field> getFields() {
        List<Field> fields = new ArrayList<Field>();
        for (TypeRef<Schema> schema : schemas.values()) {
            fields.addAll(schema.get().getFields());
        }
        return fields;
    }

    @Override
    public final boolean isComplexType() {
        return false;
    }

    @Override
    public final boolean isCompositeType() {
        return true;
    }

    @Override
    public final boolean validate(Object object) {
        return true;
    }

    @Override
    public TypeRef<? extends CompositeType> getRef() {
        return new TypeRef<CompositeType>(schema, name, this);
    }

}

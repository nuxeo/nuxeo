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

package org.nuxeo.ecm.core.schema;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeBindingException;
import org.nuxeo.runtime.api.Framework;

/**
 * A proxy to a type.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TypeRef<T extends Type> implements Serializable {

    private static final Log log = LogFactory.getLog(TypeRef.class);

    private static final long serialVersionUID = -6701097679440511374L;

    public static final TypeRef<Type> NULL = new TypeRef<Type>(SchemaNames.BUILTIN,
            "null", null) {
        private static final long serialVersionUID = 1609430705796023481L;

        @Override
        public Type get() {
            return null;
        }
    };

    protected final String schema;
    protected final String name;
    protected transient T object;


    public TypeRef(String schema, String name, T object) {
        this.schema = schema;
        this.name = name;
        this.object = object;
    }

    public TypeRef(String schema, String name) {
        this.schema = schema;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    public T get() {
        if (object == null) {
            try {
                object = load();
            } catch (Exception e) {
                log.error(e, e);
            }
        }
        return object;
    }

    public boolean isLoaded() {
        return object == null;
    }

    public T reload() {
        try {
            object = load();
            return object;
        } catch (Exception e) {
            log.error(e, e);
            return null;
        }
    }

    public void reset() {
        object = null;
    }

    @SuppressWarnings("unchecked")
    protected T load() throws Exception {
        TypeProvider provider = Framework.getLocalService(SchemaManager.class);
        if (provider != null) {
            return (T) provider.getType(schema, name);
        }
        throw new TypeBindingException("No type provider registered");
    }

    @Override
    public int hashCode() {
        return schema.hashCode() ^ name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TypeRef) {
            TypeRef<?> prx = (TypeRef<?>) obj;
            return prx.schema.equals(schema) && prx.name.equals(name);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuffer(128).append(schema).append(':').append(name).toString();
    }

}

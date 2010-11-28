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

import java.util.Hashtable;
import java.util.Map;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SchemaImpl extends ComplexTypeImpl implements Schema {

    private static final long serialVersionUID = 3166979966270261291L;

    private boolean isLazy = true;

    private final Map<String, Type> types = new Hashtable<String, Type>();


    public SchemaImpl(String name) {
        this(name, Namespace.DEFAULT_NS);
    }

    public SchemaImpl(String name, Namespace ns) {
        super(null, SchemaNames.SCHEMAS,  name, ns == null
                ? Namespace.DEFAULT_NS : ns, F_UNSTRUCT_DEFAULT);
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }

    @Override
    public void setLazy(boolean isLazy) {
        this.isLazy = isLazy;
    }

    @Override
    public Type getType(String typeName) {
        return types.get(typeName);
    }

    @Override
    public Type[] getTypes() {
        return types.values().toArray(new Type[types.size()]);
    }

    @Override
    public void registerType(Type type) {
        types.put(type.getName(), type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

    @Override
    public TypeRef<Schema> getRef() {
        return new TypeRef<Schema>(schema, name, this);
    }

}

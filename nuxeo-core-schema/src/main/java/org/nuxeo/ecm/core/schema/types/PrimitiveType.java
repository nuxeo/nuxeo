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

import org.nuxeo.ecm.core.schema.TypeRef;
import org.nuxeo.ecm.core.schema.SchemaNames;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class PrimitiveType extends AbstractType implements SimpleType {

    private static final long serialVersionUID = -2698475002119528248L;

    protected PrimitiveType(String name) {
        super(null, SchemaNames.BUILTIN, name);
    }

    @Override
    public abstract boolean validate(Object object);

    @Override
    public Type getSuperType() {
        return null;
    }

    @Override
    public Type[] getTypeHierarchy() {
        return EMPTY_SUPERTYPES;
    }

    // FIXME: IType doesn't have an isPrimitive method.
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isSimpleType() {
        return true;
    }

    public SimpleType getPrimitiveType() {
        return this;
    }

    @Override
    public TypeRef<PrimitiveType> getRef() {
        return new TypeRef<PrimitiveType>(schema, name, this);
    }

}

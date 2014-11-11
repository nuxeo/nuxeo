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

package org.nuxeo.ecm.core.api.model.impl.osm.util;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MethodAccessor implements MemberAccessor {

    private static final long serialVersionUID = 8937769086103552264L;

    private final Method setter;
    private final Method getter;

    public MethodAccessor(Method getter, Method setter) {
        this.setter = setter;
        this.getter = getter;
        if (setter != null && !setter.isAccessible()) {
            setter.setAccessible(true);
        }
        if (!getter.isAccessible()) {
            getter.setAccessible(true);
        }
    }

    @Override
    public boolean isReadOnly() {
        return setter == null;
    }

    @Override
    public Object get(Object instance)  throws AccessException {
        try {
            return getter.invoke(instance);
        } catch (Exception e) {
            throw new AccessException("Failed to read field: "+getter, e);
        }
    }

    @Override
    public void set(Object instance, Object value) throws AccessException {
        if (isReadOnly()) {
            throw new ReadOnlyAccessException("Attempted to write on a read only field: "
                    + ObjectAccessorHelper.getPropertyName(getter.getName()));
        }
        try {
            setter.invoke(instance, value);
        } catch (Exception e) {
            throw new AccessException("Failed to write field: "+setter, e);
        }
    }


    @Override
    public Class<?> getType() {
        return getter.getReturnType();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return getter.getDeclaringClass();
    }


    public Method getSetterMethod() {
        return setter;
    }

    public Method getGetterMethod() {
        return getter;
    }

}

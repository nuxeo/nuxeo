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

package org.nuxeo.ecm.core.api.model.impl.osm;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.PropertyAccessException;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.osm.util.AccessException;
import org.nuxeo.ecm.core.api.model.impl.osm.util.MemberAccessor;
import org.nuxeo.ecm.core.api.model.impl.osm.util.ObjectAccessorHelper;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class DynamicObjectAdapter implements ObjectAdapter {

    private static final long serialVersionUID = 7739233932736918918L;

    protected final Map<String, MemberAccessor> fields;

    protected final Class<?> type;

    protected DynamicObjectAdapter(Class<?> type) {
        this.type = type;
        fields = new HashMap<String, MemberAccessor>();
    }

    public void addField(String name, String property) throws PropertyException {
        addField(name, property, false);
    }

    public void addField(String name, String property, boolean isReadOnly) throws PropertyException {
        try {
            MemberAccessor accessor = ObjectAccessorHelper.getMemberAccessor(type, property, isReadOnly);
            fields.put(name, accessor);
        } catch (Exception e) {
            throw new PropertyException(
                    "Mapping property " + property + " to field " + name + " from type " + type + " failed",
                    e);
        }
    }

    public Map<String, Object> getMap(Object object) throws PropertyException {
        if (object == null) { //TODO
            throw new PropertyAccessException("Trying to access a member of a null object");
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<String, MemberAccessor> entry : fields.entrySet()) {
            MemberAccessor accessor = entry.getValue();
            ObjectAdapter adapter = ObjectAdapterManager.getInstance().get(accessor.getType());
            try {
                if (adapter == null) { // a simple object
                    map.put(entry.getKey(), accessor.get(object));
                } else {
                    map.put(entry.getKey(), adapter.getMap(accessor.get(object)));
                }
            } catch (AccessException e) {
                throw new PropertyAccessException(entry.getKey(), e);
            }
        }
        return map;
    }

    public void setMap(Object object, Map<String, Object> value) throws PropertyException {
        if (object == null) {
            throw new PropertyAccessException("Trying to access a member of a null object");
        }
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            String key = entry.getKey();
            MemberAccessor accessor = fields.get(key);
            if (accessor == null) {
                throw new PropertyNotFoundException(key);
            }
            ObjectAdapter adapter = ObjectAdapterManager.getInstance().get(accessor.getType());
            try {
                if (adapter == null) { // a simple object
                    accessor.set(object, entry.getValue());
                } else {
                    adapter.setMap(object, (Map<String, Object>) entry.getValue());
                }
            } catch (AccessException e) {
                throw new PropertyAccessException(entry.getKey(), e);
            }
        }
    }

    public Object getValue(Object object, String name) throws PropertyException {
        if (object == null) {
            throw new PropertyAccessException("Trying to access a member of a null object: "+name);
        }
        MemberAccessor accessor = fields.get(name);
        if (accessor == null) {
            throw new PropertyNotFoundException(name);
        }
        try {
            return accessor.get(object);
        } catch (AccessException e) {
            throw new PropertyAccessException(name, e);
        }
    }

    public void setValue(Object object, String name, Object value) throws PropertyException {
        if (object == null) {
            throw new PropertyAccessException("Trying to access a member of a null object: "+name);
        }
        MemberAccessor accessor = fields.get(name);
        if (accessor == null) {
            throw new PropertyNotFoundException(name);
        }
        try {
            accessor.set(object, value);
        } catch (AccessException e) {
            throw new PropertyAccessException(name, e);
        }
    }

    public ObjectAdapter getAdapter(String name) throws PropertyNotFoundException {
        MemberAccessor accessor = fields.get(name);
        if (accessor == null) {
            throw new PropertyNotFoundException(name);
        }
        return ObjectAdapterManager.getInstance().get(accessor.getType());
    }

    public Serializable getDefaultValue() throws PropertyNotFoundException {
        return null;
    }

}

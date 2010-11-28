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

package org.nuxeo.ecm.core.api.model.impl;

import java.util.Hashtable;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyFactory;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * A composite property factory that use children factories to create properties.
 * <p>
 * The children factories are registered under a string key that is the type name corresponding
 * to the property that is to be created. The type name can be specified as an absolute or as a local type name.
 * For example if the global type <code>string</code> is redefined by a schema <code>myschema</code>
 * then you need to use the absolute type name to refer to that type: myschema:string.
 * <p>
 * If one looks up a factory using an absolute type name - the absolute name will be used and if no factory is found
 * then the local type name is used.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositePropertyFactory implements PropertyFactory {

    protected final Map<String, PropertyFactory>factories = new Hashtable<String, PropertyFactory>();

    protected final PropertyFactory defaultFactory;


    public CompositePropertyFactory(PropertyFactory defaultFactory) {
        this.defaultFactory = defaultFactory;
    }


    public void registerFactory(String type, PropertyFactory factory) {
        factories.put(type, factory);
    }

    public void registerFactory(String schema, String type, PropertyFactory factory) {
        if (schema == null) {
            factories.put(type, factory);
        } else {
            factories.put(schema+':'+type, factory);
        }
    }

    public PropertyFactory getFactory(String type) {
        return factories.get(type);
    }

    public PropertyFactory getFactory(String schema, String type) {
        //TODO: types must use QName for the type name
        String key = schema+':'+type;
        PropertyFactory factory = factories.get(key);
        if (factory == null) {
            factory = factories.get(type);
            factories.put(key, factory);
        }
        return factory;
    }

    @Override
    public Property createProperty(Property parent, Field field, int flags) {
        Type type = field.getType();
        PropertyFactory factory = getFactory(type.getSchemaName(), type.getName());
        if (factory != null ) {
            return factory.createProperty(parent, field, flags);
        }
        return defaultFactory.createProperty(parent, field, flags);
    }

}

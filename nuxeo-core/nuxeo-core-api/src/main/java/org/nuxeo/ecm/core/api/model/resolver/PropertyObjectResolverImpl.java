/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.model.resolver;

import java.util.List;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

/**
 * @since 7.1
 */
public class PropertyObjectResolverImpl implements PropertyObjectResolver {

    protected Property property;

    protected ObjectResolver resolver;

    public PropertyObjectResolverImpl(Property property, ObjectResolver resolver) {
        this.property = property;
        this.resolver = resolver;
    }

    @Override
    public List<Class<?>> getManagedClasses() {
        return resolver.getManagedClasses();
    }

    @Override
    public boolean validate() {
        return resolver.validate(property.getValue());
    }

    @Override
    public Object fetch() {
        return resolver.fetch(property.getValue());
    }

    @Override
    public <T> T fetch(Class<T> type) {
        return resolver.fetch(type, property.getValue());
    }

    @Override
    public void setObject(Object object) {
        Object reference = resolver.getReference(object);
        property.setValue(reference);
    }

    @Override
    public ObjectResolver getObjectResolver() {
        return resolver;
    }

}

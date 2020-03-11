/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
    public boolean validate(Object context) {
        return resolver.validate(property.getValue(), context);
    }

    @Override
    public Object fetch() {
        return resolver.fetch(property.getValue());
    }

    @Override
    public Object fetch(Object context) {
        return resolver.fetch(property.getValue(), context);
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

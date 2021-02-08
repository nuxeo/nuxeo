/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.schema;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.w3c.dom.Element;

/**
 * Registry for multiple descriptors to the "schema" extension point.
 *
 * @since 11.5
 */
public class SchemaRegistry implements Registry {

    protected SchemaBindingRegistry schemaBindingRegistry = new SchemaBindingRegistry();

    protected MapRegistry propertyRegistry = new MapRegistry();

    @Override
    public void initialize() {
        schemaBindingRegistry.initialize();
        propertyRegistry.initialize();
    }

    @Override
    public void tag(String id) {
        schemaBindingRegistry.tag(id);
        propertyRegistry.tag(id);
    }

    @Override
    public boolean isTagged(String id) {
        return schemaBindingRegistry.isTagged(id) || propertyRegistry.isTagged(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        Class<?> klass = xObject.getKlass();
        if (SchemaBindingDescriptor.class.equals(klass)) {
            schemaBindingRegistry.register(ctx, xObject, element, tag);
        } else if (PropertyDescriptor.class.equals(klass)) {
            propertyRegistry.register(ctx, xObject, element, tag);
        } else {
            throw new IllegalArgumentException("Unsupported class " + klass);
        }

    }

    @Override
    public void unregister(String tag) {
        schemaBindingRegistry.unregister(tag);
        propertyRegistry.unregister(tag);
    }

    // custom API

    public Map<String, SchemaBindingDescriptor> getSchemas() {
        return schemaBindingRegistry.getContributions();
    }

    public Set<String> getDisabledSchemas() {
        return schemaBindingRegistry.getDisabledContributions();
    }

    public List<PropertyDescriptor> getProperties() {
        return propertyRegistry.getContributionValues();
    }

}

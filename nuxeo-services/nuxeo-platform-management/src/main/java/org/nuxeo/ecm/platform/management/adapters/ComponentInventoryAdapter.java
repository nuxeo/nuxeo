/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.platform.management.adapters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.Property;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class ComponentInventoryAdapter implements ComponentInventoryMBean {

    protected final RegistrationInfo info;

    public ComponentInventoryAdapter(RegistrationInfo info) {
        this.info = info;
    }

    @Override
    public String getDescription() {
        return info.getDocumentation();
    }

    @Override
    public Integer getExtensionPointsCount() {
        return info.getExtensionPoints().length;
    }

    @Override
    public Set<String> getExtensionPointsName() {
        Set<String> names = new HashSet<>();
        for (ExtensionPoint extensionPoint : info.getExtensionPoints()) {
            names.add(extensionPoint.getName());
        }
        return names;
    }

    @Override
    public String getName() {
        return info.getName().getRawName();
    }

    @Override
    public Map<String, Property> getProperties() {
        return info.getProperties();
    }

    @Override
    public Set<String> getProvidedServices() {
        Set<String> names = new HashSet<>();
        names.addAll(Arrays.asList(info.getProvidedServiceNames()));
        return names;
    }

    @Override
    public Integer getProvidedServicesCount() {
        return info.getProvidedServiceNames().length;
    }

    @Override
    public String getVersion() {
        return info.getVersion().toString();
    }

}

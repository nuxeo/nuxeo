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
 *    Stephane Lacoin (Nuxeo EP Software Engineer)ne Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.collection.TransformedCollection;
import org.apache.commons.collections.map.TransformedMap;
import org.apache.commons.collections.set.TransformedSet;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class RuntimeServiceMBeanAdapter implements RuntimeServiceMBean {

    protected RuntimeService doGetRuntime() {
        return Framework.getRuntime();
    }

    @Override
    public String getDescription() {
        return doGetRuntime().getDescription();
    }

    @Override
    public String getHomeLocation() {
        try {
            return doGetRuntime().getHome().getCanonicalPath();
        } catch (IOException e) {
            throw ManagementRuntimeException.wrap(e);
        }
    }

    @Override
    public String getName() {
        return doGetRuntime().getName();
    }

    @Override
    public String getVersion() {
        return doGetRuntime().getVersion().toString();
    }

    private static class ComponentNameTransformer implements Transformer {

        public static final ComponentNameTransformer INSTANCE = new ComponentNameTransformer();

        @Override
        public Object transform(Object input) {
            return ((ComponentName) input).getRawName();
        }
    }

    private static class ComponentNamesTransformer implements Transformer {

        public static final ComponentNamesTransformer INSTANCE = new ComponentNamesTransformer();

        @Override
        @SuppressWarnings("unchecked")
        public Object transform(Object input) {
            Set<String> output = new HashSet<>();
            TransformedSet.decorate(output, ComponentNameTransformer.INSTANCE).addAll((Collection<?>) input);
            return output;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Set<String>> getPendingComponents() {
        Map<String, Set<String>> returnedMap = new HashMap<>();
        Map<ComponentName, Set<ComponentName>> pendingRegistrations = doGetRuntime().getComponentManager().getPendingRegistrations();
        if (pendingRegistrations.size() != 0) {
            TransformedMap.decorate(returnedMap, ComponentNameTransformer.INSTANCE, ComponentNamesTransformer.INSTANCE).putAll(
                    pendingRegistrations);
        }
        return returnedMap;
    }

    private static class RegistrationTransformer implements Transformer {

        public static final RegistrationTransformer INSTANCE = new RegistrationTransformer();

        @Override
        public Object transform(Object input) {
            return ((RegistrationInfo) input).getName().getRawName();
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getResolvedComponents() {
        Collection<RegistrationInfo> registrations = doGetRuntime().getComponentManager().getRegistrations();
        Set<String> returnedNames = new HashSet<>(registrations.size());
        if (registrations.size() > 0) {
            TransformedCollection.decorate(returnedNames, RegistrationTransformer.INSTANCE).addAll(registrations);
        }
        return returnedNames;
    }

}

/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.runtime.management.ManagementRuntimeException;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;


/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class RuntimeServiceMBeanAdapter implements RuntimeServiceMBean {

    protected RuntimeService doGetRuntime() {
        return Framework.getRuntime();
    }

    public String getDescription() {
        return doGetRuntime().getDescription();
    }

    public String getHomeLocation() {
        try {
            return doGetRuntime().getHome().getCanonicalPath();
        } catch (IOException e) {
            throw ManagementRuntimeException.wrap(e);
        }
    }

    public String getName() {
        return doGetRuntime().getName();
    }

    public String getVersion() {
        return doGetRuntime().getVersion().toString();
    }

    private static class ComponentNameTransformer implements Transformer {

        public static final ComponentNameTransformer INSTANCE = new ComponentNameTransformer();

        public Object transform(Object input) {
            return ((ComponentName) input).getRawName();
        }
    }

    private static class ComponentNamesTransformer implements Transformer {

        public static final ComponentNamesTransformer INSTANCE = new ComponentNamesTransformer();

        @SuppressWarnings("unchecked")
        public Object transform(Object input) {
            Set<String> output = new HashSet<String>();
            TransformedSet.decorate(output, ComponentNameTransformer.INSTANCE).addAll(
                    (Collection) input);
            return output;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Set<String>> getPendingComponents() {
        Map<String, Set<String>> returnedMap = new HashMap<String, Set<String>>();
        Map<ComponentName, Set<ComponentName>> pendingRegistrations = doGetRuntime().getComponentManager().getPendingRegistrations();
        if (pendingRegistrations.size() != 0) {
            TransformedMap.decorate(returnedMap,
                    ComponentNameTransformer.INSTANCE,
                    ComponentNamesTransformer.INSTANCE).putAll(
                    pendingRegistrations);
        }
        return returnedMap;
    }

    private static class RegistrationTransformer implements Transformer {

        public static final RegistrationTransformer INSTANCE = new RegistrationTransformer();

        public Object transform(Object input) {
            return ((RegistrationInfo) input).getName().getRawName();
        }

    }

    @SuppressWarnings("unchecked")
    public Set<String> getResolvedComponents() {
        Collection<RegistrationInfo> registrations = doGetRuntime().getComponentManager().getRegistrations();
        Set<String> returnedNames = new HashSet<String>(registrations.size());
        if (registrations.size() > 0) {
            TransformedCollection.decorate(returnedNames,
                    RegistrationTransformer.INSTANCE).addAll(registrations);
        }
        return returnedNames;
    }

}

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;

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

    @Override
    public Map<String, Set<String>> getPendingComponents() {
        return doGetRuntime().getComponentManager()
                             .getPendingRegistrations()
                             .entrySet()
                             .stream()
                             .collect(Collectors.toMap(e -> e.getKey().getRawName(),
                                     e -> e.getValue()
                                           .stream()
                                           .map(ComponentName::getRawName)
                                           .collect(Collectors.toSet())));
    }

    @Override
    public Set<String> getResolvedComponents() {
        return doGetRuntime().getComponentManager()
                             .getRegistrations()
                             .stream()
                             .map(r -> r.getName().getRawName())
                             .collect(Collectors.toSet());
    }

}

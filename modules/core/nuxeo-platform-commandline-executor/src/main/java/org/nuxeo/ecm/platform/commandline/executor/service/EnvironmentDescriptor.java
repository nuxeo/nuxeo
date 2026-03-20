/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.commandline.executor.service;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject(value = "environment")
public class EnvironmentDescriptor implements Descriptor {

    /**
     * If {@code name} is null, then the environment is global.<br>
     * Else the environment can be associated with a command ("command name") or with a tool ("command line").
     *
     * @since 7.4
     */
    @XNode("@name")
    protected String name;

    @XNode("workingDirectory")
    protected String workingDirectory;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> parameters = new HashMap<>();

    @Override
    public String getId() {
        return defaultIfEmpty(name, null);
    }

    /**
     * @since 7.4
     */
    public String getName() {
        return name;
    }

    public String getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = Environment.getDefault().getTemp().getPath();
        }
        if (!workingDirectory.endsWith("/")) {
            workingDirectory += "/";
        }
        return workingDirectory;
    }

    /**
     * @since 7.4
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    protected EnvironmentDescriptor withName(String name) {
        var descriptor = new EnvironmentDescriptor();
        descriptor.name = name;
        descriptor.workingDirectory = workingDirectory;
        descriptor.parameters = new HashMap<>(parameters);
        return descriptor;
    }

    @Override
    public EnvironmentDescriptor merge(Descriptor o) {
        if (o == null) {
            return this;
        }
        var other = (EnvironmentDescriptor) o;
        var merged = new EnvironmentDescriptor();
        // special case, as we reduce a list in the component with an identity that doesn't have a name
        merged.name = getIfNull(other.name, name);
        merged.workingDirectory = getIfNull(other.workingDirectory, workingDirectory);
        merged.parameters = new HashMap<>(parameters);
        merged.parameters.putAll(other.parameters);
        return merged;
    }
}

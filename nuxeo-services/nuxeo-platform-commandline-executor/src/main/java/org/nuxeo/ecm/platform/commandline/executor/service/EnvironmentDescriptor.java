/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "environment")
public class EnvironmentDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public String getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = Environment.getDefault().getTemp().getPath();
        }
        if (!workingDirectory.endsWith("/")) {
            workingDirectory += "/";
        }
        return workingDirectory;
    }

    public EnvironmentDescriptor merge(EnvironmentDescriptor other) {
        if (other != null) {
            if (other.workingDirectory != null) {
                workingDirectory = other.workingDirectory;
            }
            getParameters().putAll(other.getParameters());
        }
        return this;
    }

    /**
     * @since 7.4
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * @since 7.4
     */
    public String getName() {
        return name;
    }

}

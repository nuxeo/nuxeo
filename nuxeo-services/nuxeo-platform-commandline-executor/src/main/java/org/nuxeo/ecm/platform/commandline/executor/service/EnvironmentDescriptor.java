/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "environment")
public class EnvironmentDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("workingDirectory")
    protected String workingDirectory;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    public String getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = System.getProperty("java.io.tmpdir");
        }
        if (!workingDirectory.endsWith("/")) {
            workingDirectory += "/";
        }
        return workingDirectory;
    }

    public void merge(EnvironmentDescriptor other) {
        if (other.workingDirectory != null) {
            workingDirectory = other.workingDirectory;
        }
        parameters.putAll(other.parameters);
    }

}

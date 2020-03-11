/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveManager;

/**
 * XMap descriptor for contributions to the {@code changeFinder} extension point of the {@link NuxeoDriveManager}.
 *
 * @author Antoine Taillefer
 * @since 7.3
 */
@XObject("changeFinder")
public class ChangeFinderDescriptor {

    @XNode("@class")
    protected Class<? extends FileSystemChangeFinder> changeFinderClass;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<>();

    public FileSystemChangeFinder getChangeFinder() throws ReflectiveOperationException {
        FileSystemChangeFinder changeFinder = changeFinderClass.getDeclaredConstructor().newInstance();
        changeFinder.handleParameters(parameters);
        return changeFinder;
    }

    public Class<? extends FileSystemChangeFinder> getChangeFinderClass() {
        return changeFinderClass;
    }

    public void setChangeFinderClass(Class<? extends FileSystemChangeFinder> changeFinderClass) {
        this.changeFinderClass = changeFinderClass;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getparameter(String name) {
        return parameters.get(name);
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChangeFinderDescriptor)) {
            return false;
        }
        return changeFinderClass == ((ChangeFinderDescriptor) obj).changeFinderClass;
    }

    @Override
    public int hashCode() {
        return changeFinderClass.getName().hashCode();
    }

    @Override
    public String toString() {
        return changeFinderClass.getName();
    }

}

/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;

/**
 * XMap descriptor for factories contributed to the {@code topLevelFolderItemFactory} extension point of the
 * {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@XObject("topLevelFolderItemFactory")
public class TopLevelFolderItemFactoryDescriptor {

    @XNode("@class")
    protected Class<? extends TopLevelFolderItemFactory> factoryClass;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<>();

    public TopLevelFolderItemFactory getFactory() throws ReflectiveOperationException {
        TopLevelFolderItemFactory factory = factoryClass.getDeclaredConstructor().newInstance();
        factory.setName(factory.getClass().getName());
        factory.handleParameters(parameters);
        return factory;
    }

    public Class<? extends TopLevelFolderItemFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends TopLevelFolderItemFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getparameter(String name) {
        return parameters.get(name);
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getName() {
        return factoryClass.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TopLevelFolderItemFactoryDescriptor)) {
            return false;
        }
        return factoryClass == ((TopLevelFolderItemFactoryDescriptor) obj).factoryClass;
    }

    @Override
    public int hashCode() {
        return factoryClass.getName().hashCode();
    }

}

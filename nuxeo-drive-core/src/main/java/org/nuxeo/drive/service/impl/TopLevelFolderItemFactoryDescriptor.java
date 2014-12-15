/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * XMap descriptor for factories contributed to the {@code topLevelFolderItemFactory} extension point of the
 * {@link FileSystemItemAdapterService}.
 * 
 * @author Antoine Taillefer
 */
@XObject("topLevelFolderItemFactory")
public class TopLevelFolderItemFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = -7837197812448232426L;

    @XNode("@class")
    protected Class<? extends TopLevelFolderItemFactory> factoryClass;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    public TopLevelFolderItemFactory getFactory() throws InstantiationException, IllegalAccessException,
            ClientException {
        TopLevelFolderItemFactory factory = factoryClass.newInstance();
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
        return this.factoryClass.getName().equals(((TopLevelFolderItemFactoryDescriptor) obj).factoryClass.getName());
    }

    @Override
    public int hashCode() {
        return factoryClass.getName().hashCode();
    }

}

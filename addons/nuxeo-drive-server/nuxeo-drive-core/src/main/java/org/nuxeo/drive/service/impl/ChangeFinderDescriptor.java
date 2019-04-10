/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * XMap descriptor for contributions to the {@code changeFinder} extension point of the {@link NuxeoDriveManager}.
 *
 * @author Antoine Taillefer
 * @since 7.3
 */
@XObject("changeFinder")
public class ChangeFinderDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<? extends FileSystemChangeFinder> changeFinderClass;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    public FileSystemChangeFinder getChangeFinder() throws InstantiationException, IllegalAccessException,
            ClientException {
        FileSystemChangeFinder changeFinder = changeFinderClass.newInstance();
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
        return changeFinderClass.getName().equals(((ChangeFinderDescriptor) obj).changeFinderClass.getName());
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

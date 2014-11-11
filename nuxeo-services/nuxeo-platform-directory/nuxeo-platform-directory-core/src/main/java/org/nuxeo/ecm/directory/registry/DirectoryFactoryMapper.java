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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.registry;

import java.util.ArrayList;
import java.util.List;

/**
 * Pseudo contribution, to make the mapping between a directory and its
 * factory.
 *
 * @since 5.6
 */
public class DirectoryFactoryMapper {

    protected String directoryName;

    protected List<String> factories;

    public DirectoryFactoryMapper() {
        super();
    }

    public DirectoryFactoryMapper(String directoryName, String factory) {
        super();
        this.directoryName = directoryName;
        this.factories = new ArrayList<String>();
        this.factories.add(factory);
    }

    public DirectoryFactoryMapper(String directoryName, List<String> factories) {
        super();
        this.directoryName = directoryName;
        this.factories = factories;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public List<String> getFactories() {
        return factories;
    }

    public void setFactories(List<String> factories) {
        this.factories = factories;
    }

    public DirectoryFactoryMapper clone() {
        DirectoryFactoryMapper clone = new DirectoryFactoryMapper();
        clone.setDirectoryName(directoryName);
        clone.setFactories(factories);
        return clone;
    }

}

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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.registry;

import java.util.ArrayList;
import java.util.List;

/**
 * Pseudo contribution, to make the mapping between a directory and its factory.
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

    @Override
    public DirectoryFactoryMapper clone() {
        DirectoryFactoryMapper clone = new DirectoryFactoryMapper();
        clone.setDirectoryName(directoryName);
        clone.setFactories(factories);
        return clone;
    }

}

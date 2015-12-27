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
package org.nuxeo.ecm.directory.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.6
 */
public class MockMemoryDirectoryFactory extends DefaultComponent implements DirectoryFactory {

    protected Map<String, MockMemoryDirectoryDescriptor> reg = new HashMap<String, MockMemoryDirectoryDescriptor>();

    @Override
    public String getName() {
        return "org.nuxeo.ecm.directory.service.MockMemoryDirectoryFactory";
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        MockMemoryDirectoryDescriptor desc = (MockMemoryDirectoryDescriptor) contribution;
        String directoryName = desc.name;
        reg.put(directoryName, desc);
        try {
            DirectoryService directoryService = Framework.getService(DirectoryService.class);
            directoryService.registerDirectory(directoryName, this);
        } catch (Exception e) {
            throw new DirectoryException("Error in Directory Service lookup", e);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        String directoryName = ((MockMemoryDirectoryDescriptor) contribution).name;
        reg.remove(directoryName);
        try {
            DirectoryService directoryService = Framework.getService(DirectoryService.class);
            if (directoryService != null) {
                directoryService.unregisterDirectory(directoryName, this);
            }
        } catch (Exception e) {
            throw new DirectoryException("Error in Directory Service lookup", e);
        }
    }

    @Override
    public Directory getDirectory(String name) {
        if (reg.containsKey(name)) {
            return new MemoryDirectory(name, null, null, "uid", "foo");
        }
        return null;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<Directory> getDirectories() {
        List<Directory> directories = new ArrayList<>();
        for (String name : reg.keySet()) {
            directories.add(getDirectory(name));
        }
        return directories;
    }

}

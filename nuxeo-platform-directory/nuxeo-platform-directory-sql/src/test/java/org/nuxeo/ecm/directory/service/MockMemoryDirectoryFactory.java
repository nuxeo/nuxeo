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
package org.nuxeo.ecm.directory.service;

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
public class MockMemoryDirectoryFactory extends DefaultComponent implements
        DirectoryFactory {

    protected Map<String, MockMemoryDirectoryDescriptor> reg = new HashMap<String, MockMemoryDirectoryDescriptor>();

    @Override
    public String getName() {
        return "org.nuxeo.ecm.directory.service.MockMemoryDirectoryFactory";
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
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
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
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
        throw new NotImplementedException();
    }

}

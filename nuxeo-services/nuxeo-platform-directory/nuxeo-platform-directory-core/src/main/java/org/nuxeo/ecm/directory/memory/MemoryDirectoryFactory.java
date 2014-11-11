/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *
 * $Id: MemoryDirectoryFactory.java 30374 2008-02-20 16:31:28Z gracinet $
 */

package org.nuxeo.ecm.directory.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 *
 */
public class MemoryDirectoryFactory implements DirectoryFactory {

    private final Map<String, MemoryDirectory> directories;

    private final DirectoryService directoryService;

    public MemoryDirectoryFactory() throws DirectoryException {
        directories = new HashMap<String, MemoryDirectory>();
        // GR now NXRuntime provides the local one by default
        try {
            directoryService = Framework.getService(DirectoryService.class);
        } catch (Exception e) {
            throw new DirectoryException("Error in Directory Service lookup", e);
        }
//        directoryService=MultiDirectoryFactory.getDirectoryService();
    }

    public String getName() {
        return "memdirs";
    }

    public void registerDirectory(MemoryDirectory directory) {
        String directoryName = directory.getName();
        directories.put(directoryName, directory);
        directoryService.registerDirectory(directoryName, this);
    }

    public void unregisterDirectory(MemoryDirectory directory) {
        String directoryName = directory.getName();
        directoryService.unregisterDirectory(directoryName, this);
        directories.remove(directoryName);
    }

    public Directory getDirectory(String name) {
        return directories.get(name);
    }

    public void shutdown() {
    }

    public List<Directory> getDirectories() {
        return new ArrayList<Directory>(directories.values());
    }

}

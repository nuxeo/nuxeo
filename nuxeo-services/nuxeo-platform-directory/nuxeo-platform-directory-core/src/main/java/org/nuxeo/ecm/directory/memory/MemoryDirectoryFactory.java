/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
public class MemoryDirectoryFactory implements DirectoryFactory {

    private final Map<String, MemoryDirectory> directories;

    private final DirectoryService directoryService;

    public MemoryDirectoryFactory() throws DirectoryException {
        directories = new HashMap<String, MemoryDirectory>();
        directoryService = Framework.getService(DirectoryService.class);
    }

    @Override
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

    @Override
    public Directory getDirectory(String name) {
        return directories.get(name);
    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<Directory> getDirectories() {
        return new ArrayList<Directory>(directories.values());
    }

}

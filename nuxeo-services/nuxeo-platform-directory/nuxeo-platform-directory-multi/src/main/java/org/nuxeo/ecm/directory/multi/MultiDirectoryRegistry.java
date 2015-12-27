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
package org.nuxeo.ecm.directory.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class MultiDirectoryRegistry extends ContributionFragmentRegistry<MultiDirectoryDescriptor> {

    private static final Log log = LogFactory.getLog(MultiDirectoryRegistry.class);

    protected Map<String, MultiDirectoryDescriptor> descriptors = new HashMap<String, MultiDirectoryDescriptor>();

    // cache of directories
    protected Map<String, MultiDirectory> directories = new HashMap<String, MultiDirectory>();

    @Override
    public String getContributionId(MultiDirectoryDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id, MultiDirectoryDescriptor descriptor,
            MultiDirectoryDescriptor newOrigContrib) {
        String name = descriptor.name;
        if (descriptor.remove) {
            contributionRemoved(id, descriptor);
        } else {
            if (descriptors.containsKey(name)) {
                log.info("Directory registration updated: " + name);
            } else {
                log.info("Directory registered: " + name);
            }
            descriptors.put(id, descriptor);
            MultiDirectory directory = new MultiDirectory(descriptor);
            directories.put(name, directory);
        }
    }

    @Override
    public void contributionRemoved(String id, MultiDirectoryDescriptor origContrib) {
        descriptors.remove(id);
        Directory dir = directories.remove(id);
        if (dir != null) {
            try {
                dir.shutdown();
            } catch (DirectoryException e) {
                log.error(String.format("Error while shutting down directory '%s'", id), e);
            }
        }
        log.info("Directory removed: " + id);
    }

    @Override
    public MultiDirectoryDescriptor clone(MultiDirectoryDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(MultiDirectoryDescriptor src, MultiDirectoryDescriptor dst) {
        boolean remove = src.remove;
        // keep old remove info: if old contribution was removed, new one
        // should replace the old one completely
        boolean wasRemoved = dst.remove;
        if (remove) {
            dst.remove = remove;
            // don't bother merging
            return;
        }

        dst.merge(src, wasRemoved);
    }

    // API

    public MultiDirectory getDirectory(String name) {
        return directories.get(name);
    }

    public List<Directory> getDirectories() {
        List<Directory> res = new ArrayList<Directory>();
        res.addAll(directories.values());
        return res;
    }

}

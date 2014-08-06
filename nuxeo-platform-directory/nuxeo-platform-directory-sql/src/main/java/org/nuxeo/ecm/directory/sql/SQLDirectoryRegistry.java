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
package org.nuxeo.ecm.directory.sql;

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
public class SQLDirectoryRegistry extends
        ContributionFragmentRegistry<SQLDirectoryDescriptor> {

    private static final Log log = LogFactory.getLog(SQLDirectoryRegistry.class);

    protected Map<String, SQLDirectoryDescriptor> descriptors = new HashMap<String, SQLDirectoryDescriptor>();

    // cache map of directories
    protected Map<String, Directory> directories = new HashMap<String, Directory>();

    @Override
    public String getContributionId(SQLDirectoryDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id,
            SQLDirectoryDescriptor descriptor,
            SQLDirectoryDescriptor newOrigContrib) {
        String directoryName = descriptor.getName();
        if (descriptor.getRemove()) {
            log.info("Removing directory: " + directoryName);
            contributionRemoved(id, descriptor);
        } else {
            if (directories.containsKey(directoryName)) {
                log.info("Re-registered directory: " + directoryName);
            } else {
                log.info("Registered directory: " + directoryName);
            }
            descriptors.put(id, descriptor);
            directories.put(id, new SQLDirectory(descriptor));
        }
    }

    @Override
    public void contributionRemoved(String id, SQLDirectoryDescriptor descriptor) {
        String descriptorName = descriptor.getName();
        log.info("Unregistered directory: " + descriptorName);
        descriptors.remove(id);
        Directory dir = directories.remove(id);
        if (dir != null) {
            try {
                dir.shutdown();
            } catch (DirectoryException e) {
                log.error(String.format(
                        "Error while shutting down directory '%s'", id), e);
            }
        }
    }

    @Override
    public SQLDirectoryDescriptor clone(SQLDirectoryDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(SQLDirectoryDescriptor src, SQLDirectoryDescriptor dst) {
        boolean remove = src.getRemove();
        // keep old remove info: if old contribution was removed, new one
        // should replace the old one completely
        boolean wasRemoved = dst.getRemove();
        if (remove) {
            dst.setRemove(remove);
            // don't bother merging
            return;
        }

        dst.merge(src, wasRemoved);
    }

    // API

    public Directory getDirectory(String name) {
        return directories.get(name);
    }

    public List<Directory> getDirectories() {
        List<Directory> res = new ArrayList<Directory>();
        res.addAll(directories.values());
        return res;
    }

}

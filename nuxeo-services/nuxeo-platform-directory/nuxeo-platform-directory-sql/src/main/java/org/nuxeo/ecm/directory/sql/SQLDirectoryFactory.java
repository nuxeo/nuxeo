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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class SQLDirectoryFactory extends DefaultComponent implements
        DirectoryFactory {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.directory.sql.SQLDirectoryFactory");

    private static final Log log = LogFactory.getLog(SQLDirectoryFactory.class);

    /** All descriptors registered. */
    public List<SQLDirectoryDescriptor> descriptors = new ArrayList<SQLDirectoryDescriptor>();

    /** Effective directories. */
    private Map<String, Directory> directories;

    @Override
    public Directory getDirectory(String name) throws DirectoryException {
        return directories.get(name);
    }

    @Override
    public String getName() {
        return NAME.getName();
    }

    @Override
    public void activate(ComponentContext context) {
        directories = new LinkedHashMap<String, Directory>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            shutdown();
        } catch (DirectoryException e) {
            log.error("Error shutting down sql directories", e);
        }
        directories = null;
    }

    protected static DirectoryServiceImpl getDirectoryService() {
        return (DirectoryServiceImpl) Framework.getRuntime().getComponent(
                DirectoryService.NAME);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            SQLDirectoryDescriptor descriptor = (SQLDirectoryDescriptor) contrib;
            String directoryName = descriptor.getName();
            if (descriptor.getRemove()) {
                log.info("Removing directory: " + directoryName);
            } else {
                if (directories.containsKey(directoryName)) {
                    log.info("Re-registered directory: " + directoryName);
                } else {
                    log.info("Registered directory: " + directoryName);
                }
            }
            addDescriptor(descriptor);
        }
    }

    protected void addDescriptor(SQLDirectoryDescriptor descriptor) {
        descriptors.add(descriptor);
        refresh();
    }

    protected void removeDescriptor(SQLDirectoryDescriptor descriptor) {
        descriptors.remove(descriptor);
        refresh();
    }

    /**
     * Recompute effective directories from active descriptors.
     */
    protected void refresh() {
        DirectoryServiceImpl dirService = getDirectoryService();
        for (Entry<String, Directory> es : directories.entrySet()) {
            String directoryName = es.getKey();
            Directory directory = es.getValue();
            dirService.unregisterDirectory(directoryName, this);
            try {
                directory.shutdown();
            } catch (DirectoryException e) {
                log.error(
                        "Error shutting down sql directory: " + directoryName,
                        e);
            }
        }
        directories.clear();

        // compute effective descriptors
        Map<String, SQLDirectoryDescriptor> effective = new LinkedHashMap<String, SQLDirectoryDescriptor>();
        for (SQLDirectoryDescriptor descriptor : descriptors) {
            String directoryName = descriptor.getName();
            if (descriptor.getRemove()) {
                effective.remove(directoryName);
                continue;
            }
            if (effective.containsKey(directoryName)) {
                SQLDirectoryDescriptor old = effective.get(directoryName);
                descriptor.merge(old);
            }

            effective.put(directoryName, descriptor);
        }

        // compute effective directories
        for (SQLDirectoryDescriptor descriptor : effective.values()) {
            String descriptorName = descriptor.getName();
            directories.put(descriptorName, new SQLDirectoryProxy(descriptor));
            dirService.registerDirectory(descriptorName, this);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            SQLDirectoryDescriptor descriptor = (SQLDirectoryDescriptor) contrib;
            String descriptorName = descriptor.getName();
            log.info("Unregistered directory: " + descriptorName);
            removeDescriptor(descriptor);
        }
    }

    @Override
    public void shutdown() throws DirectoryException {
        for (Directory directory : directories.values()) {
            directory.shutdown();
        }
    }

    @Override
    public List<Directory> getDirectories() throws DirectoryException {
        return new ArrayList<Directory>(directories.values());
    }

}

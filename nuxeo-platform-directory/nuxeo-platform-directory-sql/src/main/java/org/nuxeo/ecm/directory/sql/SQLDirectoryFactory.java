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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.InverseReference;
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

    private Map<String, Directory> proxies;

    public Directory getDirectory(String name) throws DirectoryException {
        return proxies.get(name);
    }

    public String getName() {
        return NAME.getName();
    }

    @Override
    public void activate(ComponentContext context) {
        proxies = new HashMap<String, Directory>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        proxies = null;
    }

    protected static DirectoryServiceImpl getDirectoryService() {
        return (DirectoryServiceImpl) Framework.getRuntime().getComponent(
                DirectoryService.NAME);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        DirectoryServiceImpl dirService = getDirectoryService();
        for (Object contrib : contribs) {
            SQLDirectoryDescriptor descriptor = (SQLDirectoryDescriptor) contrib;
            String descriptorName = descriptor.getName();

            if (descriptor.getRemove()) {
                log.info("Removing directory: " + descriptorName);
                proxies.remove(descriptorName);
                dirService.unregisterDirectory(descriptorName, this);
                continue;
            }

            if (proxies.containsKey(descriptorName)) {
                mergeDescriptor(descriptor);
            }

            proxies.put(descriptorName, new SQLDirectoryProxy(descriptor));
            dirService.registerDirectory(descriptorName, this);
            log.info("Registered directory: " + descriptorName);
        }
    }

    private void mergeDescriptor(SQLDirectoryDescriptor descriptor) {
        SQLDirectoryDescriptor oldDescriptor;
        oldDescriptor = ((SQLDirectoryProxy) proxies.get(descriptor.getName())).getDescriptor();

        if (descriptor.getDbUser() == null) {
            descriptor.setDbUser(oldDescriptor.getDbUser());
        }

        if (descriptor.getDbPassword() == null) {
            descriptor.setDbPassword(oldDescriptor.getDbPassword());
        }

        if (descriptor.getDataSourceName() == null) {
            descriptor.setDataSourceName(oldDescriptor.getDataSourceName());
        }

        if (descriptor.getDbDriver() == null) {
            descriptor.setDbDriver(oldDescriptor.getDbDriver());
        }

        if (descriptor.getDbUrl() == null) {
            descriptor.setDbUrl(oldDescriptor.getDbUrl());
        }

        if (descriptor.getCreateTablePolicy() == null) {
            try {
                descriptor.setCreateTablePolicy(oldDescriptor.getCreateTablePolicy());
            } catch (DirectoryException e) {
                // Should never happend since Descriptor was already created
                log.error(e);
            }
        }

        if (descriptor.getIdField() == null) {
            descriptor.setIdField(oldDescriptor.getIdField());
        }

        if (descriptor.getReadOnly() == null) {
            descriptor.setReadOnly(oldDescriptor.getReadOnly());
        }

        if (descriptor.getSchemaName() == null) {
            descriptor.setSchemaName(oldDescriptor.getSchemaName());
        }

        if (descriptor.getParentDirectory() == null) {
            descriptor.setParentDirectory(oldDescriptor.getParentDirectory());
        }

        if (descriptor.getDataFileName() == null) {
            descriptor.setDataFileName(oldDescriptor.getDataFileName());
        }

        if (descriptor.getTableName() == null) {
            descriptor.setTableName(oldDescriptor.getTableName());
        }

        // References
        // for now only reuse the old descriptor
        // if no reference is set in the new one

        if (descriptor.getInverseReferences() == null
                || descriptor.getInverseReferences().length == 0) {
            descriptor.setInverseReferences((InverseReference[]) oldDescriptor.getInverseReferences());
        }

        if (descriptor.getTableReferences() == null
                || descriptor.getTableReferences().length == 0) {
            descriptor.setTableReferences((TableReference[]) oldDescriptor.getTableReferences());
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        DirectoryServiceImpl dirService = getDirectoryService();
        for (Object contrib : contribs) {
            SQLDirectoryDescriptor descriptor = (SQLDirectoryDescriptor) contrib;
            String directoryName = descriptor.getName();
            dirService.unregisterDirectory(directoryName, this);
            Directory directory = proxies.remove(directoryName);
            if (directory != null) {
                directory.shutdown();
            }
        }
    }

    public void shutdown() throws DirectoryException {
        for (Directory directory : proxies.values()) {
            directory.shutdown();
        }
    }

    public List<Directory> getDirectories() throws DirectoryException {
        List<Directory> directoryList = new ArrayList<Directory>();
        directoryList.addAll(proxies.values());
        return directoryList;
    }

}

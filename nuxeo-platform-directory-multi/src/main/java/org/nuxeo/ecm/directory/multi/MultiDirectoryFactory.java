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
 * $Id: MultiDirectoryFactory.java 29587 2008-01-23 21:52:30Z jcarsique $
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
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author Florent Guillaume
 *
 */
public class MultiDirectoryFactory extends DefaultComponent implements
        DirectoryFactory {

    private static final String NAME = "org.nuxeo.ecm.directory.multi.MultiDirectoryFactory";

    private static final Log log = LogFactory.getLog(MultiDirectoryFactory.class);

    private static DirectoryService directoryService;

    protected Map<String, MultiDirectory> directories;

    public Directory getDirectory(String name) {
        return directories.get(name);
    }

    public String getName() {
        return NAME;
    }

    @Override
    public void activate(ComponentContext context) {
        directories = new HashMap<String, MultiDirectory>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        directories = null;
    }

    public static DirectoryService getDirectoryService() {
        directoryService = (DirectoryService) Framework.getRuntime().getComponent(
                DirectoryService.NAME);
        if (directoryService == null) {
            directoryService = Framework.getLocalService(DirectoryService.class);
            if (directoryService == null) {
                try {
                    directoryService = Framework.getService(
                            DirectoryService.class);
                } catch (Exception e) {
                    log.error("Can't find Directory Service", e);
                }
            }
        }
        return directoryService;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        DirectoryService dirService = getDirectoryService();
        for (Object contrib : contribs) {
            MultiDirectoryDescriptor descriptor = (MultiDirectoryDescriptor) contrib;
            String name = descriptor.name;
            if (descriptor.remove) {
                log.info("Directory removed: " + name);
                directories.remove(name);
                dirService.unregisterDirectory(name, this);
                continue;
            }
            if (directories.containsKey(name)) {
                MultiDirectoryDescriptor previous = directories.get(name).getDescriptor();
                previous.merge(descriptor);
                log.info("Directory registration updated: " + name);
            } else {
                MultiDirectory directory = new MultiDirectory(descriptor);
                directories.put(name, directory);
                dirService.registerDirectory(name, this);
                log.info("Directory registered: " + name);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws DirectoryException {
        Object[] contribs = extension.getContributions();
        DirectoryService dirService = getDirectoryService();
        for (Object contrib : contribs) {
            MultiDirectoryDescriptor descriptor = (MultiDirectoryDescriptor) contrib;
            String directoryName = descriptor.name;
            dirService.unregisterDirectory(directoryName, this);
            directories.get(directoryName).shutdown();
            directories.remove(directoryName);
        }
    }

    public void shutdown() throws DirectoryException {
        for (Directory directory : directories.values()) {
            directory.shutdown();
        }
    }

    public List<Directory> getDirectories() {
        return new ArrayList<Directory>(directories.values());
    }

}

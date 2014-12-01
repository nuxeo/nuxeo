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
import java.util.List;

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
 */
public class MultiDirectoryFactory extends DefaultComponent implements
        DirectoryFactory {

    private static final String NAME = "org.nuxeo.ecm.directory.multi.MultiDirectoryFactory";

    protected MultiDirectoryRegistry directories;

    @Override
    public Directory getDirectory(String name) {
        return directories.getDirectory(name);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void activate(ComponentContext context) {
        directories = new MultiDirectoryRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        directories = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        DirectoryService dirService = Framework.getService(DirectoryService.class);
        for (Object contrib : contribs) {
            MultiDirectoryDescriptor descriptor = (MultiDirectoryDescriptor) contrib;
            directories.addContribution(descriptor);
            String name = descriptor.name;
            if (directories.getDirectory(name) != null) {
                dirService.registerDirectory(name, this);
            } else {
                // handle case where directory is marked with "remove"
                dirService.unregisterDirectory(name, this);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension)
            throws DirectoryException {
        Object[] contribs = extension.getContributions();
        DirectoryService dirService = Framework.getService(DirectoryService.class);
        for (Object contrib : contribs) {
            MultiDirectoryDescriptor descriptor = (MultiDirectoryDescriptor) contrib;
            String directoryName = descriptor.name;
            dirService.unregisterDirectory(directoryName, this);
            directories.removeContribution(descriptor);
        }
    }

    @Override
    public void shutdown() throws DirectoryException {
        for (Directory directory : directories.getDirectories()) {
            directory.shutdown();
        }
    }

    @Override
    public List<Directory> getDirectories() {
        return new ArrayList<Directory>(directories.getDirectories());
    }

}

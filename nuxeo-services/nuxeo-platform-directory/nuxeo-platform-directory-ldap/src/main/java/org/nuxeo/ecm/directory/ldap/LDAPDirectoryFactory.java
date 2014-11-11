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

package org.nuxeo.ecm.directory.ldap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.ldap.registry.LDAPDirectoryRegistry;
import org.nuxeo.ecm.directory.ldap.registry.LDAPServerRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class LDAPDirectoryFactory extends DefaultComponent implements
        DirectoryFactory {

    public static final String NAME = "org.nuxeo.ecm.directory.ldap.LDAPDirectoryFactory";

    private static final Log log = LogFactory.getLog(LDAPDirectoryFactory.class);

    protected LDAPDirectoryRegistry proxies;

    protected LDAPServerRegistry servers;

    public Directory getDirectory(String name) {
        return proxies.getDirectory(name);
    }

    public List<Directory> getDirectories() {
        List<Directory> directories = new ArrayList<Directory>();
        directories.addAll(proxies.getDirectories());
        return directories;
    }

    public LDAPServerDescriptor getServer(String name) {
        return servers.getServer(name);
    }

    public String getName() {
        return NAME;
    }

    @Override
    public void activate(ComponentContext context) {
        log.info("component activated");
        proxies = new LDAPDirectoryRegistry();
        servers = new LDAPServerRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.info("component deactivated");
        proxies = null;
        servers = null;
    }

    protected static DirectoryServiceImpl getDirectoryService() {
        return (DirectoryServiceImpl) Framework.getLocalService(DirectoryService.class);
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("directories")) {
            registerDirectoryExtension(extension);
        } else if (xp.equals("servers")) {
            registerServerExtension(extension);
        }
    }

    @Override
    public void unregisterExtension(Extension extension)
            throws DirectoryException {
        String xp = extension.getExtensionPoint();
        if (xp.equals("directories")) {
            unregisterDirectoryExtension(extension);
        } else if (xp.equals("servers")) {
            unregisterServerExtension(extension);
        }
    }

    public void registerServerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            LDAPServerDescriptor descriptor = (LDAPServerDescriptor) contrib;
            servers.addContribution(descriptor);
        }
    }

    public void unregisterServerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            LDAPServerDescriptor descriptor = (LDAPServerDescriptor) contrib;
            servers.removeContribution(descriptor);
        }
    }

    public void registerDirectoryExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        DirectoryServiceImpl dirService = getDirectoryService();
        for (Object contrib : contribs) {
            LDAPDirectoryDescriptor descriptor = (LDAPDirectoryDescriptor) contrib;
            proxies.addContribution(descriptor);
            dirService.registerDirectory(descriptor.getName(), this);
        }
    }

    public void unregisterDirectoryExtension(Extension extension)
            throws DirectoryException {
        Object[] contribs = extension.getContributions();
        DirectoryServiceImpl dirService = getDirectoryService();
        for (Object contrib : contribs) {
            LDAPDirectoryDescriptor descriptor = (LDAPDirectoryDescriptor) contrib;
            proxies.removeContribution(descriptor);
            dirService.unregisterDirectory(descriptor.getName(), this);
        }
    }

    public void shutdown() throws DirectoryException {
        for (Directory directory : proxies.getDirectories()) {
            directory.shutdown();
        }
    }

}

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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.directory.DefaultDirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.ldap.registry.LDAPServerRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;

public class LDAPDirectoryFactory extends DefaultDirectoryFactory {

    public static final String SERVERS_XP = "servers";

    protected LDAPServerRegistry servers = new LDAPServerRegistry();

    public LDAPServerDescriptor getServer(String name) {
        return servers.getServer(name);
    }

    protected static DirectoryServiceImpl getDirectoryService() {
        return (DirectoryServiceImpl) Framework.getService(DirectoryService.class);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DIRECTORIES_XP.equals(extensionPoint)) {
            super.registerContribution(contribution, extensionPoint, contributor);
        } else if (SERVERS_XP.equals(extensionPoint)) {
            registerServerContribution((LDAPServerDescriptor) contribution);
        } else {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DIRECTORIES_XP.equals(extensionPoint)) {
            super.unregisterContribution(contribution, extensionPoint, contributor);
        } else if (SERVERS_XP.equals(extensionPoint)) {
            unregisterServerContribution((LDAPServerDescriptor) contribution);
        }
    }

    public void registerServerContribution(LDAPServerDescriptor descriptor) {
        servers.addContribution(descriptor);
    }

    public void unregisterServerContribution(LDAPServerDescriptor descriptor) {
        servers.removeContribution(descriptor);
    }

}

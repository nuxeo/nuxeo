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

package org.nuxeo.ecm.directory.ldap.registry;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.ldap.LDAPServerDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class LDAPServerRegistry extends
        ContributionFragmentRegistry<LDAPServerDescriptor> {

    public static final Log log = LogFactory.getLog(LDAPServerRegistry.class);

    private final Map<String, LDAPServerDescriptor> servers = new HashMap<String, LDAPServerDescriptor>();

    @Override
    public String getContributionId(LDAPServerDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, LDAPServerDescriptor descriptor,
            LDAPServerDescriptor newOrigContrib) {
        String descriptorName = descriptor.getName();
        servers.put(descriptorName, descriptor);
        log.info("server registered: " + descriptorName);
    }

    @Override
    public void contributionRemoved(String id, LDAPServerDescriptor descriptor) {
        String descriptorName = descriptor.getName();
        servers.remove(descriptorName);
        log.info("server unregistered: " + descriptorName);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public LDAPServerDescriptor clone(LDAPServerDescriptor orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(LDAPServerDescriptor src, LDAPServerDescriptor dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public LDAPServerDescriptor getServer(String name) {
        return servers.get(name);
    }

}

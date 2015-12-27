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
package org.nuxeo.ecm.directory.ldap.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPDirectoryDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class LDAPDirectoryRegistry extends ContributionFragmentRegistry<LDAPDirectoryDescriptor> {

    private static final Log log = LogFactory.getLog(LDAPDirectoryRegistry.class);

    private final Map<String, Directory> proxies = new HashMap<String, Directory>();

    @Override
    public String getContributionId(LDAPDirectoryDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, LDAPDirectoryDescriptor descriptor,
            LDAPDirectoryDescriptor newOrigContrib) {
        String descriptorName = descriptor.getName();
        proxies.put(descriptorName, new LDAPDirectory(descriptor));
        log.info("directory registered: " + descriptorName);
    }

    @Override
    public void contributionRemoved(String id, LDAPDirectoryDescriptor descriptor) {
        String directoryName = descriptor.getName();
        Directory dir = proxies.remove(directoryName);
        if (dir != null) {
            try {
                dir.shutdown();
            } catch (DirectoryException e) {
                log.error(String.format("Error while shutting down directory '%s'", id), e);
            }
        }
        log.info("directory unregistered: " + directoryName);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public LDAPDirectoryDescriptor clone(LDAPDirectoryDescriptor orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(LDAPDirectoryDescriptor src, LDAPDirectoryDescriptor dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public Directory getDirectory(String name) {
        return proxies.get(name);
    }

    public List<Directory> getDirectories() {
        List<Directory> res = new ArrayList<Directory>();
        res.addAll(proxies.values());
        return res;
    }

}

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.ldap.LDAPServerDescriptor;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for LDAP servers
 *
 * @since 5.6
 */
public class LDAPServerRegistry extends SimpleContributionRegistry<LDAPServerDescriptor> {

    public static final Log log = LogFactory.getLog(LDAPServerRegistry.class);

    @Override
    public String getContributionId(LDAPServerDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, LDAPServerDescriptor contrib, LDAPServerDescriptor newOrigContrib) {
        super.contributionUpdated(id, contrib, newOrigContrib);
        log.info("server registered: " + contrib.getName());
    }

    @Override
    public void contributionRemoved(String id, LDAPServerDescriptor origContrib) {
        super.contributionRemoved(id, origContrib);
        log.info("server unregistered: " + origContrib.getName());
    }

    // API

    public LDAPServerDescriptor getServer(String name) {
        return getCurrentContribution(name);
    }

}

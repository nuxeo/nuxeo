/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.security;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Helper for Nuxeo security-related checks on Explorer.
 *
 * @since 11.2
 */
public class SecurityHelper {

    public static final String PROPERTY_READERS_GROUP = "org.nuxeo.apidoc.apidocreaders.group";

    public static final String DEFAULT_READERS_GROUP = "Everyone";

    public static final String PROPERTY_APIDOC_MANAGERS_GROUP = "org.nuxeo.apidoc.apidocmanagers.group";

    public static final String DEFAULT_APIDOC_MANAGERS_GROUP = "ApidocManagers";

    public static String getApidocReadersGroup() {
        return Framework.getService(ConfigurationService.class)
                        .getString(PROPERTY_READERS_GROUP, DEFAULT_READERS_GROUP);
    }

    public static String getApidocManagersGroup() {
        return Framework.getService(ConfigurationService.class)
                        .getString(PROPERTY_APIDOC_MANAGERS_GROUP, DEFAULT_APIDOC_MANAGERS_GROUP);
    }

    /**
     * Returns true if given principal can initialize the distributions root.
     */
    public static boolean canInitDistributionsRoot(NuxeoPrincipal principal) {
        if (principal == null || principal.isAnonymous()) {
            return false;
        }
        if (principal.isAdministrator()) {
            return true;
        }
        return principal.getAllGroups().contains(getApidocManagersGroup());
    }

    public static boolean canManageDistributions(NuxeoPrincipal principal) {
        return canInitDistributionsRoot(principal);
    }

    public static boolean canSnapshotLiveDistribution(NuxeoPrincipal principal) {
        return principal.isAdministrator();
    }

}

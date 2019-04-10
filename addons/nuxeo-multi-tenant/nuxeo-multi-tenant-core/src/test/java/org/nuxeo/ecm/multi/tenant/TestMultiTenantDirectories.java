/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.multi.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.multi.tenant.Constants.POWER_USERS_GROUP;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test class for the management of directories with multitenancy.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.multi.tenant")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-enabled-default-test-contrib.xml")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-directories-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = MultiTenantRepositoryInit.class)
public class TestMultiTenantDirectories {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected UserManager userManager;

    @Test
    public void iCanCreateSameDirectoryEntryInDifferentTenant() throws LoginException {
        // Add user0 and user1 to the powerusers group so they can edit the directory
        Framework.doPrivileged(() -> {
            NuxeoPrincipal user0Principal = userManager.getPrincipal("user0");
            List<String> groups = user0Principal.getGroups();
            groups.add(POWER_USERS_GROUP);
            user0Principal.setGroups(groups);
            userManager.updateUser(user0Principal.getModel());
            NuxeoPrincipal user1Principal = userManager.getPrincipal("user1");
            groups = user1Principal.getGroups();
            groups.add(POWER_USERS_GROUP);
            user1Principal.setGroups(groups);
            userManager.updateUser(user1Principal.getModel());
        });

        // Map of the entry to create per tenant
        Map<String, Object> newEntry = new HashMap<>();
        newEntry.put("id", "key0");
        newEntry.put("label", "Label Key 0");

        // As user0, add a new entry in directory
        LoginContext loginContext = Framework.loginAsUser("user0");
        try (CloseableCoreSession userSession = openSession()) {
            // Open the testDirectory
            try (Session sessionDir = directoryService.open("testDirectory")) {
                sessionDir.createEntry(newEntry);
            }
        }
        loginContext.logout();

        // As user1, add the same entry in directory
        loginContext = Framework.loginAsUser("user1");
        try (CloseableCoreSession userSession = openSession()) {
            // Open the testDirectory
            try (Session sessionDir = directoryService.open("testDirectory")) {
                sessionDir.createEntry(newEntry);
            }
        }
        loginContext.logout();

        // Check that the two entries are created
        Framework.doPrivileged(() -> {
            // Open the testDirectory
            try (Session sessionDir = directoryService.open("testDirectory")) {
                DocumentModelList entries = sessionDir.query(Collections.emptyMap());
                assertThat(entries).hasSize(2);
            }
        });
    }

    @Test()
    public void iCannotCreateSameDirectoryEntryInSameTenant() throws LoginException {
        // Add user0 to the powerusers group so he can edit the directory
        Framework.doPrivileged(() -> {
            NuxeoPrincipal user0Principal = userManager.getPrincipal("user0");
            List<String> groups = user0Principal.getGroups();
            groups.add(POWER_USERS_GROUP);
            user0Principal.setGroups(groups);
            userManager.updateUser(user0Principal.getModel());
        });

        // Map of the entry to create per tenant
        Map<String, Object> newEntry = new HashMap<>();
        newEntry.put("id", "key0");
        newEntry.put("label", "Label Key 0");

        // As user0, add a new entry in directory
        LoginContext loginContext = Framework.loginAsUser("user0");
        try (CloseableCoreSession userSession = openSession()) {
            // Open the testDirectory
            try (Session sessionDir = directoryService.open("testDirectory")) {
                sessionDir.createEntry(newEntry);
                // Test if the second creation fails
                try {
                    sessionDir.createEntry(newEntry);
                } catch (DirectoryException e) {
                    assertThat(e.getMessage()).isEqualTo("Entry with id tenant_domain0_key0 already exists");
                }
            }
        }
        loginContext.logout();
    }

    protected CloseableCoreSession openSession() {
        return coreFeature.openCoreSession();
    }
}

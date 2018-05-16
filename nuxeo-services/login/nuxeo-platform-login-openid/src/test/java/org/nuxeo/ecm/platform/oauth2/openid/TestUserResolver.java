/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDUserInfoStoreImpl.DIRECTORY_NAME;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.openid.auth.DefaultOpenIDUserInfo;
import org.nuxeo.ecm.platform.oauth2.openid.auth.EmailBasedUserResolver;
import org.nuxeo.ecm.platform.oauth2.openid.auth.StoredUserInfoResolver;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.login.openid.test:OSGI-INF/openid-connect-provider-registry.xml")
@Deploy("org.nuxeo.ecm.platform.login.openid.test:OSGI-INF/openid-directory-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.login.openid.test:OSGI-INF/openid-schema-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.login.openid.test:OSGI-INF/openid-userresolver-contrib.xml")
public class TestUserResolver {

    protected static final String ADMIN_USERNAME = "Administrator";

    @Inject
    protected UserManager userManager;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testEmailBasedUserResolver() {

        OpenIDConnectProvider provider = Framework.getService(OpenIDConnectProviderRegistry.class)
                                                  .getProvider("provider1");

        assertTrue(provider.getUserResolver() instanceof EmailBasedUserResolver);
        EmailBasedUserResolver emailResolver = (EmailBasedUserResolver) provider.getUserResolver();

        DefaultOpenIDUserInfo info = new DefaultOpenIDUserInfo();
        info.put("email", "devnull@nuxeo.com");
        DocumentModel doc = userManager.getUserModel(ADMIN_USERNAME);

        ClientLoginModule.getThreadLocalLogin().pop();
        try {
            // Assert that you can fetch username even if not authenticated
            String username = emailResolver.findNuxeoUser(info);
            assertEquals(ADMIN_USERNAME, username);

            info.put("email", "test@nuxeo.com");
            // Assert that you can update a user even if not authenticated
            doc = emailResolver.updateUserInfo(doc, info);
            assertEquals("test@nuxeo.com", doc.getPropertyValue("user:email"));

        } finally {
            ClientLoginModule.getThreadLocalLogin().push(new SystemPrincipal(null), null, null);
        }

    }

    @Test
    public void testStoredUserBasedInfoResolver() {

        OpenIDConnectProvider provider = Framework.getService(OpenIDConnectProviderRegistry.class)
                                                  .getProvider("provider2");

        assertTrue(provider.getUserResolver() instanceof StoredUserInfoResolver);
        StoredUserInfoResolver userInfoResolver = (StoredUserInfoResolver) provider.getUserResolver();

        DefaultOpenIDUserInfo info = new DefaultOpenIDUserInfo();
        // Initialize directory
        try (Session session = directoryService.open(DIRECTORY_NAME)) {
            Map<String, Object> infos = new HashMap<>();
            infos.put("id", "null@null");
            infos.put("nuxeoLogin", ADMIN_USERNAME);
            infos.put("provider", "null");
            infos.put("email", "devnull@nuxeo.com");
            session.createEntry(infos);

            DocumentModel doc = userManager.getUserModel(ADMIN_USERNAME);
            ClientLoginModule.getThreadLocalLogin().pop();
            try {
                // Assert that you can fetch username even if not authenticated
                String username = userInfoResolver.findNuxeoUser(info);
                assertEquals(ADMIN_USERNAME, username);

                info.put("email", "test@nuxeo.com");
                info.put("updated_time", new Date().toInstant().toString());
                // Assert that you can update a user even if not authenticated
                userInfoResolver.updateUserInfo(doc, info);

                // Assert that the info is updated in the directory
                doc = Framework.doPrivileged(
                        () -> session.query(Collections.singletonMap("nuxeoLogin", ADMIN_USERNAME)).get(0));
                assertEquals("test@nuxeo.com", doc.getPropertyValue("openIdUserInfo:email"));

            } finally {
                ClientLoginModule.getThreadLocalLogin().push(new SystemPrincipal(null), null, null);
            }
        }
    }

}

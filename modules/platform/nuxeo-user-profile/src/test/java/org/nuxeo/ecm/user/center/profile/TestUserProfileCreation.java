/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.center.profile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.user.center.profile")
public class TestUserProfileCreation {

    @Inject
    CoreFeature coreFeature;

    @Inject
    UserWorkspaceService userWorkspaceService;

    @Inject
    DirectoryService directoryService;

    @Inject
    UserProfileService ups;

    @Test
    public void testAdminCreate() throws Exception {

        DocumentModel user;
        try (Session userDir = directoryService.getDirectory("userDirectory").getSession()) {
            Map<String, Object> user1 = new HashMap<>();
            user1.put("username", "user1");
            user1.put("groups", Arrays.asList(new String[] { "members" }));
            user = userDir.createEntry(user1);
        }

        CoreSession session = coreFeature.getCoreSession(user.getId());
        DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(session, null);
        Assert.assertEquals(user.getId(), userWorkspace.getName());

        DocumentModel up = ups.getUserProfileDocument(session);
        Assert.assertNotNull(up);
    }

}

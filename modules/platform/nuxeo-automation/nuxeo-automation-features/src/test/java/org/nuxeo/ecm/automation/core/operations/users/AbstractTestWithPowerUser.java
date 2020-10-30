/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.operations.users;

import java.io.Serializable;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.automation.features:test-user-directories-contrib.xml")
@Deploy("org.nuxeo.ecm.automation.features:test-usermanager-powerusers.xml")
public abstract class AbstractTestWithPowerUser {

    public static final String ADMINISTRATORS_GROUP = "administrators";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected UserManager userManager;

    @Inject
    protected AutomationService automationService;

    @Before
    public void before() {
        // power user
        if (userManager.getPrincipal("leela") != null) {
            userManager.deleteUser("leela");
        }
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", "leela");
        user.setPropertyValue("user:password", "pwd");
        user.setPropertyValue("user:groups", (Serializable) Collections.singletonList("powerusers"));
        userManager.createUser(user);

        // simple user with no group
        if (userManager.getPrincipal("fry") != null) {
            userManager.deleteUser("fry");
        }
        user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", "fry");
        userManager.createUser(user);

        // subgroup with administrators as parent group
        if (userManager.getGroup("subgroup") != null) {
            userManager.deleteGroup("subgroup");
        }
        NuxeoGroup group = new NuxeoGroupImpl("subgroup");
        group.setParentGroups(Collections.singletonList(ADMINISTRATORS_GROUP));
        userManager.createGroup(group.getModel());

        txFeature.nextTransaction();
    }
}

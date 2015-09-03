/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.platform.usermanager;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.After;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class) // to init properties for SQL datasources
@Deploy({ "org.nuxeo.ecm.core.schema", //
        "org.nuxeo.ecm.core.api", //
        "org.nuxeo.ecm.core", //
        "org.nuxeo.ecm.core.event", //
        "org.nuxeo.ecm.platform.usermanager.api", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.api", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.platform.query.api", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/usermanager-inmemory-cache-config.xml", //
        "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/userservice-config.xml", //
})
public abstract class UserManagerTestCase {

    @Inject
    protected UserManager userManager;

    public void TODO() {
//        if (!RedisFeature.setup(this)) {
//            deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
//                    "test-usermanagerimpl/usermanager-redis-cache-config.xml");
//        } else {
//            deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
//                    "test-usermanagerimpl/usermanager-inmemory-cache-config.xml");
//        }
//        fireFrameworkStarted();
    }

    @After
    public void cleanup() {
        DocumentModelList users = userManager.searchUsers(null);
        for (DocumentModel user : users) {
            String userId = user.getId();
            if (userId.equals(userManager.getAnonymousUserId())) {
                continue;
            }
            if (userId.startsWith("Administrator")) {
                // comes from a CSV
                continue;
            }
            userManager.getPrincipal(userId); // init relation tables needed on delete
            userManager.deleteUser(userId);
        }
        for (String groupId : Arrays.asList("group1", "group2", "group3")) {
            if (userManager.getGroup(groupId) != null) {
                userManager.deleteGroup(groupId);
            }
        }
    }

}

/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.platform.test.guice;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class UserManagerProvider implements Provider<UserManager> {

    private static final Log log = LogFactory.getLog(UserManagerProvider.class);

    private final RuntimeHarness harness;

    private final DirectoryService service;

    @Inject
    public UserManagerProvider(RuntimeHarness harness, DirectoryService service)
            throws Exception {
        this.harness = harness;
        this.service = service;
    }

    public UserManager get() {
        try {
            assertNotNull(service);

            // Deploy UserManager
            harness.deployContrib("org.nuxeo.ecm.platform.usermanager",
                    "OSGI-INF/UserService.xml");

            harness.deployContrib("org.nuxeo.ecm.platform.test",
                    "test-usermanagerimpl/userservice-config.xml");
            return Framework.getService(UserManager.class);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

}

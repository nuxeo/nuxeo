/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vincent Dutat
 */
package org.nuxeo.ecm.multi.tenant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.multi.tenant", "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.ecm.core.cache",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.default.config" })
@LocalDeploy({ "org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml",
        "org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml" })
public class TestMultiTenantHelper {

    @Inject protected CoreSession session;

    @Inject protected UserManager userManager;

    /**
     * just to test it does not throw an exception.
     *
     * @throws Exception
     */
    @Test public void testGetCurrentTenantId() throws Exception {
        MultiTenantHelper.getCurrentTenantId(new SystemPrincipal("nobody"));
    }

    /**
     * just to test it does not throw an exception.
     *
     * @throws Exception
     */
    @Test public void testGetTenantId() throws Exception {
        MultiTenantHelper.getTenantId("nobody");
    }

}

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
 *     Vincent Dutat
 */
package org.nuxeo.ecm.multi.tenant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.NuxeoLoginFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, NuxeoLoginFeature.class} )
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.multi.tenant")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml")
public class TestMultiTenantHelper {

    /**
     * just to test it does not throw an exception.
     *
     * @throws Exception
     */
    @Test
    public void testGetCurrentTenantId() throws Exception {
        MultiTenantHelper.getCurrentTenantId(new SystemPrincipal("nobody"));
    }

    /**
     * just to test it does not throw an exception.
     *
     * @throws Exception
     */
    @Test
    public void testGetTenantId() throws Exception {
        MultiTenantHelper.getTenantId("nobody");
    }

}

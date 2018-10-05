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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.usermanager.io;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class NuxeoPrincipalJsonWriterTest extends
        AbstractJsonWriterTest.External<NuxeoPrincipalJsonWriter, NuxeoPrincipal> {

    public NuxeoPrincipalJsonWriterTest() {
        super(NuxeoPrincipalJsonWriter.class, NuxeoPrincipal.class);
    }

    @Inject
    private UserManager userManager;

    @Test
    public void test() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        JsonAssert json = jsonAssert(principal);
        json.isObject();
        json.properties(6);
        json.has("entity-type").isEquals("user");
        json.has("id").isEquals("Administrator");
        json.has("isAdministrator").isTrue();
        json.has("isAnonymous").isFalse();
        JsonAssert model = json.has("properties").properties(7);
        model.has("lastName").isEmptyStringOrNull();
        model.has("username").isEquals("Administrator");
        model.has("email").isEquals("devnull@nuxeo.com");
        model.has("company").isEmptyStringOrNull();
        model.has("firstName").isEmptyStringOrNull();
        model.has("groups").contains("administrators");
        JsonAssert exGroup = json.has("extendedGroups").length(1).has(0);
        exGroup.properties(3);
        exGroup.has("name").isEquals("administrators");
        exGroup.has("label").isEquals("Administrators group");
        exGroup.has("url").isEquals("group/administrators");
    }

    @Test
    public void testSystemUser() throws Exception {
        NuxeoPrincipal principal = new SystemPrincipal(null);
        JsonAssert json = jsonAssert(principal);
        json.isObject();
        // it has no properties
        json.properties(5);
        json.has("entity-type").isEquals("user");
        json.has("id").isEquals(SecurityConstants.SYSTEM_USERNAME);
        json.has("isAdministrator").isTrue();
        json.has("isAnonymous").isFalse();
        JsonAssert exGroup = json.has("extendedGroups").length(1).has(0);
        exGroup.properties(3);
        exGroup.has("name").isEquals("administrators");
        exGroup.has("label").isEquals("Administrators group");
        exGroup.has("url").isEquals("group/administrators");
    }

}

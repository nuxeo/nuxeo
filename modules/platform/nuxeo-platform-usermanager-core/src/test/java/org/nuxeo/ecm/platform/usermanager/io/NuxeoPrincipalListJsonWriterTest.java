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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class NuxeoPrincipalListJsonWriterTest extends
        AbstractJsonWriterTest.External<NuxeoPrincipalListJsonWriter, List<NuxeoPrincipal>> {

    public NuxeoPrincipalListJsonWriterTest() {
        super(NuxeoPrincipalListJsonWriter.class, List.class, TypeUtils.parameterize(List.class, NuxeoPrincipal.class));
    }

    @Inject
    private UserManager userManager;

    @Test
    public void test() throws Exception {
        NuxeoPrincipal principal1 = userManager.getPrincipal("Administrator");
        NuxeoPrincipal principal2 = new NuxeoPrincipalImpl("nicolas", true);
        JsonAssert json = jsonAssert(Arrays.asList(principal1, principal2));
        json.isObject();
        json.properties(2);
        json.has("entity-type").isEquals("users");
        json = json.has("entries").length(2);
        String entryType = "user";
        json.childrenContains("entity-type", entryType, entryType);
        json.childrenContains("id", "Administrator", "nicolas");
    }

}

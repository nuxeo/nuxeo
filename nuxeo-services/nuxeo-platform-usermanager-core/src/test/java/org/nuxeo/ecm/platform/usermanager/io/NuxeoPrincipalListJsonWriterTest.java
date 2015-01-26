/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.usermanager.io;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.core.cache",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.platform.usermanager" })
@LocalDeploy({ "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml" })
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

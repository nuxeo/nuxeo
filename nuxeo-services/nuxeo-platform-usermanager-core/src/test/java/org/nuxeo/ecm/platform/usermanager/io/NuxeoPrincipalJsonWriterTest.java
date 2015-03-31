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

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.core.cache",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.platform.usermanager" })
@LocalDeploy({ "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml" })
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
        model.has("lastName").isEquals("");
        model.has("username").isEquals("Administrator");
        model.has("email").isEquals("Administrator@example.com");
        model.has("company").isEquals("");
        model.has("firstName").isEquals("");
        model.has("password").isEquals("");
        model.has("groups").contains("administrators");
        JsonAssert exGroup = json.has("extendedGroups").length(1).has(0);
        exGroup.properties(3);
        exGroup.has("name").isEquals("administrators");
        exGroup.has("label").isEquals("Administrators group");
        exGroup.has("url").isEquals("group/administrators");
    }

}

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
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.core.cache",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.platform.usermanager" })
@LocalDeploy({ "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml" })
public class NuxeoGroupJsonWriterTest extends AbstractJsonWriterTest.External<NuxeoGroupJsonWriter, NuxeoGroup> {

    public NuxeoGroupJsonWriterTest() {
        super(NuxeoGroupJsonWriter.class, NuxeoGroup.class);
    }

    @Inject
    private UserManager userManager;

    @Test
    public void test() throws Exception {
        NuxeoGroup group = userManager.getGroup("administrators");
        JsonAssert json = jsonAssert(group);
        json.isObject();
        json.properties(5);
        json.has("entity-type").isEquals("group");
        json.has("groupname").isEquals("administrators");
        json.has("grouplabel").isEquals("Administrators group");
        json.has("memberUsers").contains("Administrator");
        json.has("memberGroups").length(0);
    }

}

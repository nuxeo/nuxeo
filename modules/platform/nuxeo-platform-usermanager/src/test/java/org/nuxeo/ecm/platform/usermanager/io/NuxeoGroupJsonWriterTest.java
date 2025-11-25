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

import static org.nuxeo.ecm.directory.api.DirectoryConstants.SYSTEM_ID_PROPERTY;
import static org.nuxeo.ecm.directory.api.DirectoryConstants.SYSTEM_SCHEMA;

import java.util.UUID;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class NuxeoGroupJsonWriterTest extends AbstractJsonWriterTest.External<NuxeoGroupJsonWriter, NuxeoGroup> {

    @Inject
    private UserManager userManager;

    @Test
    public void test() throws Exception {
        NuxeoGroup group = userManager.getGroup("administrators");
        JsonAssert json = jsonAssert(group);
        json.isObject();
        json.properties(6);
        json.has("entity-type").isEquals("group");
        json.has("groupname").isEquals("administrators");
        json.has("grouplabel").isEquals("Administrators group");
        json.has("id").isEquals("administrators");
        json.has("name").isEquals("administrators");
        JsonAssert properties = json.has("properties").properties(4);
        properties.has("groupname").isEquals("administrators");
        properties.has("grouplabel").isEquals("Administrators group");
        properties.has("description").isEquals("Group of users with adminstrative rights");
        properties.has("tenantId").isNull();
    }

    // TODO NXP-33314 copy the assertion on id to tests above and remove this method
    @Test
    public void testWithExternalId() throws Exception {
        SimpleDocumentModel model = SimpleDocumentModel.ofSchemas("group", SYSTEM_SCHEMA);
        model.setPropertyValue("groupname", "test");
        model.setPropertyValue(SYSTEM_ID_PROPERTY, UUID.randomUUID().toString());
        NuxeoGroup group = new NuxeoGroupImpl(model, new GroupConfig());
        JsonAssert json = jsonAssert(group);
        json.isObject();
        // it has no properties
        json.properties(6);
        json.has("entity-type").isEquals("group");
        json.has("id").isUUID();
        json.has("name").isEquals("test");
        JsonAssert properties = json.has("properties").properties(4);
        properties.has("groupname").isEquals("test");
        properties.has("grouplabel").isNull();
        properties.has("description").isNull();
        properties.has("tenantId").isNull();
    }
}

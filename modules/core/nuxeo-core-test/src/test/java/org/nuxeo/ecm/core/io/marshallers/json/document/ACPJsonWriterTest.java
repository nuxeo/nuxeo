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

package org.nuxeo.ecm.core.io.marshallers.json.document;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class ACPJsonWriterTest extends AbstractJsonWriterTest.Local<ACPJsonWriter, ACP> {

    public ACPJsonWriterTest() {
        super(ACPJsonWriter.class, ACP.class);
    }

    private DocumentModel document;

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "myDoc", "Document");
        document = session.createDocument(document);
        UserEntry entry = new UserEntryImpl("SpongeBob");
        entry.addPrivilege("DancingWithPatrickStar");
        document.getACP().setRules("SpongeBobRules", new UserEntry[] { entry });
    }

    @Test
    public void test() throws Exception {
        JsonAssert json = jsonAssert(document.getACP());
        json.isObject();
        json.properties(2);
        json.has("entity-type").isEquals("acls");
        json = json.has("acl").isArray();
        json.length(2);
        json.has(1).has("name").isEquals("inherited");
        json = json.has(0);
        json.has("name").isEquals("SpongeBobRules");
        json = json.has("ace").length(1);
        json = json.has(0).isObject();
        json.has("username").isEquals("SpongeBob");
        json.has("permission").isEquals("DancingWithPatrickStar");
        json.has("granted").isTrue();
    }

}

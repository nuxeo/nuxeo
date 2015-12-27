/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.restapi.jaxrs.io.documents.ACPWriter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @deprecated since 7.10 see {@link ACPWriter}
 */
@Deprecated
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.io" })
public class ACPWriterTest {

    @Inject
    JsonFactory factory;

    @Test
    public void writeACP() throws Exception {
        // Given an ACP
        ACP acp = getTestACP();

        // When i write this ACP
        JsonNode node = writeAcpAsJson(acp);

        // Then the json reflects the acp properties
        assertEquals(ACPWriter.ENTITY_TYPE, node.get("entity-type").getValueAsText());
        JsonNode aclNode = node.get("acl").get(0);
        assertEquals("local", aclNode.get("name").getValueAsText());

        ArrayNode aceNode = (ArrayNode) aclNode.get("ace");

        assertEquals(acp.getACL("local").size(), aceNode.size());
    }

    /**
     * Return a parsed Json representation of an ACP.
     *
     * @param acp
     * @return
     * @throws IOException
     */
    private JsonNode writeAcpAsJson(ACP acp) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = JsonHelper.createJsonGenerator(factory, out);

        ACPWriter acpWriter = new ACPWriter();
        acpWriter.writeEntity(jg, acp);

        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.readTree(out.toString());
        return node;
    }

    /**
     * @return
     */
    private ACP getTestACP() {
        ACP acp = new ACPImpl();
        ACLImpl acl = new ACLImpl("local");

        acl.add(new ACE("John", "READ", true));
        acl.add(new ACE("Paul", "READ", true));
        acl.add(new ACE("Ringo", "WRITE", true));
        acp.addACL(acl);
        return acp;
    }
}

/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.restapi.jaxrs.io.documents.ACPWriter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

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
        assertEquals(ACPWriter.ENTITY_TYPE,
                node.get("entity-type").getValueAsText());
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
     * @throws ClientException
     *
     */
    private JsonNode writeAcpAsJson(ACP acp) throws IOException,
            ClientException {
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
     *
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

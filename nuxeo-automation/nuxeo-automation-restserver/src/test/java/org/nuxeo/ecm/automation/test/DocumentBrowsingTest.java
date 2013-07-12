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
package org.nuxeo.ecm.automation.test;

import static org.junit.Assert.assertEquals;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.AutomationServerComponent;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 *
 *
 * @since TODO
 */

@RunWith(FeaturesRunner.class)
@Features({RuntimeFeature.class,  EmbeddedAutomationServerFeature.class })
@Deploy("nuxeo-automation-restserver")
@LocalDeploy({ "nuxeo-automation-restserver:adapter-contrib.xml"})
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentBrowsingTest {

    private WebResource service;

    @Inject
    CoreSession session;

    @BeforeClass
    public static void setupCodecs() throws Exception {
        // Fire application start on AutomationServer component forcing to load
        // correctly Document Adapter Codec in Test scope (to take into account
        // of document adapters contributed into test) -> see execution order
        // here: org.nuxeo.runtime.test.runner.RuntimeFeature.start()

        ComponentInstance componentInstance = Framework.getRuntime().getComponentInstance(
                "org.nuxeo.ecm.automation.server.AutomationServer");
        AutomationServerComponent automationServerComponent = (AutomationServerComponent) componentInstance.getInstance();
        automationServerComponent.applicationStarted(componentInstance);
    }




    @Before
    public void doBefore() {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter("Administrator",
                "Administrator"));
        service = client.resource("http://localhost:18090/api/");

    }




    @Test
    public void iCanBrowseTheRepo() throws Exception {

        DocumentModel note = RestServerInit.getNote(0, session);

        ClientResponse response = service.path("path" + note.getPathAsString()).accept(
                MediaType.APPLICATION_JSON).get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);

        assertEquals("document", node.get("entity-type").getValueAsText());
        assertEquals(note.getPathAsString(), node.get("path").getValueAsText());
        assertEquals(note.getId(), node.get("uid").getValueAsText());
        assertEquals(note.getTitle(), node.get("title").getValueAsText());

    }

}

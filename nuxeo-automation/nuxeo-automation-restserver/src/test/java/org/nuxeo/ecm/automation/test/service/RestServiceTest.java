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
package org.nuxeo.ecm.automation.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.rest.service.HeaderDocEvaluationContext;
import org.nuxeo.ecm.automation.rest.service.RestContributor;
import org.nuxeo.ecm.automation.rest.service.RestContributorService;
import org.nuxeo.ecm.automation.rest.service.RestContributorServiceImpl;
import org.nuxeo.ecm.automation.rest.service.RestEvaluationContext;
import org.nuxeo.ecm.automation.server.AutomationServerComponent;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentWriter;
import org.nuxeo.ecm.automation.test.RestServerInit;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 *
 *
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
@Deploy({ "nuxeo-automation-server", "nuxeo-automation-restserver" })
@LocalDeploy("nuxeo-automation-restserver:testrestcontrib.xml")
public class RestServiceTest {

    @Inject
    RestContributorService rcs;

    @Inject
    CoreSession session;

    @Test
    public void itCanGetTheRestContributorService() throws Exception {
        assertNotNull(rcs);
    }

    @Test
    public void itCanGetContributorsFromTheService() throws Exception {
        List<RestContributor> cts = rcs.getContributors("test", null);
        assertEquals(1, cts.size());
    }

    @Test
    public void itCanFilterContributorsByCategory() throws Exception {
        List<RestContributor> cts = rcs.getContributors("anothertest", null);
        assertEquals(2, cts.size());
    }

    @Test
    public void itCanWriteToContext() throws Exception {

        // Given some input context (header + doc)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        DocumentModel folder = RestServerInit.getFolder(1, session);
        RestEvaluationContext ec = new HeaderDocEvaluationContext(folder,
                getFakeHeaders());

        // When the service write to the context
        jg.writeStartObject();
        rcs.writeContext(jg, ec);
        jg.writeEndObject();
        jg.flush();

        // Then it is filled with children contributor
        String json = out.toString();
        ObjectMapper m = new ObjectMapper();
        JsonNode node = m.readTree(json);
        assertEquals("documents",
                node.get("children").get("entity-type").getValueAsText());

    }

    private JsonGenerator getJsonGenerator(OutputStream out) throws IOException {
        JsonGenerator jg = AutomationServerComponent.me.getFactory().createJsonGenerator(
                out);
        return jg;
    }

    private HttpHeaders getFakeHeaders() {
        HttpHeaders headers = mock(HttpHeaders.class);

        when(
                headers.getRequestHeader(JsonDocumentWriter.DOCUMENT_PROPERTIES_HEADER)).thenReturn(
                Arrays.asList(new String[] {}));

        when(
                headers.getRequestHeader(RestContributorServiceImpl.NXCONTENT_CATEGORY_HEADER)).thenReturn(
                Arrays.asList(new String[] { "test" }));
        return headers;
    }

}

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
 */
package org.nuxeo.ecm.automation.core.test.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.users.GetNuxeoPrincipal;
import org.nuxeo.ecm.automation.core.operations.users.SuggestUserEntries;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.automation.features:test-user-directories-contrib.xml")
public class GetNuxeoUserTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    protected OperationContext ctx;

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void shouldRetrieveCurrentPrincipalAsDoc() throws Exception {

        // test without params

        OperationChain chain = new OperationChain("fakeChain");
        chain.add(GetNuxeoPrincipal.ID);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals("Jacky", doc.getPropertyValue("user:firstName"));
        assertEquals("Chan", doc.getPropertyValue("user:lastName"));
        assertEquals("Nuxeo", doc.getPropertyValue("user:company"));
        assertEquals("devnull@nuxeo.com", doc.getPropertyValue("user:email"));

    }

    @Test
    public void shouldRetrievePrincipalAsDoc() throws Exception {

        // test with params

        Map<String, Object> params = new HashMap<>();
        params.put("login", "jdoe");

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(GetNuxeoPrincipal.ID, params);
        chain.add(oparams);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals("John", doc.getPropertyValue("user:firstName"));
        assertEquals("Doe", doc.getPropertyValue("user:lastName"));
        assertEquals("Nuxeo", doc.getPropertyValue("user:company"));
        assertEquals("jdoe@nuxeo.com", doc.getPropertyValue("user:email"));

    }

    /**
     * @since 10.2
     */
    @Test
    public void shouldSuggestProperEntries() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("searchTerm", "jdoe");
        OperationParameters oparams = new OperationParameters(SuggestUserEntries.ID, params);

        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> entries = mapper.readValue(result.getString(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(1, entries.size());

        Map<String, Object> entry = entries.get(0);
        assertEquals("jdoe", entry.get("id"));
        assertEquals(NuxeoPrincipalJsonWriter.ENTITY_TYPE, entry.get(MarshallingConstants.ENTITY_FIELD_NAME));
    }

}

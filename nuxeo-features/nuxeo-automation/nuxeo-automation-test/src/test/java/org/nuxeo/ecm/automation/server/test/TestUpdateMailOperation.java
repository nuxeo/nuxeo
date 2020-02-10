/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.automation.server.test.operations.UpdateMailOperation.TEST_EMAIL;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.test.operations.UpdateMailOperation;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.automation.test.test:operation-contrib.xml")
public class TestUpdateMailOperation {

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected HttpAutomationSession session;

    @Test
    public void testOperation() throws IOException {

        JsonNode updatedUsers = session.newRequest(UpdateMailOperation.ID)
                                       .set("users", List.of(coreSession.getPrincipal().getName()))
                                       .execute();

        assertNotNull(updatedUsers);

        List<NuxeoPrincipal> users = MarshallerHelper.jsonToList(NuxeoPrincipal.class, updatedUsers.toString(),
                RenderingContext.CtxBuilder.get());
        assertEquals(TEST_EMAIL, users.get(0).getEmail());

    }
}

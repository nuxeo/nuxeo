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

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.rest.jaxrs.adapters.OperationAdapter;
import org.nuxeo.ecm.automation.test.helpers.OperationCall;
import org.nuxeo.ecm.automation.test.helpers.TestOperation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Test the Rest binding to run operations
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@LocalDeploy({ "nuxeo-automation-restserver:operation-contrib.xml" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OperationBindingTest extends BaseTest {

    private static String PARAMS = "{\"params\":{\"one\":\"1\",\"two\": 2}}";

    @Override
    @Before
    public void doBefore() {
        super.doBefore();
        TestOperation.reset();
    }

    @Test
    public void itCanRunAnOperationOnaDocument() throws Exception {

        // Given a document and an operation
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i call the REST binding on the document resource
        ClientResponse response = getResponse(RequestType.POSTREQUEST, "id/"
                + note.getId() + "/@" + OperationAdapter.NAME + "/testOp",
                PARAMS);

        // Then the operation is called on the document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<OperationCall> calls = TestOperation.getCalls();
        assertEquals(1, calls.size());

        OperationCall call = calls.get(0);
        assertEquals("1", call.getParamOne());
        assertEquals(2, call.getParamTwo());
        assertEquals(note.getId(), call.getDocument().getId());
    }

    @Test
    public void itCanRunAChainOnADocument() throws Exception {
        // Given a document and an operation
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i call the REST binding on the document resource
        ClientResponse response = getResponse(RequestType.POSTREQUEST, "id/"
                + note.getId() + "/@" + OperationAdapter.NAME
                + "/Chain.testChain", "{}");

        // Then the operation is called twice on the document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<OperationCall> calls = TestOperation.getCalls();
        assertEquals(2, calls.size());

        OperationCall call = calls.get(0);
        assertEquals("One", call.getParamOne());
        assertEquals(2, call.getParamTwo());
        assertEquals(note.getId(), call.getDocument().getId());

        call = calls.get(1);
        assertEquals("Two", call.getParamOne());
        assertEquals(4, call.getParamTwo());

    }

    @Test
    public void itCanRunAChainOnMutlipleDocuments() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When i call the REST binding on the children resource

        getResponse(RequestType.POSTREQUEST, "id/" + folder.getId()
                + "/@children/@" + OperationAdapter.NAME + "/testOp", PARAMS);

        // Then the operation is called on all children documents
        List<OperationCall> calls = TestOperation.getCalls();
        assertEquals(session.getChildren(folder.getRef()).size(), calls.size());

    }

}

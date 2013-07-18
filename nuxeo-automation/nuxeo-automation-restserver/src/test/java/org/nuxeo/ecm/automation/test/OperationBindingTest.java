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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.helpers.OperationCall;
import org.nuxeo.ecm.automation.test.helpers.TestOperation;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * Test the Rest binding to run operations
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy("nuxeo-automation-restserver")
@LocalDeploy({ "nuxeo-automation-restserver:adapter-contrib.xml",
        "nuxeo-automation-restserver:operation-contrib.xml" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OperationBindingTest extends BaseTest {

    @Inject
    CoreSession session;

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
        String params = "{\"params\":{\"one\":\"1\",\"two\": 2}}";
        getResponse(RequestType.POSTREQUEST, "id/" + note.getId()
                + "/@op/testOp", params);

        // Then the operation is called on the document

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
        getResponse(RequestType.POSTREQUEST, "id/" + note.getId()
                + "/@op/Chain.testChain","{}");

        // Then the operation is called twice on the document

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

}

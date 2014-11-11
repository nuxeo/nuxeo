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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Test registration of operation chains on service.
 *
 * @sincze 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-registration-chain.xml")
public class OperationChainRegistrationTest {

    @Inject
    AutomationService service;

    /**
     * Check chain properties when using escaped params in operations
     */
    @Test
    public void testChainWithEscapedParams() throws Exception {
        OperationChain chain = service.getOperationChain("chainWithEscapedParams");
        assertEquals(2, chain.getOperations().size());
        OperationParameters params = chain.getOperations().get(1);
        assertEquals(2, params.map().size());
        assertEquals("NXQL", params.map().get("language"));
        assertEquals("SELECT * FROM Document WHERE dc:created"
                + "\n          < DATE '2013-08-19'",
                params.map().get("query"));
    }

}

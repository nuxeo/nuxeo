/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Test for parameter key alias.
 *
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
public class OperationAliasTest {

    private final static String HELLO_WORLD = "Hello World!";

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    /**
     * Call an operation which has a parameter with an alias. Don't give the
     * name of the parameter but its alias and check it is resolved thanks its
     * alias.
     *
     * @since 5.9.2
     */
    @Test
    public void testAliasOnOperationParam() throws InvalidChainException, OperationException, Exception {
        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("paramAlias", HELLO_WORLD);

        OperationChain chain = new OperationChain("testChain");
        chain.add(ParamNameWithAliasOperation.ID).set(ParamNameWithAliasOperation.ALIAS1, HELLO_WORLD);

        Object result = service.run(ctx, chain);
        assertNotNull(result);
        assertTrue(result instanceof String);
        assertEquals(HELLO_WORLD, result);

        chain = new OperationChain("testChain");
        chain.add(ParamNameWithAliasOperation.ID).set(ParamNameWithAliasOperation.ALIAS2, HELLO_WORLD);
        result = service.run(ctx, chain);
        assertNotNull(result);
        assertTrue(result instanceof String);
        assertEquals(HELLO_WORLD, result);
    }
}

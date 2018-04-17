/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * Test for parameter key alias.
 *
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-operations-alias.xml")
public class OperationAliasTest {

    private final static String HELLO_WORLD = "Hello World!";

    @Inject
    HotDeployer deployer;

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    /**
     * Call an operation which has a parameter with an alias. Don't give the name of the parameter but its alias and
     * check it is resolved thanks its alias.
     *
     * @since 5.9.2
     */
    @Test
    public void testAliasOnOperationParam() throws InvalidChainException, OperationException, Exception {
        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<>();
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

    /**
     * Call an operation with its alias.
     *
     * @since 7.1
     */
    @Test
    public void testAliasOnOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        params.put("paramName", HELLO_WORLD);
        Object result = service.run(ctx, ParamNameWithAliasOperation.ALIAS_OP, params);
        assertNotNull(result);
    }

    /**
     * Call a chain with its alias.
     *
     * @since 7.1
     */
    @Test
    public void testAliasOnChain() throws Exception {
        // We can call a chain with its alias.
        OperationContext ctx = new OperationContext(session);
        Object result = service.run(ctx, "chainAlias2");
        assertNotNull(result);
        // And the another chain with its alias containing an operation named
        // with its alias.
        result = service.run(ctx, "chainAlias3");
        assertNotNull(result);
    }

    @Test
    public void testAliasesDocumentation() throws Exception {
        List<OperationDocumentation> documentation = service.getDocumentation();

        OperationDocumentation operationDoc = documentation.stream()
                                                           .filter(od -> od.id.equals(ParamNameWithAliasOperation.ID))
                                                           .findFirst()
                                                           .orElse(null);
        assertNotNull(operationDoc);
        assertEquals(ParamNameWithAliasOperation.ID, operationDoc.id);

        operationDoc = documentation.stream()
                                    .filter(od -> od.id.equals(ParamNameWithAliasOperation.ALIAS_OP))
                                    .findFirst()
                                    .orElse(null);
        assertNull(operationDoc);

        deployer.deploy("org.nuxeo.ecm.automation.core:test-export-alias-config.xml");

        documentation = service.getDocumentation();

        operationDoc = documentation.stream()
                                    .filter(od -> od.id.equals(ParamNameWithAliasOperation.ALIAS_OP))
                                    .findFirst()
                                    .orElse(null);
        assertNotNull(operationDoc);
        assertEquals(ParamNameWithAliasOperation.ALIAS_OP, operationDoc.id);
        assertNull(operationDoc.aliases);
    }

}

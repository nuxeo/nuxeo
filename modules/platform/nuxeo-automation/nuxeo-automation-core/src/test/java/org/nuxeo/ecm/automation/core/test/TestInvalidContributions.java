/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test invalid contributions to operation extension points.
 *
 * @since 11.3
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class TestInvalidContributions {

    protected void checkStartupError(String message) {
        List<String> errors = Framework.getRuntime().getMessageHandler().getErrors();
        assertEquals(1, errors.size());
        assertEquals(message, errors.get(0));
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-invalid-operation.xml")
    public void testInvalidOperation() {
        checkStartupError(
                "Failed to register extension to: service:org.nuxeo.ecm.core.operation.OperationServiceComponent, "
                        + "xpoint: operations in component: service:org.nuxeo.automation.rest.test.faultyOperationContrib "
                        + "(java.lang.IllegalArgumentException: Invalid operation class: class org.nuxeo.ecm.automation.core.AutomationComponent. "
                        + "No @Operation annotation found on class.)");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-invalid-operation-notfound.xml")
    public void testInvalidOperationNotFound() {
        checkStartupError(
                "Failed to register extension to: service:org.nuxeo.ecm.core.operation.OperationServiceComponent, "
                        + "xpoint: operations in component: service:org.nuxeo.automation.rest.test.faultyOperationContrib "
                        + "(java.lang.IllegalArgumentException: Invalid operation class 'org.nuxeo.ecm.automation.test.helpers.NonExistingOperation': "
                        + "class not found.)");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-invalid-chain.xml")
    public void testInvalidChain() {
        checkStartupError(
                "Failed to register extension to: service:org.nuxeo.ecm.core.operation.OperationServiceComponent, "
                        + "xpoint: chains in component: service:org.nuxeo.automation.rest.test.faultyOperationContrib "
                        + "(java.lang.RuntimeException: org.nuxeo.ecm.automation.OperationException: Operation with id "
                        + "'NonExistingOperation' could not be found.)");
    }

}

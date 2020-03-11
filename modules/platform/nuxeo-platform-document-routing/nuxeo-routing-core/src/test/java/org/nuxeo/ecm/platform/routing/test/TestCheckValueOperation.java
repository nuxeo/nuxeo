/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;


@Operation(id = TestCheckValueOperation.ID, //
        category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, //
        label = "Check value")
public class TestCheckValueOperation {

    public final static String ID = "Document.Routing.Test.CheckValue";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "expected", required = true)
    protected String expected;

    @Param(name = "actual", required = true)
    protected String actual;

    @OperationMethod
    public void run() {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("Expected value is " + expected);
        }
    }

}

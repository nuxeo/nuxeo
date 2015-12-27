/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test.helpers;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.jaxrs.io.operations.RestOperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @since 7.1
 */
@Operation(id = HttpStatusOperationTest.ID, category = Constants.CAT_SERVICES,
        label = "Test Custom Http Status", description = "Test Custom Http " +
        "Status")
public class HttpStatusOperationTest {

    public static final String ID = "Test.HttpStatus";

    @Param(name = "isFailing")
    protected boolean isFailing = true;

    @Context
    RestOperationContext context;

    @Context
    CoreSession session;

    @OperationMethod()
    public Object run() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        // If context is instanceof RestOperationContext when jaxrs call is
        // executed
        if (context != null) {
            if (isFailing) {
                ExceptionTest exception = new ExceptionTest("Exception " +
                        "Message");
                exception.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                throw exception;
            } else {
                context.setHttpStatus(HttpServletResponse
                        .SC_PARTIAL_CONTENT);
            }
        }// else context is instanceof OperationContext
        return root;
    }

}

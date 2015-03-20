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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test.helpers;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.jaxrs.io.operations.RestOperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

import javax.servlet.http.HttpServletResponse;

/**
 * @since 7.1
 */
@Operation(id = HttpStatusOperationTest.ID, category = Constants.CAT_SERVICES, label = "Test Custom Http Status", description = "Test Custom Http "
        + "Status")
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
                ExceptionTest exception = new ExceptionTest("Exception " + "Message");
                exception.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                throw exception;
            } else {
                context.setHttpStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        }// else context is instanceof OperationContext
        return root;
    }

}

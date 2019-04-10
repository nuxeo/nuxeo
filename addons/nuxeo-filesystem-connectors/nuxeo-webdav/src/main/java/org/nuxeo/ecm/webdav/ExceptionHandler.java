/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webdav;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.nuxeo.ecm.core.api.RecoverableClientException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Simple error handler to give back a user-readable status, and log it to the console.
 * <p>
 * This is a convenience for trouble-shouting.
 */
@Provider
public class ExceptionHandler implements ExceptionMapper<Exception> {

    private static final Log log = LogFactory.getLog(ExceptionHandler.class);

    @Override
    public Response toResponse(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        int status = 500;
        // String msg;
        if (e instanceof WebApplicationException) {
            status = ((WebApplicationException) e).getResponse().getStatus();
            if (status < 400 || status >= 500) {
                log.error("Status = " + status);
                log.error(e, e);
            }
            // msg = "Error " + status + "\n" + e.getMessage() + "\n" + sw;
        } else if (e.getCause() instanceof RecoverableClientException
                && ("QuotaExceededException".equals(e.getCause().getClass().getSimpleName()))) {
            status = HttpStatus.SC_INSUFFICIENT_STORAGE; // 507
            log.debug(e, e);
        } else {
            log.error(e, e);
            // msg = "Error\n\n" + e.getMessage() + "\n\n" + sw;
        }
        return Response.status(status).build();
    }

}

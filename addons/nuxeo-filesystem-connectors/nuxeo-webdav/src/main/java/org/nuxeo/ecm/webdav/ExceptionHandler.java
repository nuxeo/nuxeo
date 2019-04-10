/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

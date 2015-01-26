/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

import com.sun.jersey.api.NotFoundException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
public class WebEngineExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    HttpHeaders headers;

    protected static final Log log = LogFactory.getLog(WebEngineExceptionMapper.class);

    @Override
    public Response toResponse(Throwable cause) {
        if (headers.getAcceptableMediaTypes().contains(APPLICATION_JSON_TYPE)) {
            if (cause instanceof DocumentValidationException) {
                DocumentValidationException dve = (DocumentValidationException) cause;
                return Response.status(Status.BAD_REQUEST).entity(dve.getReport()).build();
            }
        }
        if (cause instanceof NotFoundException) {
            NotFoundException nfe = (NotFoundException) cause;
            log.debug("JAX-RS 404 Not Found: " + nfe.getNotFoundUri());
        } else if (cause instanceof WebResourceNotFoundException) {
            WebResourceNotFoundException nfe = (WebResourceNotFoundException) cause;
            log.debug("JAX-RS 404 Not Found: " + nfe.getMessage());
        } else {
            log.warn("Exception in JAX-RS processing", cause);
        }
        return WebException.newException(cause.getMessage(), WebException.wrap(cause)).toResponse();
    }

}

/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.runtime.transaction.TransactionHelper;

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
        TransactionHelper.setTransactionRollbackOnly();
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

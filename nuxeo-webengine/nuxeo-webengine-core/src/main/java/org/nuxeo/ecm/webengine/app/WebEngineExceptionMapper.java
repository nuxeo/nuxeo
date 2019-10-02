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

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

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
                return Response.status(dve.getStatusCode()).entity(dve.getReport()).build();
            }
        }

        // backward compatibility
        if (cause instanceof WebException) {
            return ((WebException) cause).toResponse();
        }

        // webengine custom error handling, if any
        Object result = handleErrorOnWebModule(cause);
        if (result instanceof Throwable) {
            cause = (Throwable) result;
        } else if (result instanceof Response) {
            return (Response) result;
        } else if (result != null) {
            return Response.status(SC_INTERNAL_SERVER_ERROR).entity(result).build();
        }

        int statusCode = getStatusCode(cause);
        if (statusCode >= SC_INTERNAL_SERVER_ERROR) {
            log.error(cause, cause);
        } else {
            log.debug(cause, cause);
        }
        // make sure we have a NuxeoException
        return Response.status(statusCode)
                       .entity(cause instanceof NuxeoException ? cause : new NuxeoException(cause, statusCode))
                       .build();
    }

    protected static int getStatusCode(Throwable t) {
        if (t instanceof WebException) {
            WebException webException = (WebException) t;
            return webException.getStatusCode();
        } else if (t instanceof WebApplicationException) {
            WebApplicationException e = (WebApplicationException) t;
            return e.getResponse().getStatus();
        } else if (t instanceof NuxeoException) {
            NuxeoException e = (NuxeoException) t;
            return e.getStatusCode();
        } else if (t instanceof SecurityException) {
            return SC_FORBIDDEN;
        }

        Throwable cause = t.getCause();
        if (cause == null || t == cause) {
            return SC_INTERNAL_SERVER_ERROR;
        }
        return getStatusCode(cause);
    }

    protected static Object handleErrorOnWebModule(Throwable t) {
        WebContext ctx = WebEngine.getActiveContext();
        if (ctx != null && ctx.head() instanceof ModuleResource) {
            ModuleResource mr = (ModuleResource) ctx.head();
            return mr.handleError(t);
        }
        return null;
    }

}

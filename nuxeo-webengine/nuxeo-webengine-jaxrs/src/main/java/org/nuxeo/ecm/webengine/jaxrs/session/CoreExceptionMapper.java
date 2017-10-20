/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.jaxrs.session;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 9.3.
 */
@Deprecated
public class CoreExceptionMapper implements ExceptionMapper<Throwable> {

    protected static final Log log = LogFactory.getLog(CoreExceptionMapper.class);

    @Override
    public Response toResponse(Throwable t) {
        log.error("Exception in JAX-RS processing", t);
        if (t instanceof WebApplicationException) {
            return ((WebApplicationException) t).getResponse();
        } else if (t instanceof DocumentSecurityException) {
            return getResponse(t, 401);
        } else if (t instanceof DocumentNotFoundException) {
            return getResponse(t, 404);
        }
        return getResponse(t, 500);
    }

    public static Response getResponse(Throwable t, int status) {
        String message = status == 500 ? getStackTrace(t) : null;
        return Response.status(status).entity(message).build();
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

}

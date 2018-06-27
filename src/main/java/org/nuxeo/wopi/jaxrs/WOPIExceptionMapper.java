/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 *     Thomas Roger
 */
package org.nuxeo.wopi.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.nuxeo.wopi.exception.WOPIException;

/**
 * Exception mapper for the {@link WOPIException}s.
 *
 * @since 10.3
 */
@Provider
public class WOPIExceptionMapper implements ExceptionMapper<WOPIException> {

    @Override
    public Response toResponse(WOPIException exception) {
        return Response.status(exception.getStatusCode()).build();
    }

}

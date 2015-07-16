/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.restapi.server.jaxrs.resource.wro;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.restapi.server.jaxrs.resource.wro.ResourceBundleEndpoint.ResourceBundleDispatcher;

/**
 * Writer for a resource bundle, used to dispatch jax-rs call internally to the wro servlet.
 *
 * @since 7.3
 */
@Provider
public class ResourceBundleWriter implements MessageBodyWriter<ResourceBundleDispatcher> {

    private static final Log log = LogFactory.getLog(ResourceBundleEndpoint.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ServletContext servletContext;

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Override
    public long getSize(ResourceBundleDispatcher arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return ResourceBundleDispatcher.class.isAssignableFrom(arg0);
    }

    @Override
    public void writeTo(ResourceBundleDispatcher arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException, WebApplicationException {
        try {
            URI uri = uriInfo.getRequestUri();
            String path = uri.getPath();
            // remove lead /nuxeo
            path = path.replaceFirst(servletContext.getContextPath(), "");
            // redirect to the wro servlet path
            path = path.replaceFirst("/site/api/", "/wapi/");
            URI dispatch = new URI(null, null, path, uri.getQuery(), uri.getFragment());
            servletContext.getRequestDispatcher(dispatch.toString()).forward(request, response);
        } catch (URISyntaxException | ServletException e) {
            log.error("Error while forwarding to Wro servlet", e);
        }
    }

}

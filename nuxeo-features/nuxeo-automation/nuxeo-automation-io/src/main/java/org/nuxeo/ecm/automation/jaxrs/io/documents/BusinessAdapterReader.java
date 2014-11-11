/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * JAX-RS Message body reeader to decode BusinessAdapter
 *
 * @since 5.7.2
 */
@Provider
@Consumes({"application/json+nxentity","application/json"})
public class BusinessAdapterReader implements MessageBodyReader<BusinessAdapter>{


    @Context
    protected HttpServletRequest request;

    @Context
    JsonFactory factory;

    private CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return BusinessAdapter.class.isAssignableFrom(type);
    }

    @Override
    public BusinessAdapter readFrom(Class<BusinessAdapter> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> headers, InputStream in)
            throws IOException, WebApplicationException {
            return readRequest(in,headers);
    }

    public BusinessAdapter readRequest(InputStream in,
            MultivaluedMap<String, String> headers) throws IOException, WebApplicationException {
        // As stated in http://tools.ietf.org/html/rfc4627.html UTF-8 is the
        // default encoding for JSON content
        // TODO: add introspection on the first bytes to detect other admissible
        // json encodings, namely: UTF-8, UTF-16 (BE or LE), or UTF-32 (BE or LE)
        String content = IOUtils.toString(in, "UTF-8");
        if (content.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return readRequest(content, headers);
    }

    public BusinessAdapter readRequest(String content, MultivaluedMap<String, String> headers) throws WebApplicationException {
        try {
            return readRequest0(content, headers);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    public BusinessAdapter readRequest0(String content, MultivaluedMap<String, String> headers) throws Exception {
        ObjectCodecService codecService = Framework.getLocalService(ObjectCodecService.class);

        JsonParser jp = factory.createJsonParser(content);
        JsonNode inputNode = jp.readValueAsTree();

        return  (BusinessAdapter) codecService.readNode(inputNode, getCoreSession());

    }

}

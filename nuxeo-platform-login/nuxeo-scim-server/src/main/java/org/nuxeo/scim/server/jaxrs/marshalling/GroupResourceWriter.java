/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.scim.server.jaxrs.marshalling;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.sdk.SCIMException;

/**
 * Handles marshaling for SCIM {@link GroupResource}
 *
 * @author tiry
 * @since 7.4
 */
@Provider
@Produces({ "application/xml", "application/json" })
public class GroupResourceWriter implements MessageBodyWriter<GroupResource> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return GroupResource.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(GroupResource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(GroupResource t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {

        try {
            Marshaller marshaller = null;
            if (mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                marshaller = new XmlMarshaller();
            } else {
                marshaller = new JsonMarshaller();
            }
            marshaller.marshal(t, entityStream);
        } catch (SCIMException e) {
            throw new WebApplicationException(e);
        }
    }

}

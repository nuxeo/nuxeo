/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
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

import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.sdk.SCIMException;

/**
 * Handles marshaling for SCIM {@link UserResource}
 *
 * @author tiry
 * @since 7.4
 */
@Provider
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public class UserResourceWriter implements MessageBodyWriter<UserResource> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return UserResource.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(UserResource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(UserResource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
            WebApplicationException {

        try {
            Marshaller marshaller = null;
            httpHeaders.remove("Content-Type");
            if (t instanceof UserResourceWithMimeType) {
                if (((UserResourceWithMimeType) t).getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                    marshaller = new JsonMarshaller();
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON);
                } else {
                    marshaller = new XmlMarshaller();
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_XML);
                }
            } else {
                if (mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                    marshaller = new XmlMarshaller();
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_XML);
                } else {
                    marshaller = new JsonMarshaller();
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON);
                }
            }
            marshaller.marshal(t, entityStream);
        } catch (SCIMException e) {
            throw new WebApplicationException(e);
        }
    }

}

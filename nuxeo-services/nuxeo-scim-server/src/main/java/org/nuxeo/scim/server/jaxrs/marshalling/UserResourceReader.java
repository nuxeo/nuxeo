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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.InvalidResourceException;

/**
 * Handles marshaling for SCIM {@link UserResource}
 *
 * @author tiry
 * @since 7.4
 */
@Provider
@Consumes({ "application/xml", "application/json" })
public class UserResourceReader implements MessageBodyReader<UserResource> {

    private static final Log log = LogFactory.getLog(UserResourceReader.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return UserResource.class.isAssignableFrom(type);
    }

    @Override
    public UserResource readFrom(Class<UserResource> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        Unmarshaller unmarshaller = null;
        if (mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)) {
            unmarshaller = new XmlUnmarshaller();
        } else {
            unmarshaller = new NXJsonUnmarshaller();
        }
         try {
            return unmarshaller.unmarshal(entityStream, CoreSchema.USER_DESCRIPTOR, UserResource.USER_RESOURCE_FACTORY);
        } catch (InvalidResourceException e) {
            log.error(e, e);
        }
        return null;
    }

}

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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.InvalidResourceException;

/**
 * Handles marshaling for SCIM {@link GroupResource}
 *
 * @author tiry
 * @since 7.4
 */
@Provider
@Consumes({ "application/xml", "application/json" })
public class GroupResourceReader implements MessageBodyReader<GroupResource> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return GroupResource.class.isAssignableFrom(type);
    }

    @Override
    public GroupResource readFrom(Class<GroupResource> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        Unmarshaller unmarshaller = null;
        if (mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)) {
            unmarshaller = new XmlUnmarshaller();
        } else {
            unmarshaller = new NXJsonUnmarshaller();
        }
        try {
            return unmarshaller.unmarshal(entityStream, CoreSchema.GROUP_DESCRIPTOR,
                    GroupResource.GROUP_RESOURCE_FACTORY);
        } catch (InvalidResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}

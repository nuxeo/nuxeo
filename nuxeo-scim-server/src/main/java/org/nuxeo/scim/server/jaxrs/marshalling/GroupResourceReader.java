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

@Provider
@Consumes({ "application/xml", "application/json" })
public class GroupResourceReader implements MessageBodyReader<GroupResource> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return GroupResource.class.isAssignableFrom(type);
    }

    @Override
    public GroupResource readFrom(Class<GroupResource> type, Type genericType,
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
            return unmarshaller.unmarshal(entityStream, CoreSchema.GROUP_DESCRIPTOR, GroupResource.GROUP_RESOURCE_FACTORY);
        } catch (InvalidResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
        return null;
    }

}

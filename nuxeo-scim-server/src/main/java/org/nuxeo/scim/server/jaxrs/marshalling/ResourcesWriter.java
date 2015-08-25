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

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;

@Provider
@Produces({ "application/xml", "application/json" })
public class ResourcesWriter implements MessageBodyWriter<Resources<BaseResource>> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {        
        return Resources.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Resources<BaseResource> t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {                
        return -1;
    }

    @Override
    public void writeTo(Resources<BaseResource> t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
                
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

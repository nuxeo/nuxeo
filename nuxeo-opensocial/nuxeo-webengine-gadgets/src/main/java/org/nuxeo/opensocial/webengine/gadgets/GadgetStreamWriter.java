package org.nuxeo.opensocial.webengine.gadgets;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;



public class GadgetStreamWriter implements MessageBodyWriter<GadgetStream> {

  public long getSize(GadgetStream t, Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  public boolean isWriteable(Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType) {
    return type.isAssignableFrom(GadgetStream.class);
  }


  public void writeTo(GadgetStream t, Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream) throws IOException,
      WebApplicationException {
    int c;

        while ((c = t.getStream().read()) != -1)
        {
           entityStream.write(c);
        }
        t.getStream().close();

  }
}

package org.nuxeo.template.xdocreport.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingServletOutputStream;

import fr.opensagres.xdocreport.remoting.resources.domain.LargeBinaryData;
import fr.opensagres.xdocreport.remoting.resources.services.rest.LargeBinaryDataMessageBodyWriter;

@Provider
public class NuxeoLargeBinaryDataMessageWriter extends
        LargeBinaryDataMessageBodyWriter {

    @Override
    public void writeTo(LargeBinaryData t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        BufferingServletOutputStream.stopBufferingThread();
        httpHeaders.add("X-Nuxeo", "WebEngine-JAXRS");
        super.writeTo(t, type, genericType, annotations, mediaType,
                httpHeaders, entityStream);
    }
}

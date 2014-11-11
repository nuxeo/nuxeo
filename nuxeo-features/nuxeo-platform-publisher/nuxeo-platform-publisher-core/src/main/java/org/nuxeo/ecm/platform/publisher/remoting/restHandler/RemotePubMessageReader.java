package org.nuxeo.ecm.platform.publisher.remoting.restHandler;

import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;
import org.nuxeo.ecm.webengine.WebEngine;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

public class RemotePubMessageReader implements
        MessageBodyReader<RemotePubParam> {

    public boolean isReadable(Class arg0, Type arg1, Annotation[] arg2,
            MediaType mt) {
        return mt.equals(RemotePubParam.mediaType);
    }

    public RemotePubParam readFrom(Class arg0, Type arg1, Annotation[] arg2,
            MediaType arg3, MultivaluedMap arg4, InputStream is)
            throws IOException, WebApplicationException {

        DefaultMarshaler marshaler = new DefaultMarshaler(
                WebEngine.getActiveContext().getCoreSession());

        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = br.read()) > -1) {
            sb.append((char) ch);
        }
        br.close();

        String xmlData = sb.toString();

        try {
            List<Object> params = marshaler.unMarshallParameters(xmlData);
            return new RemotePubParam(params);
        } catch (PublishingMarshalingException e) {
            throw new IOException("Error while unmarshaling parameters"
                    + e.getMessage());
        }
    }
}

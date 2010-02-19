package org.nuxeo.apidoc.browse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.DocumentationItem;

public class DocumentationItemReader implements MessageBodyReader<DocumentationItem> {

    public static final MediaType DocumentationItemMediaType = new MediaType("application", "x-www-form-urlencoded");

    protected static final Log log = LogFactory.getLog(DocumentationItemReader.class);

    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return DocumentationItemMediaType.equals(mediaType);
    }

    public DocumentationItem readFrom(Class<DocumentationItem> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        InputStreamReader isr = new InputStreamReader(entityStream, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = br.read()) > -1) {
            sb.append((char) ch);
        }
        br.close();

        String data = sb.toString();

        String parts[] = data.split("&");


        SimpleDocumentationItem item = new SimpleDocumentationItem();
        for (String part : parts) {

            String[] subParts=part.split("=");

            if (subParts.length==2) {
                if ("content".equals(subParts[0])) {
                    item.content = subParts[1];
                }
                else if ("id".equals(subParts[0])) {
                    item.id = subParts[1];
                }
                else if ("renderingType".equals(subParts[0])) {
                    item.renderingType = subParts[1];
                }
                else if ("target".equals(subParts[0])) {
                    item.target = subParts[1];
                }
                else if ("targetType".equals(subParts[0])) {
                    item.targetType = subParts[1];
                }
                else if ("title".equals(subParts[0])) {
                    item.title = subParts[1];
                }
                else if ("type".equals(subParts[0])) {
                    item.type = subParts[1];
                }
                else if ("uuid".equals(subParts[0])) {
                    item.uuid = subParts[1];
                }
                else if ("approved".equals(subParts[0])) {
                    if ("on".equals(subParts[1])) {
                        item.approved = true;
                    }
                }
                else if ("versions".equals(subParts[0])) {
                    item.applicableVersion.add(subParts[1]);
                }
            }
        }

        log.debug("POST data = " + data);

        return item;
    }

}

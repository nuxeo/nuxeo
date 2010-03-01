package org.nuxeo.apidoc.browse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URLDecoder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.DocumentationItem;

@Provider
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

                String dataStr = URLDecoder.decode(subParts[1], "utf-8");

                if ("content".equals(subParts[0])) {
                    item.content = dataStr;
                }
                else if ("id".equals(subParts[0])) {
                    item.id = dataStr;
                }
                else if ("renderingType".equals(subParts[0])) {
                    item.renderingType = dataStr;
                }
                else if ("target".equals(subParts[0])) {
                    item.target = dataStr;
                }
                else if ("targetType".equals(subParts[0])) {
                    item.targetType = dataStr;
                }
                else if ("title".equals(subParts[0])) {
                    item.title = dataStr;
                }
                else if ("type".equals(subParts[0])) {
                    item.type = dataStr;
                }
                else if ("uuid".equals(subParts[0])) {
                    item.uuid = dataStr;
                }
                else if ("approved".equals(subParts[0])) {
                    if ("on".equals(dataStr)) {
                        item.approved = true;
                    }
                }
                else if ("versions".equals(subParts[0])) {
                    item.applicableVersion.add(dataStr);
                }
            }
        }

        log.debug("POST data = " + data);

        return item;
    }

}

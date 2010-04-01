package org.nuxeo.apidoc.browse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.DocumentationItem;

@Provider
public class DocumentationItemReader implements MessageBodyReader<DocumentationItem> {

    public static final MediaType DocumentationItemMediaType = new MediaType("application", "x-www-form-urlencoded");

    protected static final Log log = LogFactory.getLog(DocumentationItemReader.class);

    protected @Context HttpServletRequest request;

    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return DocumentationItemMediaType.equals(mediaType);
    }

    public DocumentationItem readFrom(Class<DocumentationItem> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        SimpleDocumentationItem item = new SimpleDocumentationItem();
        item.content = request.getParameter("content");
        item.id = request.getParameter("id");
        item.renderingType = request.getParameter("renderingType");
        item.target = request.getParameter("target");
        item.targetType = request.getParameter("targetType");
        item.title = request.getParameter("title");
        item.type = request.getParameter("type");
        item.uuid = request.getParameter("uuid");
        String v = request.getParameter("approved");
        if ("on".equals(v)) { //TODO better to use "true" or "false" and use Boolean.parseBoolean(v) to decode it
            item.approved = true;
        }
        String[] ar = request.getParameterValues("versions");
        if (ar != null) {
            for (int i=0; i<ar.length; i++) {
                item.applicableVersion.add(ar[i]);
            }
        }

        String[] attachementsTitles = request.getParameterValues("attachementsTitle");
        if (attachementsTitles!=null && attachementsTitles.length>0) {
            String[] attachementsContents = request.getParameterValues("attachementsContent");
            Map<String, String> attachements = new LinkedMap();
            int idx=0;
            for (String attachementsTitle : attachementsTitles) {
                if (attachementsContents.length>idx) {
                    attachements.put(attachementsTitle, attachementsContents[idx]);
                }
                idx+=1;
            }
            item.attachements=attachements;
        }
        return item;
    }

}

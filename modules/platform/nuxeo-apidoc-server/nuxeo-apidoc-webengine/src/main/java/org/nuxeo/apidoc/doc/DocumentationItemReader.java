/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.doc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.AbstractDocumentationItem;
import org.nuxeo.apidoc.api.DocumentationItem;

@Provider
public class DocumentationItemReader implements MessageBodyReader<DocumentationItem> {

    public static final MediaType DocumentationItemMediaType = new MediaType("application", "x-www-form-urlencoded");

    protected static final Log log = LogFactory.getLog(DocumentationItemReader.class);

    @Context
    protected HttpServletRequest request;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return DocumentationItemMediaType.equals(mediaType);
    }

    @Override
    public DocumentationItem readFrom(Class<DocumentationItem> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        SimpleDocumentationItem item = new SimpleDocumentationItem(
                AbstractDocumentationItem.typeLabelOf(request.getParameter("type")));
        item.content = request.getParameter("content");
        item.id = request.getParameter("id");
        item.renderingType = request.getParameter("renderingType");
        item.target = request.getParameter("target");
        item.targetType = request.getParameter("targetType");
        item.title = request.getParameter("title");
        item.type = request.getParameter("type");
        item.uuid = request.getParameter("uuid");
        String v = request.getParameter("approved");
        if ("on".equals(v)) { // TODO better to use "true" or "false" and use
                              // Boolean.parseBoolean(v) to decode it
            item.approved = true;
        }
        String[] versions = request.getParameterValues("versions");
        if (versions != null) {
            for (String version : versions) {
                item.applicableVersion.add(version);
            }
        }

        String[] attachmentsTitles = request.getParameterValues("attachmentsTitle");
        if (attachmentsTitles != null && attachmentsTitles.length > 0) {
            String[] attachmentsContents = request.getParameterValues("attachmentsContent");
            Map<String, String> attachments = new LinkedHashMap<>();
            int idx = 0;
            for (String attachmentsTitle : attachmentsTitles) {
                if (attachmentsContents.length > idx) {
                    attachments.put(attachmentsTitle, attachmentsContents[idx]);
                }
                idx += 1;
            }
            item.attachments = attachments;
        }
        return item;
    }

}

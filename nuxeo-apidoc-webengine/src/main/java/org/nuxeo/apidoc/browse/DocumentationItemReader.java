/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

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

    @Context
    protected HttpServletRequest request;

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
        String[] versions = request.getParameterValues("versions");
        if (versions != null) {
            for (String version : versions) {
                item.applicableVersion.add(version);
            }
        }

        String[] attachmentsTitles = request.getParameterValues("attachmentsTitle");
        if (attachmentsTitles!=null && attachmentsTitles.length>0) {
            String[] attachmentsContents = request.getParameterValues("attachmentsContent");
            Map<String, String> attachments = new LinkedMap();
            int idx=0;
            for (String attachmentsTitle : attachmentsTitles) {
                if (attachmentsContents.length>idx) {
                    attachments.put(attachmentsTitle, attachmentsContents[idx]);
                }
                idx+=1;
            }
            item.attachments=attachments;
        }
        return item;
    }

}

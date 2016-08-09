/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.util.DateTimeFormat;
import org.nuxeo.ecm.automation.core.util.JSONPropertyWriter;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * JSon writer that outputs a format ready to eat by elasticsearch.
 *
 * @since 5.9.3
 */
@Provider
@Produces({ JsonESDocumentWriter.MIME_TYPE })
public class JsonESDocumentWriter implements MessageBodyWriter<DocumentModel> {

    public static final String MIME_TYPE = "application/json+esentity";

    public static final String DOCUMENT_PROPERTIES_HEADER = "X-NXDocumentProperties";

    @Context
    protected HttpHeaders headers;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return DocumentModel.class.isAssignableFrom(type) && MIME_TYPE.equals(mediaType.toString());
    }

    @Override
    public long getSize(DocumentModel arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1L;
    }

    @Override
    public void writeTo(DocumentModel doc, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        // schema names: dublincore, file, ... or *
        List<String> props = headers.getRequestHeader(DOCUMENT_PROPERTIES_HEADER);
        String[] schemas = null;
        if (props != null && !props.isEmpty()) {
            schemas = StringUtils.split(props.get(0), ", ");
        }
        writeDocument(entityStream, doc, schemas, null);
    }

    public void writeDoc(JsonGenerator jg, DocumentModel doc, String[] schemas, Map<String, String> contextParameters,
            HttpHeaders headers) throws IOException {

        jg.writeStartObject();
        writeSystemProperties(jg, doc);
        writeSchemas(jg, doc, schemas);
        writeContextParameters(jg, doc, contextParameters);
        jg.writeEndObject();
        jg.flush();
    }

    /**
     * @since 7.2
     */
    protected void writeSystemProperties(JsonGenerator jg, DocumentModel doc) throws IOException {
        jg.writeStringField("ecm:repository", doc.getRepositoryName());
        jg.writeStringField("ecm:uuid", doc.getId());
        jg.writeStringField("ecm:name", doc.getName());
        jg.writeStringField("ecm:title", doc.getTitle());

        String pathAsString = doc.getPathAsString();
        jg.writeStringField("ecm:path", pathAsString);
        if (StringUtils.isNotBlank(pathAsString)) {
            String[] split = pathAsString.split("/");
            if (split.length > 0) {
                for (int i = 1; i < split.length; i++) {
                    jg.writeStringField("ecm:path@level" + i, split[i]);
                }
            }
            jg.writeNumberField("ecm:path@depth", split.length);
        }

        jg.writeStringField("ecm:primaryType", doc.getType());
        DocumentRef parentRef = doc.getParentRef();
        if (parentRef != null) {
            jg.writeStringField("ecm:parentId", parentRef.toString());
        }
        jg.writeStringField("ecm:currentLifeCycleState", doc.getCurrentLifeCycleState());
        jg.writeStringField("ecm:versionLabel", doc.getVersionLabel());
        jg.writeBooleanField("ecm:isCheckedIn", !doc.isCheckedOut());
        jg.writeBooleanField("ecm:isProxy", doc.isProxy());
        jg.writeBooleanField("ecm:isVersion", doc.isVersion());
        jg.writeBooleanField("ecm:isLatestVersion", doc.isLatestVersion());
        jg.writeBooleanField("ecm:isLatestMajorVersion", doc.isLatestMajorVersion());
        jg.writeArrayFieldStart("ecm:mixinType");
        for (String facet : doc.getFacets()) {
            jg.writeString(facet);
        }
        jg.writeEndArray();
        TagService tagService = Framework.getService(TagService.class);
        if (tagService != null) {
            jg.writeArrayFieldStart("ecm:tag");
            for (Tag tag : tagService.getDocumentTags(doc.getCoreSession(), doc.getId(), null, true)) {
                jg.writeString(tag.getLabel());
            }
            jg.writeEndArray();
        }
        jg.writeStringField("ecm:changeToken", doc.getChangeToken());
        Long pos = doc.getPos();
        if (pos != null) {
            jg.writeNumberField("ecm:pos", pos);
        }
        // Add a positive ACL only
        SecurityService securityService = Framework.getService(SecurityService.class);
        List<String> browsePermissions = new ArrayList<String>(
                Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        jg.writeArrayFieldStart("ecm:acl");
        outerloop: for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted() && ace.isEffective() && browsePermissions.contains(ace.getPermission())) {
                    jg.writeString(ace.getUsername());
                }
                if (ace.isDenied() && ace.isEffective()) {
                    if (!EVERYONE.equals(ace.getUsername())) {
                        jg.writeString(UNSUPPORTED_ACL);
                    }
                    break outerloop;
                }
            }
        }

        jg.writeEndArray();
        Map<String, String> bmap = doc.getBinaryFulltext();
        if (bmap != null && !bmap.isEmpty()) {
            for (Map.Entry<String, String> item : bmap.entrySet()) {
                String value = item.getValue();
                if (value != null) {
                    jg.writeStringField("ecm:" + item.getKey(), value);
                }
            }
        }
    }

    /**
     * @since 7.2
     */
    protected void writeSchemas(JsonGenerator jg, DocumentModel doc, String[] schemas) throws IOException {
        if (schemas == null || (schemas.length == 1 && "*".equals(schemas[0]))) {
            schemas = doc.getSchemas();
        }
        for (String schema : schemas) {
            writeProperties(jg, doc, schema, null);
        }
    }

    /**
     * @since 7.2
     */
    protected void writeContextParameters(JsonGenerator jg, DocumentModel doc, Map<String, String> contextParameters)
            throws IOException {
        if (contextParameters != null && !contextParameters.isEmpty()) {
            for (Map.Entry<String, String> parameter : contextParameters.entrySet()) {
                jg.writeStringField(parameter.getKey(), parameter.getValue());
            }
        }
    }

    public void writeDocument(OutputStream out, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters) throws IOException {
        writeDoc(JsonHelper.createJsonGenerator(out), doc, schemas, contextParameters, headers);
    }

    public void writeESDocument(JsonGenerator jg, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters) throws IOException {
        writeDoc(jg, doc, schemas, contextParameters, null);
    }

    protected static void writeProperties(JsonGenerator jg, DocumentModel doc, String schema, ServletRequest request)
            throws IOException {
        DocumentPart part = doc.getPart(schema);
        if (part == null) {
            return;
        }
        String prefix = part.getSchema().getNamespace().prefix;
        if (prefix == null || prefix.length() == 0) {
            prefix = schema;
        }
        prefix = prefix + ":";

        String blobUrlPrefix = null;
        if (request != null) {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            blobUrlPrefix = VirtualHostHelper.getBaseURL(request) + downloadService.getDownloadUrl(doc, null, null)
                    + "/";
        }

        for (Property p : part.getChildren()) {
            jg.writeFieldName(prefix + p.getField().getName().getLocalName());
            JSONPropertyWriter.writePropertyValue(jg, p, DateTimeFormat.W3C, blobUrlPrefix);
        }
    }

}

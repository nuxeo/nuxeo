/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.io;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.util.JSONPropertyWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * JSon writer that outputs a format ready to eat by elasticsearch.
 *
 * @since 5.9.3
 */
public class JsonESDocumentWriter {

    /**
     * @since 7.2
     */
    protected void writeSystemProperties(JsonGenerator jg, DocumentModel doc) throws IOException {
        String docId = doc.getId();
        CoreSession session = doc.getCoreSession();
        jg.writeStringField("ecm:repository", doc.getRepositoryName());
        jg.writeStringField("ecm:uuid", docId);
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
        if (doc.isVersion()) {
            jg.writeStringField("ecm:versionLabel", doc.getVersionLabel());
            jg.writeStringField("ecm:versionVersionableId", doc.getVersionSeriesId());
        }
        jg.writeBooleanField("ecm:isCheckedIn", !doc.isCheckedOut());
        jg.writeBooleanField("ecm:isProxy", doc.isProxy());
        jg.writeBooleanField("ecm:isTrashed", doc.isTrashed());
        jg.writeBooleanField("ecm:isVersion", doc.isVersion());
        jg.writeBooleanField("ecm:isLatestVersion", doc.isLatestVersion());
        jg.writeBooleanField("ecm:isLatestMajorVersion", doc.isLatestMajorVersion());
        jg.writeArrayFieldStart("ecm:mixinType");
        for (String facet : doc.getFacets()) {
            jg.writeString(facet);
        }
        jg.writeEndArray();
        TagService tagService = Framework.getService(TagService.class);
        if (tagService != null && tagService.supportsTag(session, docId)) {
            jg.writeArrayFieldStart("ecm:tag");
            for (String tag : tagService.getTags(session, docId)) {
                jg.writeString(tag);
            }
            jg.writeEndArray();
        }
        jg.writeStringField("ecm:changeToken", doc.getChangeToken());
        Long pos = doc.getPos();
        if (pos != null) {
            jg.writeNumberField("ecm:pos", pos.longValue());
        }
        // Add a positive ACL only
        SecurityService securityService = Framework.getService(SecurityService.class);
        List<String> browsePermissions = new ArrayList<>(Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
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

    public void writeESDocument(JsonGenerator jg, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters) throws IOException {
        jg.writeStartObject();
        writeSystemProperties(jg, doc);
        writeSchemas(jg, doc, schemas);
        writeContextParameters(jg, doc, contextParameters);
        jg.writeEndObject();
        jg.flush();
    }

    protected static void writeProperties(JsonGenerator jg, DocumentModel doc, String schema, ServletRequest request)
            throws IOException {
        Collection<Property> properties = doc.getPropertyObjects(schema);
        if (properties.isEmpty()) {
            return;
        }

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        String prefix = schemaManager.getSchema(schema).getNamespace().prefix;
        if (prefix == null || prefix.length() == 0) {
            prefix = schema;
        }
        JSONPropertyWriter writer = JSONPropertyWriter.create().writeNull(false).writeEmpty(false).prefix(prefix);

        if (request != null) {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            String blobUrlPrefix = VirtualHostHelper.getBaseURL(request)
                    + downloadService.getDownloadUrl(doc, null, null) + "/";
            writer.filesBaseUrl(blobUrlPrefix);
        }

        for (Property p : properties) {
            writer.writeProperty(jg, p);
        }
    }

}

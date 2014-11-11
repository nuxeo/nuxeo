package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.api.Framework;

/**
 * JSon writer that outputs a format ready to eat by elasticsearch.
 *
 *
 * @since 5.9.3
 */
@Provider
@Produces({ JsonESDocumentWriter.MIME_TYPE })
public class JsonESDocumentWriter extends JsonDocumentWriter {

    public static final String MIME_TYPE = "application/json+esentity";

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return super.isWriteable(type, genericType, annotations, mediaType)
                && MIME_TYPE.equals(mediaType.toString());
    }

    public static void writeDoc(JsonGenerator jg, DocumentModel doc,
            String[] schemas, Map<String, String> contextParameters,
            HttpHeaders headers) throws Exception {

        jg.writeStartObject();
        jg.writeStringField("ecm:repository", doc.getRepositoryName());
        jg.writeStringField("ecm:uuid", doc.getId());
        jg.writeStringField("ecm:name", doc.getName());
        jg.writeStringField("ecm:title", doc.getTitle());
        jg.writeStringField("ecm:path", doc.getPathAsString());
        jg.writeStringField("ecm:primaryType", doc.getType());
        DocumentRef parentRef = doc.getParentRef();
        if (parentRef != null) {
            jg.writeStringField("ecm:parentId", parentRef.toString());
        }
        jg.writeStringField("ecm:currentLifeCycleState",
                doc.getCurrentLifeCycleState());
        jg.writeStringField("ecm:versionLabel", doc.getVersionLabel());
        jg.writeBooleanField("ecm:isCheckedIn", !doc.isCheckedOut());
        jg.writeBooleanField("ecm:isProxy", doc.isProxy());
        jg.writeBooleanField("ecm:isVersion", doc.isVersion());
        jg.writeArrayFieldStart("ecm:mixinType");
        for (String facet : doc.getFacets()) {
            jg.writeString(facet);
        }
        jg.writeEndArray();
        TagService tagService = Framework.getService(TagService.class);
        if (tagService != null) {
            jg.writeArrayFieldStart("ecm:tag");
            for (Tag tag : tagService.getDocumentTags(doc.getCoreSession(),
                    doc.getId(), null, true)) {
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
        SecurityService securityService = Framework
                .getService(SecurityService.class);
        List<String> browsePermissions = new ArrayList<String>(
                Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
        ACP acp = doc.getACP();
        jg.writeArrayFieldStart("ecm:acl");
        outerloop: for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted()
                        && browsePermissions.contains(ace.getPermission())) {
                    jg.writeString(ace.getUsername());
                }
                if (ace.isDenied()) {
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
        if (schemas == null || (schemas.length == 1 && "*".equals(schemas[0]))) {
            schemas = doc.getSchemas();
        }
        for (String schema : schemas) {
            writeProperties(jg, doc, schema);
        }
        if (contextParameters != null && !contextParameters.isEmpty()) {
            for (Map.Entry<String, String> parameter : contextParameters
                    .entrySet()) {
                jg.writeStringField(parameter.getKey(), parameter.getValue());
            }
        }
        jg.writeEndObject();
        jg.flush();
    }

    @Override
    public void writeDocument(OutputStream out, DocumentModel doc,
            String[] schemas, Map<String, String> contextParameters)
            throws Exception {
        writeDoc(factory.createJsonGenerator(out, JsonEncoding.UTF8), doc,
                schemas, contextParameters, headers);
    }

    public static void writeESDocument(JsonGenerator jg, DocumentModel doc,
            String[] schemas, Map<String, String> contextParameters)
            throws Exception {
        writeDoc(jg, doc, schemas, contextParameters, null);
    }

}

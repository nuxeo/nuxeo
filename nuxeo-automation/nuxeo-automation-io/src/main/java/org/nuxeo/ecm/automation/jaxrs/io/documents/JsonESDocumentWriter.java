package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
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
@Produces({ "application/json+esentity" })
public class JsonESDocumentWriter extends JsonDocumentWriter {

    public static void writeDoc(JsonGenerator jg, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters, HttpHeaders headers) throws Exception {

        jg.writeStartObject();
        jg.writeStringField("repository", doc.getRepositoryName());
        jg.writeStringField("ecm:uuid", doc.getId());
        jg.writeStringField("ecm:name", doc.getName());
        jg.writeStringField("ecm:title", doc.getTitle());
        jg.writeStringField("ecm:path", doc.getPathAsString());
        jg.writeStringField("ecm:primarytype", doc.getType());
        jg.writeStringField("ecm:parentId", doc.getParentRef().toString());
        jg.writeStringField("ecm:currentLifeCycleState", doc.getCurrentLifeCycleState());
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
            for (Tag tag : tagService.getDocumentTags(doc.getCoreSession(), doc.getId(),
                    null)) {
                jg.writeString(tag.getLabel());
            }
            jg.writeEndArray();
        }
        jg.writeStringField("changeToken", doc.getChangeToken());
        // TODO Add acl
        jg.writeArrayFieldStart("acl");
        jg.writeString("fake");
        jg.writeString("members");
        jg.writeEndArray();
        // TODO Add fulltext
        jg.writeStringField("fulltext", doc.getTitle() + " " + doc.getName());
        if (schemas == null || (schemas.length == 1 && "*".equals(schemas[0]))) {
            schemas = doc.getSchemas();
        }
        for (String schema : schemas) {
            writeProperties(jg, doc, schema);
        }
        if (contextParameters != null && !contextParameters.isEmpty()) {
            for (Map.Entry<String, String> parameter : contextParameters.entrySet()) {
                jg.writeStringField(parameter.getKey(), parameter.getValue());
            }
        }
        jg.writeEndObject();
        jg.flush();
    }

    @Override
    public void writeDocument(OutputStream out, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters) throws Exception {
        writeDoc(factory.createJsonGenerator(out, JsonEncoding.UTF8), doc, schemas,
                contextParameters, headers);
    }

    public static void writeESDocument(JsonGenerator jg, DocumentModel doc,
            String[] schemas, Map<String, String> contextParameters) throws Exception {
        writeDoc(jg, doc, schemas, contextParameters, null);
    }

}

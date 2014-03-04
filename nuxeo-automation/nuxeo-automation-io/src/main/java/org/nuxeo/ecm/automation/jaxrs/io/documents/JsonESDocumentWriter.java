package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;

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
        jg.writeStringField("uid", doc.getId());
        jg.writeStringField("path", doc.getPathAsString());
        jg.writeStringField("type", doc.getType());
        jg.writeStringField("state", doc.getCurrentLifeCycleState());
        jg.writeStringField("versionLabel", doc.getVersionLabel());
        jg.writeBooleanField("isCheckedOut", doc.isCheckedOut());
        jg.writeStringField("title", doc.getTitle());
        jg.writeArrayFieldStart("facets");
        for (String facet : doc.getFacets()) {
            jg.writeString(facet);
        }
        jg.writeEndArray();
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

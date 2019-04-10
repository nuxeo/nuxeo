package org.nuxeo.template.processors.xdocreport;

import java.io.StringWriter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class FieldDefinitionGenerator {

    public static String generate(String type) throws Exception {
        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        DocumentType docType = sm.getDocumentType(type);

        return generate(docType.getSchemaNames());
    }

    public static String generate(DocumentModel doc) throws Exception {
        return generate(doc.getSchemas());
    }

    protected static String generate(String[] schemaNames) throws Exception {

        FieldsMetadata fieldsMetadata = new FieldsMetadata(
                TemplateEngineKind.Freemarker.name());

        for (String schemaName : schemaNames) {
            SchemaManager sm = Framework.getLocalService(SchemaManager.class);

            Schema schema = sm.getSchema(schemaName);
            for (Field field : schema.getFields()) {
                // String pname = field.getName().getPrefixedName();
                String name = field.getName().getLocalName();
                // String fieldName = "doc['" + name + "']";
                // fieldsMetadata.addField(fieldName, false, null, null, null);
                String fieldName = "doc." + schemaName + "." + name;
                if (field.getType().isListType()) {
                    fieldsMetadata.addField(fieldName, true, null, null, null);
                } else {
                    fieldsMetadata.addField(fieldName, false, null, null, null);
                    if (field.getType().isComplexType()) {

                        ComplexType ct = (ComplexType) field.getType();
                        if ("content".equals(ct.getName())) {
                            fieldsMetadata.addField(fieldName + ".filename",
                                    false, null, null, null);
                        } else {
                            for (Field subField : ct.getFields()) {
                                fieldsMetadata.addField(fieldName + "."
                                        + subField.getName().getLocalName(),
                                        false, null, null, null);
                            }
                        }
                    }
                }
            }
        }

        fieldsMetadata.addField("doc.versionLabel", false, null, null, null);
        fieldsMetadata.addField("doc.id", false, null, null, null);
        fieldsMetadata.addField("doc.name", false, null, null, null);
        fieldsMetadata.addField("doc.title", false, null, null, null);
        fieldsMetadata.addField("doc.pathAsString", false, null, null, null);
        fieldsMetadata.addField("doc.type", false, null, null, null);
        fieldsMetadata.addField("doc.schemas", true, null, null, null);
        fieldsMetadata.addField("doc.facets", true, null, null, null);
        fieldsMetadata.addField("doc.locked", false, null, null, null);
        fieldsMetadata.addField("doc.lockInfo", false, null, null, null);
        fieldsMetadata.addField("doc.lockInfo.owner", false, null, null, null);
        fieldsMetadata.addField("doc.lockInfo.created", false, null, null, null);
        fieldsMetadata.addField("doc.checkedOut", false, null, null, null);
        fieldsMetadata.addField("doc.", false, null, null, null);

        // fieldsMetadata.load("principal", NuxeoPrincipal.class); //
        // stackoverflow

        fieldsMetadata.addField("principal.firstName", false, null, null, null);
        fieldsMetadata.addField("principal.lastName", false, null, null, null);
        fieldsMetadata.addField("principal.company", false, null, null, null);
        fieldsMetadata.addField("principal.email", false, null, null, null);
        fieldsMetadata.addField("principal.name", false, null, null, null);

        fieldsMetadata.addField("auditEntries", false, null, null, null);
        fieldsMetadata.addField("auditEntries.id", true, null, null, null);
        fieldsMetadata.addField("auditEntries.principalName", true, null, null,
                null);
        fieldsMetadata.addField("auditEntries.eventId", true, null, null, null);
        fieldsMetadata.addField("auditEntries.eventDate", true, null, null,
                null);
        fieldsMetadata.addField("auditEntries.docUUID", true, null, null, null);
        fieldsMetadata.addField("auditEntries.docPath", true, null, null, null);
        fieldsMetadata.addField("auditEntries.docType", true, null, null, null);
        fieldsMetadata.addField("auditEntries.category", true, null, null, null);
        fieldsMetadata.addField("auditEntries.comment", true, null, null, null);
        fieldsMetadata.addField("auditEntries.docLifeCycle", true, null, null,
                null);
        fieldsMetadata.addField("auditEntries.repositoryId", true, null, null,
                null);

        StringWriter writer = new StringWriter();

        fieldsMetadata.saveXML(writer, true);

        return writer.getBuffer().toString();
    }
}

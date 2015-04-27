package org.nuxeo.ecm.core.io.marshallers.csv.document;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.csv.CsvContributor;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;

public class PropertiesCsvContributor extends CsvContributor<DocumentModel> {

    private final List<Schema> schemas;

    private final List<String> headers;

    public PropertiesCsvContributor(RenderingContext ctx, SchemaManager schemaManager) {
        super(DocumentModel.class);
        this.schemas = new ArrayList<Schema>();
        headers = new ArrayList<String>();
        for (String schemaName : ctx.getProperties()) {
            if (MarshallingConstants.WILDCARD_VALUE.equals(schemaName)) {
                continue;
            }
            Schema schema = schemaManager.getSchema(schemaName);
            if (schema != null) {
                schemas.add(schema);
                for (Field field : schema.getFields()) {
                    headers.addAll(getHeadersFromField(schema.getName(), field));
                }
            }
        }
    }

    protected List<String> getHeadersFromField(String prefix, Field field) {
        List<String> result = new ArrayList<String>();
        Type type = field.getType();
        String fieldName = field.getName().getLocalName();
        if (type.isComplexType()) {
            ComplexType cType = (ComplexType) type;
            for (Field child : cType.getFields()) {
                result.addAll(getHeadersFromField(prefix + "." + fieldName, child));
            }
        } else if (type.isListType()) {
            // skip - ignore lists
        } else {
            result.add(prefix + "." + fieldName);
        }
        return result;
    }

    @Override
    public List<String> getHeaders() {
        return headers;
    }

    @Override
    public List<String> getOrderedValues(DocumentModel doc) {
        List<String> result = new ArrayList<String>();
        for (Schema schema : schemas) {
            String schemaName = schema.getName();
            if (!doc.hasSchema(schemaName)) {
                for (String header : headers) {
                    if (header.startsWith(schemaName + ".")) {
                        result.add(null);
                    }
                }
            } else {
                for (Field field : schema.getFields()) {
                    Property property = doc.getProperty(field.getName().getPrefixedName());
                    result.addAll(getValuesFromProperty(property, field));
                }
            }
        }
        return result;
    }

    protected List<String> getValuesFromProperty(Property property, Field field) {
        List<String> result = new ArrayList<String>();
        Type type = field.getType();
        if (type.isComplexType()) {
            ComplexType cType = (ComplexType) type;
            for (Field child : cType.getFields()) {
                Property childProperty = property.get(child.getName().getLocalName());
                result.addAll(getValuesFromProperty(childProperty, child));
            }
        } else if (type.isListType()) {
            // skip - ignore lists
        } else {
            result.add(type.encode(property.getValue()));
        }
        return result;
    }

}
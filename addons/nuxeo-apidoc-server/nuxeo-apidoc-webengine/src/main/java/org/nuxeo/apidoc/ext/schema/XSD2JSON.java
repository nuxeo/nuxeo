package org.nuxeo.apidoc.ext.schema;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.schema.XSDLoader;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.util.SimpleRuntime;

public class XSD2JSON {

    public final static List<String> supportedScalarTypes = Arrays.asList(
            "string", "date", "boolean", "integer", "double");

    public final static List<String> restrictedFieldNames = Arrays.asList("id");

    protected static String getTargetName(String prefix, String name) {
        String targetName = name.substring(prefix.length() + 2);
        targetName = targetName.replaceAll("/", "_");
        return targetName;
    }

    protected static void collectXPathes(String prefix, Field field,
            Map<String, Field> collector) {
        if (field.getType().isSimpleType()) {
            collector.put(prefix + "/" + field.getName().getPrefixedName(),
                    field);
        } else if (field.getType().isListType()) {

            ListType lt = (ListType) field.getType();

            if (lt.getFieldType().isSimpleType()) {
                collector.put(prefix + "/" + field.getName().getPrefixedName()
                        + "[*]", field);
            } else {
                collectXPathes(prefix + "/" + field.getName().getPrefixedName()
                        + "[*]", lt.getField(), collector);
            }

        } else {
            ComplexType ct = (ComplexType) field.getType();
            for (Field subField : ct.getFields()) {
                String path = prefix + "/" + field.getName().getPrefixedName();
                if (field.getName().getLocalName().equals("item")
                        && prefix.endsWith("[*]")) {
                    path = prefix;
                }
                collectXPathes(path, subField, collector);

            }
        }

    }

    public static String asJSON(String name, String prefix, String xsd)
            throws Exception {
        Schema schema = loadSchema(name, prefix, xsd);
        if (schema != null) {
            return asJSON(schema);
        }
        return null;
    }

    protected static Schema loadSchema(String name, String prefix, String xsd)
            throws Exception {
        if (!Framework.isInitialized()) {
            RuntimeService runtime = new SimpleRuntime();
            System.setProperty("nuxeo.home",
                    System.getProperty("java.io.tmpdir"));
            Framework.initialize(runtime);
        }

        SchemaManagerImpl schemaManager = new SchemaManagerImpl();
        XSDLoader loader = new XSDLoader(schemaManager);
        Schema schema = loader.loadSchema(name, prefix,
                new ByteArrayInputStream(xsd.getBytes()));

        return schema;
    }

    protected static String asJSON(Schema schema) throws JSONException {

        JSONObject schemaObject = new JSONObject();
        schemaObject.put("name", schema.getName());
        schemaObject.put("prefix", schema.getNamespace().prefix);

        JSONObject fields = new JSONObject();

        for (Field field : schema.getFields()) {
            addField(fields, field);
        }

        schemaObject.put("fields", fields);

        return schemaObject.toString(2);
    }

    protected static String getFiltredScalarType(String type) {
        if (supportedScalarTypes.contains(type)) {
            return type;
        }
        if ("long".equalsIgnoreCase(type)) {
            return "integer";
        }
        return "string";
    }

    protected static boolean isValidFieldName(String name) {
        if (restrictedFieldNames.contains(name)) {
            System.out.println("### Skiping field with name " + name);
            return false;
        }
        return true;
    }

    protected static void addField(JSONObject object, Field field)
            throws JSONException {

        if (!field.getType().isComplexType()) {
            if (field.getType().isListType()) {
                ListType lt = (ListType) field.getType();
                if (lt.getFieldType().isComplexType()) {
                    if (lt.getFieldType().getName().equals("content")) {
                        if (isValidFieldName(field.getName().getLocalName())) {
                            object.put(field.getName().getLocalName(), "blob[]");
                        }
                    } else {
                        JSONObject cplx = new JSONObject();
                        cplx.put("type", "complex[]");
                        JSONObject fields = buildComplexFields(lt.getField());
                        cplx.put("fields", fields);
                        if (isValidFieldName(field.getName().getLocalName())) {
                            object.put(field.getName().getLocalName(), cplx);
                        }
                    }
                } else {
                    if (isValidFieldName(field.getName().getLocalName())) {
                        object.put(
                                field.getName().getLocalName(),
                                getFiltredScalarType(lt.getFieldType().getName())
                                        + "[]");
                    }
                }
            } else {
                if (isValidFieldName(field.getName().getLocalName())) {
                    object.put(field.getName().getLocalName(),
                            getFiltredScalarType(field.getType().getName()));
                }
            }
        } else {
            if (field.getType().getName().equals("content")) {
                if (isValidFieldName(field.getName().getLocalName())) {
                    object.put(field.getName().getLocalName(), "blob");
                }
            } else {
                JSONObject cplx = new JSONObject();
                cplx.put("type", "complex");
                JSONObject fields = buildComplexFields(field);
                cplx.put("fields", fields);
                if (isValidFieldName(field.getName().getLocalName())) {
                    object.put(field.getName().getLocalName(), cplx);
                }
            }
        }
    }

    protected static JSONObject buildComplexFields(Field field)
            throws JSONException {
        JSONObject fields = new JSONObject();
        ComplexType cplXType = (ComplexType) field.getType();
        for (Field subField : cplXType.getFields()) {
            addField(fields, subField);
        }
        return fields;
    }
}

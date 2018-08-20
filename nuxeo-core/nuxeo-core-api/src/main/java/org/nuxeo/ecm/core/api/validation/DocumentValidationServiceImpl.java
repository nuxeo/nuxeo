/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation.PathNode;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentValidationServiceImpl extends DefaultComponent implements DocumentValidationService {

    private SchemaManager schemaManager;

    protected SchemaManager getSchemaManager() {
        if (schemaManager == null) {
            schemaManager = Framework.getService(SchemaManager.class);
        }
        return schemaManager;
    }

    private Map<String, Boolean> validationActivations = new HashMap<String, Boolean>();

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("activations")) {
            DocumentValidationDescriptor dvd = (DocumentValidationDescriptor) contribution;
            validationActivations.put(dvd.getContext(), dvd.isActivated());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("activations")) {
            DocumentValidationDescriptor dvd = (DocumentValidationDescriptor) contribution;
            validationActivations.remove(dvd.getContext());
        }
    }

    @Override
    public boolean isActivated(String context, Map<String, Serializable> contextMap) {
        if (contextMap != null) {
            Forcing flag = (Forcing) contextMap.get(DocumentValidationService.CTX_MAP_KEY);
            if (flag != null) {
                switch (flag) {
                case TURN_ON:
                    return true;
                case TURN_OFF:
                    return false;
                case USUAL:
                    break;
                }
            }
        }
        Boolean activated = validationActivations.get(context);
        if (activated == null) {
            return false;
        } else {
            return activated;
        }
    }

    @Override
    public DocumentValidationReport validate(DocumentModel document) {
        return validate(document, false);
    }

    @Override
    public DocumentValidationReport validate(DocumentModel document, boolean dirtyOnly) {
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        DocumentType docType = document.getDocumentType();
        if (dirtyOnly) {
            for (DataModel dataModel : document.getDataModels().values()) {
                Schema schemaDef = getSchemaManager().getSchema(dataModel.getSchema());
                for (String fieldName : dataModel.getDirtyFields()) {
                    Field field = schemaDef.getField(fieldName);
                    Property property = document.getProperty(field.getName().getPrefixedName());
                    List<PathNode> path = Arrays.asList(new PathNode(property.getField()));
                    violations.addAll(validateAnyTypeProperty(property.getSchema(), path, property, true, true));
                }
            }
        } else {
            for (Schema schema : docType.getSchemas()) {
                for (Field field : schema.getFields()) {
                    Property property = document.getProperty(field.getName().getPrefixedName());
                    List<PathNode> path = Arrays.asList(new PathNode(property.getField()));
                    violations.addAll(validateAnyTypeProperty(property.getSchema(), path, property, false, true));
                }
            }
        }
        return new DocumentValidationReport(violations);
    }

    @Override
    public DocumentValidationReport validate(Field field, Object value) {
        return validate(field, value, true);
    }

    @Override
    public DocumentValidationReport validate(Field field, Object value, boolean validateSubProperties) {
        Schema schema = field.getDeclaringType().getSchema();
        return new DocumentValidationReport(validate(schema, field, value, validateSubProperties));
    }

    @Override
    public DocumentValidationReport validate(Property property) {
        return validate(property, true);
    }

    @Override
    public DocumentValidationReport validate(Property property, boolean validateSubProperties) {
        List<PathNode> path = new ArrayList<>();
        Property inspect = property;
        while (inspect != null && !(inspect instanceof DocumentPart)) {
            path.add(0, new PathNode(inspect.getField()));
            inspect = inspect.getParent();
        }
        return new DocumentValidationReport(validateAnyTypeProperty(property.getSchema(), path, property, false,
                validateSubProperties));
    }

    @Override
    public DocumentValidationReport validate(String xpath, Object value) {
        return validate(xpath, value, true);
    }

    @Override
    public DocumentValidationReport validate(String xpath, Object value, boolean validateSubProperties)
            throws IllegalArgumentException {
        SchemaManager tm = Framework.getService(SchemaManager.class);
        List<String> splittedXpath = Arrays.asList(xpath.split("\\/"));
        List<PathNode> path = new ArrayList<>();
        Field field = null;
        String fieldXpath = null;
        // rebuild the field path
        for (String xpathToken : splittedXpath) {
            // manage the list item case
            if (field != null && field.getType().isListType()) {
                // get the list field type
                Field itemField = ((ListType) field.getType()).getField();
                if (xpathToken.matches("\\d+")) {
                    // if the current token is an index, append the token and append an indexed PathNode to the path
                    fieldXpath += "/" + xpathToken;
                    field = itemField;
                    int index = Integer.parseInt(xpathToken);
                    path.add(new PathNode(field, index));
                } else if (xpathToken.equals(itemField.getName().getLocalName())) {
                    // if the token is equals to the item's field name
                    // ignore it on the xpath but append the item's field to the path node
                    field = itemField;
                    path.add(new PathNode(field));
                } else {
                    // otherwise, the token in an item's element
                    // append the token and append the item's field and the item's element's field to the path node
                    fieldXpath += "/" + xpathToken;
                    field = itemField;
                    path.add(new PathNode(field));
                    field = tm.getField(fieldXpath);
                    if (field == null) {
                        throw new IllegalArgumentException("Invalid xpath " + fieldXpath);
                    }
                    path.add(new PathNode(field));
                }
            } else {
                // in any case, if it's the first item, the token is the path
                if (fieldXpath == null) {
                    fieldXpath = xpathToken;
                } else {
                    // otherwise, append the token to the existing path
                    fieldXpath += "/" + xpathToken;
                }
                // get the field
                field = tm.getField(fieldXpath);
                // check it exists
                if (field == null) {
                    throw new IllegalArgumentException("Invalid xpath " + fieldXpath);
                }
                // append the pathnode
                path.add(new PathNode(field));
            }
        }
        Schema schema = field.getDeclaringType().getSchema();
        return new DocumentValidationReport(validateAnyTypeField(schema, path, field, value, validateSubProperties));
    }

    // ///////////////////
    // UTILITY OPERATIONS

    protected List<ConstraintViolation> validate(Schema schema, Field field, Object value, boolean validateSubProperties) {
        List<PathNode> path = Arrays.asList(new PathNode(field));
        return validateAnyTypeField(schema, path, field, value, validateSubProperties);
    }

    // ////////////////////////////
    // Exploration based on Fields

    /**
     * @since 7.1
     */
    @SuppressWarnings("rawtypes")
    private List<ConstraintViolation> validateAnyTypeField(Schema schema, List<PathNode> path, Field field,
            Object value, boolean validateSubProperties) {
        if (field.getType().isSimpleType()) {
            return validateSimpleTypeField(schema, path, field, value);
        } else if (field.getType().isComplexType()) {
            List<ConstraintViolation> res = new ArrayList<>();
            if (!field.isNillable() && (value == null || (value instanceof Map && ((Map) value).isEmpty()))) {
                addNotNullViolation(res, schema, path);
            }
            if (validateSubProperties) {
                List<ConstraintViolation> subs = validateComplexTypeField(schema, path, field, value);
                if (subs != null) {
                    res.addAll(subs);
                }
            }
            return res;
        } else if (field.getType().isListType()) {
            // maybe validate the list type here
            if (validateSubProperties) {
                return validateListTypeField(schema, path, field, value);
            }
        }
        // unrecognized type : ignored
        return Collections.emptyList();
    }

    /**
     * This method should be the only one to create {@link ConstraintViolation}.
     *
     * @since 7.1
     */
    private List<ConstraintViolation> validateSimpleTypeField(Schema schema, List<PathNode> path, Field field,
            Object value) {
        Type type = field.getType();
        assert type.isSimpleType() || type.isListType(); // list type to manage ArrayProperty
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        Set<Constraint> constraints = null;
        if (type.isListType()) { // ArrayProperty
            constraints = ((ListType) type).getFieldType().getConstraints();
        } else {
            constraints = field.getConstraints();
        }
        for (Constraint constraint : constraints) {
            if (!constraint.validate(value)) {
                ConstraintViolation violation = new ConstraintViolation(schema, path, constraint, value);
                violations.add(violation);
            }
        }
        return violations;
    }

    /**
     * Validates sub fields for given complex field.
     *
     * @since 7.1
     */
    @SuppressWarnings("unchecked")
    private List<ConstraintViolation> validateComplexTypeField(Schema schema, List<PathNode> path, Field field,
            Object value) {
        assert field.getType().isComplexType();
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        ComplexType complexType = (ComplexType) field.getType();
        // this code does not support other type than Map as value
        if (value != null && !(value instanceof Map)) {
            return violations;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        for (Field child : complexType.getFields()) {
            Object item = map.get(child.getName().getLocalName());
            List<PathNode> subPath = new ArrayList<PathNode>(path);
            subPath.add(new PathNode(child));
            violations.addAll(validateAnyTypeField(schema, subPath, child, item, true));
        }
        return violations;
    }

    /**
     * Validates sub fields for given list field.
     *
     * @since 7.1
     */
    private List<ConstraintViolation> validateListTypeField(Schema schema, List<PathNode> path, Field field,
            Object value) {
        assert field.getType().isListType();
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        Collection<?> castedValue = null;
        if (!field.isNillable() && value == null) {
            addNotNullViolation(violations, schema, path);
        }

        if (value instanceof List) {
            castedValue = (Collection<?>) value;
        } else if (value instanceof Object[]) {
            castedValue = Arrays.asList((Object[]) value);
        }
        if (castedValue != null) {
            if (!field.isNillable() && castedValue.isEmpty()) {
                addNotNullViolation(violations, schema, path);
            }
            ListType listType = (ListType) field.getType();
            Field listField = listType.getField();
            int index = 0;
            for (Object item : castedValue) {
                List<PathNode> subPath = new ArrayList<PathNode>(path);
                subPath.add(new PathNode(listField, index));
                violations.addAll(validateAnyTypeField(schema, subPath, listField, item, true));
                index++;
            }
            return violations;
        }
        return violations;
    }

    // //////////////////////////////
    // Exploration based on Property

    /**
     * @since 7.1
     */
    private List<ConstraintViolation> validateAnyTypeProperty(Schema schema, List<PathNode> path, Property prop,
            boolean dirtyOnly, boolean validateSubProperties) {
        Field field = prop.getField();
        if (!dirtyOnly || prop.isDirty()) {
            if (field.getType().isSimpleType()) {
                return validateSimpleTypeProperty(schema, path, prop, dirtyOnly);
            } else if (field.getType().isComplexType()) {
                // ignore for now the case when the complex property is null with a null contraints because it's
                // currently impossible
                if (validateSubProperties) {
                    return validateComplexTypeProperty(schema, path, prop, dirtyOnly);
                }
            } else if (field.getType().isListType()) {
                if (validateSubProperties) {
                    return validateListTypeProperty(schema, path, prop, dirtyOnly);
                }
            }
        }
        // unrecognized type : ignored
        return Collections.emptyList();
    }

    /**
     * @since 7.1
     */
    private List<ConstraintViolation> validateSimpleTypeProperty(Schema schema, List<PathNode> path, Property prop,
            boolean dirtyOnly) {
        Field field = prop.getField();
        assert field.getType().isSimpleType() || prop.isScalar();
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        Serializable value = prop.getValue();
        Object defaultValue = field.getDefaultValue();
        // check nullity constraint only if field doesn't have a default value (phantom case)
        if (prop.isPhantom() && defaultValue == null || value == null) {
            if (!field.isNillable()) {
                addNotNullViolation(violations, schema, path);
            }
        } else {
            violations.addAll(validateSimpleTypeField(schema, path, field, value));
        }
        return violations;
    }

    /**
     * @since 7.1
     */
    private List<ConstraintViolation> validateComplexTypeProperty(Schema schema, List<PathNode> path, Property prop,
            boolean dirtyOnly) {
        Field field = prop.getField();
        assert field.getType().isComplexType();
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        boolean allChildrenPhantom = true;
        for (Property child : prop.getChildren()) {
            if (!child.isPhantom()) {
                allChildrenPhantom = false;
                break;
            }
        }
        Object value = prop.getValue();
        if (prop.isPhantom() || value == null || allChildrenPhantom) {
            if (!field.isNillable()) {
                addNotNullViolation(violations, schema, path);
            }
        } else {
            // this code does not support other type than Map as value
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> castedValue = (Map<String, Object>) value;
                if (castedValue.isEmpty() || castedValue.values().stream().allMatch(Objects::isNull)) {
                    if (!field.isNillable()) {
                        addNotNullViolation(violations, schema, path);
                    }
                } else {
                    for (Property child : prop.getChildren()) {
                        List<PathNode> subPath = new ArrayList<PathNode>(path);
                        subPath.add(new PathNode(child.getField()));
                        violations.addAll(validateAnyTypeProperty(schema, subPath, child, dirtyOnly, true));
                    }
                }
            }
        }
        return violations;
    }

    /**
     * @since 7.1
     */
    private List<ConstraintViolation> validateListTypeProperty(Schema schema, List<PathNode> path, Property prop,
            boolean dirtyOnly) {
        Field field = prop.getField();
        assert field.getType().isListType();
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        Serializable value = prop.getValue();
        if (prop.isPhantom() || value == null) {
            if (!field.isNillable()) {
                addNotNullViolation(violations, schema, path);
            }
        } else {
            Collection<?> castedValue = null;
            if (value instanceof Collection) {
                castedValue = (Collection<?>) value;
            } else if (value instanceof Object[]) {
                castedValue = Arrays.asList((Object[]) value);
            }
            if (castedValue != null) {
                int index = 0;
                if (prop instanceof ArrayProperty) {
                    if (!field.isNillable() && castedValue.isEmpty()) {
                        addNotNullViolation(violations, schema, path);
                    }
                    ArrayProperty arrayProp = (ArrayProperty) prop;
                    // that's an ArrayProperty : there will not be child properties
                    for (Object itemValue : castedValue) {
                        if (!dirtyOnly || arrayProp.isDirty(index)) {
                            List<PathNode> subPath = new ArrayList<PathNode>(path);
                            subPath.add(new PathNode(field, index));
                            violations.addAll(validateSimpleTypeField(schema, subPath, field, itemValue));
                        }
                        index++;
                    }
                } else {
                    Collection<Property> children = prop.getChildren();
                    if (!field.isNillable() && children.isEmpty()) {
                        addNotNullViolation(violations, schema, path);
                    }
                    for (Property child : children) {
                        List<PathNode> subPath = new ArrayList<PathNode>(path);
                        subPath.add(new PathNode(child.getField(), index));
                        violations.addAll(validateAnyTypeProperty(schema, subPath, child, dirtyOnly, true));
                        index++;
                    }
                }
            }
        }
        return violations;
    }

    // //////
    // Utils

    private void addNotNullViolation(List<ConstraintViolation> violations, Schema schema, List<PathNode> fieldPath) {
        NotNullConstraint constraint = NotNullConstraint.get();
        ConstraintViolation violation = new ConstraintViolation(schema, fieldPath, constraint, null);
        violations.add(violation);
    }

}

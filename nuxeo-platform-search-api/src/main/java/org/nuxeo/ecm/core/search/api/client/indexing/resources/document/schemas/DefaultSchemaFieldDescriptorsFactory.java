/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: DefaultSchemaFieldDescriptorsFactory.java 25901 2007-10-11 14:59:18Z gracinet $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document.schemas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.search.api.client.common.TypeManagerServiceDelegate;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableFieldDescriptor;

/**
 * Default schema field descriptors factory.
 * <p>
 * Generates default schema field descriptors. This is invoked when <strong>no</strong>
 * explicit indexing schema and / or field configurations are contributed.
 *
 * @see org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class DefaultSchemaFieldDescriptorsFactory {

    private SchemaManager typeManager;

    /**
     * Gets the type manager from the platform service platform service.
     *
     * @return a type manager instance.
     */
    private SchemaManager getTypeManager() {
        if (typeManager == null) {
            typeManager = TypeManagerServiceDelegate.getRemoteTypeManagerService();
        }
        return typeManager;
    }

    /**
     * Returns a schema given its prefix.
     *
     * @param prefix the schema prefix
     * @return a Nuxeo core schema instance
     */
    public Schema getSchemaByPrefix(String prefix) {
        return getTypeManager().getSchemaFromPrefix(prefix);
    }

    /**
     * Returns a schema given its name.
     *
     * @param name the schema name
     * @return a Nuxeo core schema instance.
     */
    public Schema getSchemaByName(String name) {
        return getTypeManager().getSchema(name);
    }

    /**
     *
     * @param schema
     * @param excludeFields
     * @return
     */
    private List<IndexableFieldDescriptor> getFieldDescriptorsForSchema(
            Schema schema, Set<String> excludeFields) {
        List<IndexableFieldDescriptor> res = new ArrayList<IndexableFieldDescriptor>();
        for (Field field : schema.getFields()) {

            String fieldName = field.getName().getLocalName();

            if (excludeFields != null && excludeFields.contains(fieldName)) {
                continue;
            }

            Type fieldType = field.getType();

            if (fieldType.isSimpleType() || fieldType.isListType()) {
                res.addAll(handleField(field));
            } else if (fieldType.isCompositeType()) {
                // :TODO:
            } else if (fieldType.isComplexType()) {
                res.addAll(handleComplexField(field));
            }
        }
        return res;
    }

    /**
     * Returns the list of indexable field descriptor for a given schema name.
     *
     * @param name the schema name
     * @param excludeFields explicitly exclude those field names.
     * @return a list of indexable field descriptor instances.
     */
    public List<IndexableFieldDescriptor> getFieldDescriptorsBySchemaName(
            String name, Set<String> excludeFields) {
        List<IndexableFieldDescriptor> descs = new ArrayList<IndexableFieldDescriptor>();
        Schema schema = getSchemaByName(name);
        if (schema != null) {
            descs = getFieldDescriptorsForSchema(schema, excludeFields);
        }
        return descs;
    }

    /**
     * Returns the list of indexable field descriptor for a schema given its
     * prefix.
     *
     * @param prefix the schema prefix
     * @param excludeFields explicitly exclude those field names.
     * @return a list of indexable field descriptor instances.
     */
    public List<IndexableFieldDescriptor> getFieldDescriptorsBySchemaPrefix(
            String prefix, Set<String> excludeFields) {
        List<IndexableFieldDescriptor> descs = new ArrayList<IndexableFieldDescriptor>();
        Schema schema = getSchemaByPrefix(prefix);
        if (schema != null) {
            descs = getFieldDescriptorsForSchema(schema, excludeFields);
        }
        return descs;
    }

    /**
     * Returns an indexable field descriptor given a field
     *
     * @param field a Nuxeo Core field instance.
     * @return an indexable field descriptor.
     */
    private Collection<IndexableFieldDescriptor> handleField(Field field) {

        String fieldName = field.getName().getLocalName();
        return handleFieldType(field.getType(), fieldName, false);
    }

    /**
     * Returns an indexable field descriptor given a field type
     *
     * @param type the type of the field
     * @param indexingName the name of the produced field descriptor.
     * @param forceMultiple if true, forces the multiplicity of the produced
     *            descriptors
     * @return an indexable field descriptor.
     */
    private List<IndexableFieldDescriptor> handleFieldType(Type type,
            String indexingName, boolean forceMultiple) {
        String indexingAnalyzer = "default";
        String indexingType = "keyword";
        boolean multiple = forceMultiple; // a starting point

        Map<String, String> termVector = new HashMap<String, String>();

        if (type.isListType()) {
            multiple = true;
            type = ((ListType) type).getFieldType();
            if (type.isComplexType()) {
                return handleComplexType((ComplexType) type,
                        indexingName + ':', multiple);
            }
        }

        boolean stored = true;
        boolean binary = false;
        if (type.getName().equals("binary")) {
            binary = true;
            indexingType = "text";
            stored = false;
        } else if (type.getName().equals("date")) {
            indexingType = "date";
        } else if (type.getName().equals("boolean")) {
            indexingType = "boolean";
        } else if (type.getName().equals("int")) {
                indexingType = "int";
        }

        List<IndexableFieldDescriptor> res = new ArrayList<IndexableFieldDescriptor>(
                1);
        boolean indexed = true;
        boolean sortable = true; // XXX use the conf @ doctype level
        res.add(new IndexableFieldDescriptor(indexingName, indexingAnalyzer,
                indexingType, stored, indexed, binary, multiple, sortable,
                null, termVector, null));
        return res;
    }

    /**
     * Return a bunch of field descriptors from a given complex type, prefix and
     * multiplicity are imposed from the outside
     *
     * @return a collection of indexable field descriptors
     */
    private List<IndexableFieldDescriptor> handleComplexType(ComplexType type,
            String prefix, boolean multiple) {

        List<IndexableFieldDescriptor> res = new LinkedList<IndexableFieldDescriptor>();
        for (Field subField : type.getFields()) {
            res.addAll(handleFieldType(subField.getType(), prefix
                    + subField.getName().getLocalName(), multiple));
        }
        return res;
    }

    /**
     * Return a bunch of field descriptors from a given field with complex type,
     * which is by definition not multiple in itself.
     *
     * @param field the given field
     * @return a collection of indexable field descriptors
     */

    private List<IndexableFieldDescriptor> handleComplexField(Field field) {
        String prefix = field.getName().getLocalName() + ':';
        return handleComplexType((ComplexType) field.getType(), prefix, false);
    }

}

/*
 * Copyright 2009-2010 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractTypeDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * Nuxeo Type Utilities.
 * <p>
 * Maps Nuxeo types to CMIS types using the following rules:
 * <ul>
 * <li>Only types containing dublincore are exposed,</li>
 * <li>cmis:document and cmis:folder expose dublincore, and are not creatable,</li>
 * <li>The Document type is not exposed,</li>
 * <li>Types inheriting from Document are exposed as inheriting cmis:document,</li>
 * <li>The Folder type is mapped to a concrete subtype of cmis:folder,</li>
 * <li>Other folderish types directly under Folder are mapped to subtypes of
 * cmis:folder as well.</li>
 * </ul>
 */
public class NuxeoTypeHelper {

    private static final Log log = LogFactory.getLog(NuxeoTypeHelper.class);

    public static final String NUXEO_DOCUMENT = "Document";

    public static final String NUXEO_FOLDER = "Folder";

    public static final String NUXEO_FILE = "File";

    public static final String NUXEO_ORDERED_FOLDER = "OrderedFolder";

    public static final String NX_DUBLINCORE = "dublincore";

    public static final String NX_DC_TITLE = "dc:title";

    public static final String NX_DC_CREATED = "dc:created";

    public static final String NX_DC_CREATOR = "dc:creator";

    public static final String NX_DC_MODIFIED = "dc:modified";

    public static final String NX_ICON = "common:icon";

    private static final String NAMESPACE = "http://ns.nuxeo.org/cmis/type/";

    /**
     * Gets the remapped parent type id, or {@code null} if the type is to be
     * ignored.
     */

    public static String getParentTypeId(DocumentType documentType) {
        String nuxeoTypeId = documentType.getName();
        if (NuxeoTypeHelper.NUXEO_DOCUMENT.equals(nuxeoTypeId)
                || documentType.getFacets().contains(
                        FacetNames.HIDDEN_IN_NAVIGATION)
                || !documentType.hasSchema(NuxeoTypeHelper.NX_DUBLINCORE)) {
            // ignore type
            return null;
        }
        if (NUXEO_FOLDER.equals(nuxeoTypeId)) {
            // Folder has artificial parent cmis:folder
            return BaseTypeId.CMIS_FOLDER.value();
        }
        DocumentType superType = (DocumentType) documentType.getSuperType();
        if (superType == null) {
            // TODO relations
            return null;
        }
        String parentId = mappedId(superType.getName());
        if (NUXEO_FOLDER.equals(parentId)) {
            // reparent Folder child under cmis:folder
            parentId = BaseTypeId.CMIS_FOLDER.value();
        }
        if (NUXEO_DOCUMENT.equals(parentId)) {
            // reparent Document child under cmis:document
            parentId = BaseTypeId.CMIS_DOCUMENT.value();
        }
        return parentId;
    }

    public static TypeDefinition construct(DocumentType documentType,
            String parentId) {
        String nuxeoTypeId = documentType.getName();
        String id = mappedId(nuxeoTypeId);
        // base
        AbstractTypeDefinition type = constructBase(id, parentId,
                documentType.isFolder(), documentType, nuxeoTypeId, true);
        // Nuxeo Property Definitions
        for (Schema schema : documentType.getSchemas()) {
            addSchemaPropertyDefinitions(type, schema);
        }
        return type;
    }

    /**
     * Constructs a base type, not mapped to a Nuxeo type. It has the dublincore
     * schema though. When created, it actually constructs a File or a Folder.
     */
    public static TypeDefinition constructCmisBase(BaseTypeId base,
            SchemaManager schemaManager) {
        AbstractTypeDefinition type = constructBase(base.value(), null,
                base == BaseTypeId.CMIS_FOLDER, null, null, true);
        DocumentType dt = schemaManager.getDocumentType(NUXEO_FOLDER); // has dc
        addSchemaPropertyDefinitions(type, dt.getSchema(NX_DUBLINCORE));
        return type;
    }

    protected static void addSchemaPropertyDefinitions(
            AbstractTypeDefinition type, Schema schema) {
        for (Field field : schema.getFields()) {
            PropertyType propertyType;
            Cardinality cardinality;
            Type fieldType = field.getType();
            if (fieldType.isComplexType()) {
                // complex type
                log.debug("Ignoring complex type: " + schema.getName() + '/'
                        + field.getName() + " in type: " + type.getId());
                continue;
            } else {
                if (fieldType.isListType()) {
                    Type listFieldType = ((ListType) fieldType).getFieldType();
                    if (!listFieldType.isSimpleType()) {
                        // complex list
                        log.debug("Ignoring complex list: " + schema.getName()
                                + '/' + field.getName() + " in type: "
                                + type.getId());
                        continue;
                    } else {
                        // array: use a collection table
                        cardinality = Cardinality.MULTI;
                        propertyType = getPropertType((SimpleType) listFieldType);
                    }
                } else {
                    // primitive type
                    cardinality = Cardinality.SINGLE;
                    propertyType = getPropertType((SimpleType) fieldType);
                }
            }
            String name = field.getName().getPrefixedName();
            PropertyDefinition<?> pd = newPropertyDefinition(name, name,
                    propertyType, cardinality, Updatability.READWRITE, false,
                    false, true);
            if (type.getPropertyDefinitions().containsKey(pd.getId())) {
                throw new RuntimeException(
                        "Property already defined for name: " + name
                                + " in type: " + type.getId());
            }
            type.addPropertyDefinition(pd);
        }
    }

    protected static AbstractTypeDefinition constructBase(String id,
            String parentId, boolean isFolder, DocumentType documentType,
            String nuxeoTypeId, boolean creatable) {
        AbstractTypeDefinition t;
        if (isFolder) {
            t = new FolderTypeDefinitionImpl();
            t.setBaseTypeId(BaseTypeId.CMIS_FOLDER);
        } else {
            t = new DocumentTypeDefinitionImpl();
            t.setBaseTypeId(BaseTypeId.CMIS_DOCUMENT);
        }
        t.setId(id);
        t.setParentTypeId(parentId);
        t.setDescription(id);
        t.setDisplayName(id);
        t.setLocalName(nuxeoTypeId == null ? id : nuxeoTypeId);
        Namespace ns = documentType == null ? null
                : documentType.getNamespace();
        t.setLocalNamespace(ns == null ? NAMESPACE : ns.uri);
        t.setQueryName(id);
        t.setIsCreatable(Boolean.valueOf(creatable));
        t.setIsFileable(Boolean.TRUE);
        t.setIsQueryable(Boolean.TRUE);
        t.setIsIncludedInSupertypeQuery(Boolean.TRUE);
        t.setIsFulltextIndexed(Boolean.TRUE);
        t.setIsControllableAcl(Boolean.FALSE);
        t.setIsControllablePolicy(Boolean.FALSE);
        addBasePropertyDefinitions(t);
        if (t instanceof FolderTypeDefinitionImpl) {
            FolderTypeDefinitionImpl ft = (FolderTypeDefinitionImpl) t;
            addFolderPropertyDefinitions(ft);
        } else {
            DocumentTypeDefinitionImpl dt = (DocumentTypeDefinitionImpl) t;
            dt.setIsVersionable(Boolean.FALSE);
            ContentStreamAllowed csa = (documentType != null && supportsBlobHolder(documentType)) ? ContentStreamAllowed.ALLOWED
                    : ContentStreamAllowed.NOTALLOWED;
            dt.setContentStreamAllowed(csa);
            addDocumentPropertyDefinitions(dt);
        }
        return t;
    }

    protected static void addBasePropertyDefinitions(AbstractTypeDefinition t) {
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.OBJECT_ID,
                "Object ID", PropertyType.ID, Cardinality.SINGLE,
                Updatability.READONLY, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.OBJECT_TYPE_ID, "Type ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.ONCREATE, false, true, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.BASE_TYPE_ID,
                "Base Type ID", PropertyType.ID, Cardinality.SINGLE,
                Updatability.READONLY, false, true, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.NAME, "Name",
                PropertyType.STRING, Cardinality.SINGLE,
                Updatability.READWRITE, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CREATED_BY,
                "Created By", PropertyType.STRING, Cardinality.SINGLE,
                Updatability.READONLY, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.CREATION_DATE, "Creation Date",
                PropertyType.DATETIME, Cardinality.SINGLE,
                Updatability.READONLY, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.LAST_MODIFIED_BY, "Last Modified By",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY,
                false, true, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.LAST_MODIFICATION_DATE, "Last Modification Date",
                PropertyType.DATETIME, Cardinality.SINGLE,
                Updatability.READONLY, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CHANGE_TOKEN,
                "Change Token", PropertyType.STRING, Cardinality.SINGLE,
                Updatability.READONLY, false, false, false));
    }

    protected static void addFolderPropertyDefinitions(
            FolderTypeDefinitionImpl t) {
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.PARENT_ID,
                "Parent ID", PropertyType.ID, Cardinality.SINGLE,
                Updatability.READONLY, false, false, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.PATH, "Path",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY,
                false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS,
                "Allowed Child Object Type IDs", PropertyType.ID,
                Cardinality.MULTI, Updatability.READONLY, false, false, false));
    }

    protected static void addDocumentPropertyDefinitions(
            DocumentTypeDefinitionImpl t) {
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.IS_IMMUTABLE,
                "Is Immutable", PropertyType.BOOLEAN, Cardinality.SINGLE,
                Updatability.READONLY, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.IS_LATEST_VERSION, "Is Latest Version",
                PropertyType.BOOLEAN, Cardinality.SINGLE,
                Updatability.READONLY, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.IS_MAJOR_VERSION, "Is Major Version",
                PropertyType.BOOLEAN, Cardinality.SINGLE,
                Updatability.READONLY, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.IS_LATEST_MAJOR_VERSION, "Is Latest Major Version",
                PropertyType.BOOLEAN, Cardinality.SINGLE,
                Updatability.READONLY, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.VERSION_LABEL, "Version Label",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY,
                false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.VERSION_SERIES_ID, "Version Series ID",
                PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY,
                false, true, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.IS_VERSION_SERIES_CHECKED_OUT,
                "Is Version Series Checked Out", PropertyType.BOOLEAN,
                Cardinality.SINGLE, Updatability.READONLY, false, true, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.VERSION_SERIES_CHECKED_OUT_BY,
                "Version Series Checked Out By", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.VERSION_SERIES_CHECKED_OUT_ID,
                "Version Series Checked Out ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READONLY, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.CHECKIN_COMMENT, "Checkin Comment",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY,
                false, false, false));
        // mandatory properties even when content stream not allowed
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.CONTENT_STREAM_LENGTH, "Content Stream Length",
                PropertyType.INTEGER, Cardinality.SINGLE,
                Updatability.READONLY, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.CONTENT_STREAM_MIME_TYPE, "MIME Type",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY,
                false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.CONTENT_STREAM_FILE_NAME, "Filename",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY,
                false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(
                PropertyIds.CONTENT_STREAM_ID, "Content Stream ID",
                PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY,
                false, false, false));
    }

    protected static PropertyDefinition<?> newPropertyDefinition(String id,
            String displayName, PropertyType propertyType,
            Cardinality cardinality, Updatability updatability,
            boolean inherited, boolean required, boolean queryable) {
        AbstractPropertyDefinition<?> p;
        switch (propertyType) {
        case BOOLEAN:
            p = new PropertyBooleanDefinitionImpl();
            break;
        case DATETIME:
            p = new PropertyDateTimeDefinitionImpl();
            break;
        case DECIMAL:
            p = new PropertyDecimalDefinitionImpl();
            break;
        case HTML:
            p = new PropertyHtmlDefinitionImpl();
            break;
        case ID:
            p = new PropertyIdDefinitionImpl();
            break;
        case INTEGER:
            p = new PropertyIntegerDefinitionImpl();
            break;
        case STRING:
            p = new PropertyStringDefinitionImpl();
            break;
        case URI:
            p = new PropertyUriDefinitionImpl();
            break;
        default:
            throw new RuntimeException(propertyType.toString());
        }
        p.setId(id);
        p.setDescription(displayName);
        p.setDisplayName(displayName);
        p.setLocalName(id);
        p.setLocalNamespace(null); // TODO
        p.setQueryName(id);
        p.setPropertyType(propertyType);
        p.setCardinality(cardinality);
        p.setUpdatability(updatability);
        p.setIsInherited(Boolean.valueOf(inherited));
        p.setIsRequired(Boolean.valueOf(required));
        p.setIsQueryable(Boolean.valueOf(queryable));
        return p;
    }

    // TODO update BlobHolderAdapterService to be able to do this
    // without constructing a fake document
    protected static boolean supportsBlobHolder(DocumentType documentType) {
        DocumentModel doc = new DocumentModelImpl(null, documentType.getName(),
                null, new Path("/"), null, null, null,
                documentType.getSchemaNames(), documentType.getFacets(), null,
                "default");
        return doc.getAdapter(BlobHolder.class) != null;
    }

    /**
     * Turns a Nuxeo type into a CMIS type.
     */
    protected static String mappedId(String id) {
        // we don't map any Nuxeo type anymore to cmis:document or cmis:folder
        return id;
    }

    protected static PropertyType getPropertType(SimpleType type) {
        SimpleType primitive = type.getPrimitiveType();
        if (primitive == StringType.INSTANCE) {
            return PropertyType.STRING;
        } else if (primitive == BooleanType.INSTANCE) {
            return PropertyType.BOOLEAN;
        } else if (primitive == DateType.INSTANCE) {
            return PropertyType.DATETIME;
        } else if (primitive == LongType.INSTANCE) {
            return PropertyType.INTEGER;
        } else if (primitive == DoubleType.INSTANCE) {
            return PropertyType.DECIMAL;
        } else {
            return PropertyType.STRING;
        }
    }

}

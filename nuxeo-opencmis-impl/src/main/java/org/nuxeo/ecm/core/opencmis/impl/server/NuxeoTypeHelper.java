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
import org.nuxeo.ecm.core.schema.Namespace;
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
 */
public class NuxeoTypeHelper {

    private static final Log log = LogFactory.getLog(NuxeoTypeHelper.class);

    public static final String NX_DC_TITLE = "dc:title";

    public static final String NX_DC_CREATED = "dc:created";

    public static final String NX_DC_CREATOR = "dc:creator";

    public static final String NX_DC_MODIFIED = "dc:modified";

    private static final String NAMESPACE = "http://ns.nuxeo.org/cmis/type/";

    public static TypeDefinition construct(DocumentType documentType) {
        String nuxeoId = documentType.getName();
        String id = mappedId(nuxeoId);

        // parent type id
        DocumentType superType = (DocumentType) documentType.getSuperType();
        String parentId;
        if (superType == null || id.equals(BaseTypeId.CMIS_DOCUMENT.value())
                || id.equals(BaseTypeId.CMIS_FOLDER.value())) {
            parentId = null;
        } else {
            String pid = mappedId(superType.getName());
            if (documentType.isFolder()
                    && pid.equals(BaseTypeId.CMIS_DOCUMENT.value())) {
                pid = BaseTypeId.CMIS_FOLDER.value();
            }
            parentId = pid;
        }

        AbstractTypeDefinition t;
        if (documentType.isFolder()) {
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
        t.setLocalName(nuxeoId);
        Namespace ns = documentType.getNamespace();
        t.setLocalNamespace(ns == null ? NAMESPACE : ns.uri);
        t.setQueryName(id);
        t.setIsCreatable(Boolean.TRUE);
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
            ContentStreamAllowed csa = supportsBlobHolder(documentType) ? ContentStreamAllowed.ALLOWED
                    : ContentStreamAllowed.NOTALLOWED;
            dt.setContentStreamAllowed(csa);
            addDocumentPropertyDefinitions(dt);
        }

        // Nuxeo Property Definitions

        for (Schema schema : documentType.getSchemas()) {
            for (Field field : schema.getFields()) {
                String name = field.getName().getPrefixedName();
                if (NX_DC_CREATED.equals(name) || NX_DC_CREATOR.equals(name)
                        || NX_DC_MODIFIED.equals(name)) {
                    // mapped to standard CMIS properties
                    continue;
                }
                PropertyType propertyType;
                Cardinality cardinality;
                Type fieldType = field.getType();
                if (fieldType.isComplexType()) {
                    // complex type
                    log.debug("Chemistry: ignoring complex type: "
                            + schema.getName() + '/' + field.getName());
                    continue;
                } else {
                    if (fieldType.isListType()) {
                        Type listFieldType = ((ListType) fieldType).getFieldType();
                        if (!listFieldType.isSimpleType()) {
                            // complex list
                            log.debug("Chemistry: ignoring complex list: "
                                    + schema.getName() + '/' + field.getName());
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
                PropertyDefinition<?> pd = newPropertyDefinition(name, name,
                        propertyType, cardinality, Updatability.READWRITE,
                        false, false, true);
                if (t.getPropertyDefinitions().containsKey(pd.getId())) {
                    throw new RuntimeException(
                            "Property already defined for name: " + name
                                    + " in type: " + nuxeoId);
                }
                t.addPropertyDefinition(pd);
            }
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
                false, true, true));
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
                false, true, true));
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
        if (t.getContentStreamAllowed() != ContentStreamAllowed.NOTALLOWED) {
            t.addPropertyDefinition(newPropertyDefinition(
                    PropertyIds.CONTENT_STREAM_LENGTH, "Content Stream Length",
                    PropertyType.INTEGER, Cardinality.SINGLE,
                    Updatability.READONLY, false, false, false));
            t.addPropertyDefinition(newPropertyDefinition(
                    PropertyIds.CONTENT_STREAM_MIME_TYPE, "MIME Type",
                    PropertyType.STRING, Cardinality.SINGLE,
                    Updatability.READONLY, false, false, false));
            t.addPropertyDefinition(newPropertyDefinition(
                    PropertyIds.CONTENT_STREAM_FILE_NAME, "Filename",
                    PropertyType.STRING, Cardinality.SINGLE,
                    Updatability.READONLY, false, false, false));
            t.addPropertyDefinition(newPropertyDefinition(
                    PropertyIds.CONTENT_STREAM_ID, "Content Stream ID",
                    PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY,
                    false, false, false));
        }
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
        if (id.equals("Document")) {
            return BaseTypeId.CMIS_DOCUMENT.value();
        }
        if (id.equals("Folder")) {
            return BaseTypeId.CMIS_FOLDER.value();
        }
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

/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
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
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RelationshipTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.SecondaryTypeDefinitionImpl;
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
import org.nuxeo.ecm.core.schema.types.CompositeType;
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
import org.nuxeo.runtime.api.Framework;

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
 * <li>Other folderish types directly under Folder are mapped to subtypes of cmis:folder as well.</li>
 * </ul>
 */
public class NuxeoTypeHelper {

    private static final Log log = LogFactory.getLog(NuxeoTypeHelper.class);

    public static final String NUXEO_DOCUMENT = "Document";

    public static final String NUXEO_FOLDER = "Folder";

    public static final String NUXEO_RELATION = "Relation";

    public static final String NUXEO_RELATION_DEFAULT = "DefaultRelation";

    public static final String NUXEO_FILE = "File";

    public static final String NUXEO_ORDERED_FOLDER = "OrderedFolder";

    public static final String FACET_TYPE_PREFIX = "facet:";

    public static final String NX_DUBLINCORE = "dublincore";

    public static final String NX_DC_TITLE = "dc:title";

    public static final String NX_DC_DESCRIPTION = "dc:description";

    public static final String NX_DC_CREATED = "dc:created";

    public static final String NX_DC_CREATOR = "dc:creator";

    public static final String NX_DC_MODIFIED = "dc:modified";

    public static final String NX_DC_LAST_CONTRIBUTOR = "dc:lastContributor";

    public static final String NX_ICON = "common:icon";

    public static final String NX_REL_SOURCE = "relation:source";

    public static final String NX_REL_TARGET = "relation:target";

    public static final String NX_DIGEST = "nuxeo:contentStreamDigest";

    public static final String NX_ISVERSION = "nuxeo:isVersion";

    public static final String NX_ISCHECKEDIN = "nuxeo:isCheckedIn";

    /**
     * @since 10.2
     */
    public static final String NX_ISTRASHED = "nuxeo:isTrashed";

    public static final String NX_FACETS = "nuxeo:secondaryObjectTypeIds";

    public static final String NX_LIFECYCLE_STATE = "nuxeo:lifecycleState";

    public static final String NX_PARENT_ID = "nuxeo:parentId";

    public static final String NX_PATH_SEGMENT = "nuxeo:pathSegment";

    public static final String ENABLE_COMPLEX_PROPERTIES = "org.nuxeo.cmis.enableComplexProperties";

    /** @since 6.0 */
    public static final String NX_POS = "nuxeo:pos";

    private static final String NAMESPACE = "http://ns.nuxeo.org/cmis/type/";

    private static final String NAMESPACE_FACET = "http://ns.nuxeo.org/cmis/facet/";

    protected static boolean isComplexPropertiesEnabled() {
        return !Framework.isBooleanPropertyFalse(ENABLE_COMPLEX_PROPERTIES);
    }

    protected AbstractTypeDefinition t;

    // used to track down and log duplicates
    protected Map<String, String> propertyToSchema;

    protected CmisVersion cmisVersion;

    /**
     * Helper to construct one CMIS type from a {@link DocumentType}.
     */
    protected NuxeoTypeHelper(String id, String parentId, BaseTypeId baseTypeId, DocumentType documentType,
            String nuxeoTypeId, boolean creatable, CmisVersion cmisVersion) {
        propertyToSchema = new HashMap<String, String>();
        this.cmisVersion = cmisVersion;
        constructBaseDocumentType(id, parentId, baseTypeId, documentType, nuxeoTypeId, creatable);
    }

    /**
     * Helper to construct one CMIS type from a secondary type.
     */
    protected NuxeoTypeHelper(String id, String nuxeoTypeId, CmisVersion cmisVersion) {
        propertyToSchema = new HashMap<String, String>();
        this.cmisVersion = cmisVersion;
        constructBaseSecondaryType(id, nuxeoTypeId);
    }

    /**
     * Gets the remapped parent type id, or {@code null} if the type is to be ignored.
     */
    public static String getParentTypeId(DocumentType documentType) {
        if (!documentType.hasSchema(NX_DUBLINCORE)) {
            // ignore type without dublincore
            return null;
        }
        if (documentType.getFacets().contains(FacetNames.HIDDEN_IN_NAVIGATION)) {
            // ignore hiddeninnavigation type except if it's a relation
            if (getBaseTypeId(documentType) != BaseTypeId.CMIS_RELATIONSHIP) {
                return null;
            }
        }
        String nuxeoTypeId = documentType.getName();
        // NUXEO_DOCUMENT already excluded be previous checks
        if (NUXEO_FOLDER.equals(nuxeoTypeId)) {
            // Folder has artificial parent cmis:folder
            return BaseTypeId.CMIS_FOLDER.value();
        }
        if (NUXEO_RELATION.equals(nuxeoTypeId)) {
            // Relation has artificial parent cmis:relationship
            return BaseTypeId.CMIS_RELATIONSHIP.value();
        }
        DocumentType superType = (DocumentType) documentType.getSuperType();
        if (superType == null) {
            return null;
        }
        String parentId = mappedId(superType.getName());
        if (NUXEO_FOLDER.equals(parentId)) {
            // reparent Folder child under cmis:folder
            parentId = BaseTypeId.CMIS_FOLDER.value();
        }
        if (NUXEO_RELATION.equals(parentId)) {
            // reparent Relation child under cmis:relationship
            parentId = BaseTypeId.CMIS_RELATIONSHIP.value();
        }
        if (NUXEO_DOCUMENT.equals(parentId)) {
            // reparent Document child under cmis:document
            parentId = BaseTypeId.CMIS_DOCUMENT.value();
        }
        return parentId;
    }

    public static TypeDefinition constructDocumentType(DocumentType documentType, String parentId,
            CmisVersion cmisVersion) {
        String nuxeoTypeId = documentType.getName();
        String id = mappedId(nuxeoTypeId);
        NuxeoTypeHelper h = new NuxeoTypeHelper(id, parentId, getBaseTypeId(documentType), documentType, nuxeoTypeId,
                true, cmisVersion);
        // Nuxeo Property Definitions
        for (Schema schema : documentType.getSchemas()) {
            h.addSchemaPropertyDefinitions(schema);
        }
        return h.t;
    }

    public static TypeDefinition constructSecondaryType(CompositeType type, CmisVersion cmisVersion) {
        String nuxeoTypeId = type.getName();
        String id = FACET_TYPE_PREFIX + nuxeoTypeId;
        NuxeoTypeHelper h = new NuxeoTypeHelper(id, nuxeoTypeId, cmisVersion);
        // Nuxeo Property Definitions
        for (Schema schema : type.getSchemas()) {
            h.addSchemaPropertyDefinitions(schema);
        }
        return h.t;
    }

    /**
     * Constructs a base type, not mapped to a Nuxeo type. If not a secondary, it has the dublincore schema.
     */
    public static TypeDefinition constructCmisBase(BaseTypeId baseTypeId, SchemaManager schemaManager,
            CmisVersion cmisVersion) {
        NuxeoTypeHelper h;
        if (baseTypeId != BaseTypeId.CMIS_SECONDARY) {
            h = new NuxeoTypeHelper(baseTypeId.value(), null, baseTypeId, null, null, true, cmisVersion);
            DocumentType dt = schemaManager.getDocumentType(NUXEO_FOLDER); // has dc
            h.addSchemaPropertyDefinitions(dt.getSchema(NX_DUBLINCORE));
        } else {
            h = new NuxeoTypeHelper(baseTypeId.value(), null, cmisVersion);
        }
        return h.t;
    }

    protected void addSchemaPropertyDefinitions(Schema schema) {
        for (Field field : schema.getFields()) {
            PropertyType propertyType;
            Cardinality cardinality;
            Type fieldType = field.getType();
            String schemaName = schema.getName();
            boolean queryable;
            boolean orderable;
            if (fieldType.isComplexType()) {
                if (isComplexPropertiesEnabled()) {
                    // content is specifically excluded from the properties
                    if ("content".equals(fieldType.getName())) {
                        log.debug("Ignoring complex type: " + schemaName + '/' + field.getName() + " in type: "
                                + t.getId());
                        continue;
                    }
                    // complex types get exposed to CMIS as a single string value;
                    // the NuxeoPropertyData class will marshal/unmarshal them as JSON.
                    cardinality = Cardinality.SINGLE;
                    propertyType = PropertyType.STRING;
                    queryable = false;
                    orderable = false;
                } else {
                    log.debug("Ignoring complex type: " + schemaName + '/' + field.getName() + " in type: " + t.getId());
                    continue;
                }
            } else {
                if (fieldType.isListType()) {
                    Type listFieldType = ((ListType) fieldType).getFieldType();
                    if (!listFieldType.isSimpleType()) {
                        if (isComplexPropertiesEnabled()) {
                            // complex lists get exposed to CMIS as a list of string values;
                            // the NuxeoPropertyData class will marshal/unmarshal them as JSON.
                            cardinality = Cardinality.MULTI;
                            propertyType = PropertyType.STRING;
                            queryable = false;
                            orderable = false;
                        } else {
                            log.debug("Ignoring complex type: " + schemaName + '/' + field.getName() + "in type: "
                                    + t.getId());
                            continue;
                        }
                    } else {
                        // array: use a collection table
                        cardinality = Cardinality.MULTI;
                        propertyType = getPropertType((SimpleType) listFieldType);
                        queryable = false;
                        orderable = false;
                    }
                } else {
                    // primitive type
                    cardinality = Cardinality.SINGLE;
                    propertyType = getPropertType((SimpleType) fieldType);
                    queryable = true;
                    orderable = true;
                }
            }
            String name = field.getName().getPrefixedName();
            PropertyDefinition<?> pd = newPropertyDefinition(name, name, propertyType, cardinality,
                    Updatability.READWRITE, false, false, queryable, orderable);
            if (t.getPropertyDefinitions().containsKey(pd.getId())) {
                log.error("Type '" + t.getId() + "' has duplicate property '" + name + "' in schemas '"
                        + propertyToSchema.get(pd.getId()) + "' and '" + schemaName + "', ignoring the one in '"
                        + schemaName + "'");
                continue;
            }
            propertyToSchema.put(pd.getId(), schemaName);
            t.addPropertyDefinition(pd);
        }
    }

    /**
     * Constructs the base for a {@link DocumentType}.
     */
    protected void constructBaseDocumentType(String id, String parentId, BaseTypeId baseTypeId, DocumentType documentType,
            String nuxeoTypeId, boolean creatable) {
        if (baseTypeId == BaseTypeId.CMIS_FOLDER) {
            t = new FolderTypeDefinitionImpl();
        } else if (baseTypeId == BaseTypeId.CMIS_RELATIONSHIP) {
            t = new RelationshipTypeDefinitionImpl();
        } else {
            t = new DocumentTypeDefinitionImpl();
        }
        t.setBaseTypeId(baseTypeId);
        t.setId(id);
        t.setParentTypeId(parentId);
        t.setDescription(id);
        t.setDisplayName(id);
        t.setLocalName(nuxeoTypeId == null ? id : nuxeoTypeId);
        Namespace ns = documentType == null ? null : documentType.getNamespace();
        t.setLocalNamespace(ns == null ? NAMESPACE : ns.uri);
        t.setQueryName(id);
        t.setIsCreatable(Boolean.valueOf(creatable));
        t.setIsQueryable(Boolean.TRUE);
        t.setIsIncludedInSupertypeQuery(Boolean.TRUE);
        t.setIsFulltextIndexed(Boolean.TRUE);
        t.setIsControllableAcl(Boolean.TRUE);
        t.setIsControllablePolicy(Boolean.FALSE);
        addBasePropertyDefinitions();
        if (t instanceof FolderTypeDefinitionImpl) {
            t.setIsFileable(Boolean.TRUE);
            FolderTypeDefinitionImpl ft = (FolderTypeDefinitionImpl) t;
            addFolderPropertyDefinitions(ft);
        } else if (t instanceof RelationshipTypeDefinitionImpl) {
            RelationshipTypeDefinitionImpl rt = (RelationshipTypeDefinitionImpl) t;
            rt.setAllowedSourceTypes(null);
            rt.setAllowedTargetTypes(null);
            addRelationshipPropertyDefinitions(rt);
            t.setIsFileable(Boolean.FALSE);
        } else {
            DocumentTypeDefinitionImpl dt = (DocumentTypeDefinitionImpl) t;
            boolean versionable = documentType == null ? false : documentType.getFacets().contains(
                    FacetNames.VERSIONABLE);
            dt.setIsVersionable(Boolean.valueOf(versionable));
            t.setIsFileable(Boolean.TRUE);
            ContentStreamAllowed csa = (documentType != null && supportsBlobHolder(documentType)) ? ContentStreamAllowed.ALLOWED
                    : ContentStreamAllowed.NOTALLOWED;
            dt.setContentStreamAllowed(csa);
            addDocumentPropertyDefinitions(dt);
        }
    }

    /**
     * Constructs the base for a secondary type.
     */
    protected void constructBaseSecondaryType(String id, String nuxeoTypeId) {
        t = new SecondaryTypeDefinitionImpl();
        t.setBaseTypeId(BaseTypeId.CMIS_SECONDARY);
        t.setId(id);
        t.setParentTypeId(nuxeoTypeId == null ? null : BaseTypeId.CMIS_SECONDARY.value());
        t.setDescription(id);
        t.setDisplayName(id);
        t.setLocalName(nuxeoTypeId == null ? id : nuxeoTypeId);
        t.setLocalNamespace(NAMESPACE_FACET);
        t.setQueryName(id);
        t.setIsCreatable(Boolean.FALSE);
        t.setIsQueryable(Boolean.TRUE);
        t.setIsIncludedInSupertypeQuery(Boolean.TRUE);
        t.setIsFulltextIndexed(Boolean.TRUE);
        t.setIsControllableAcl(Boolean.FALSE);
        t.setIsControllablePolicy(Boolean.FALSE);
        t.setIsFileable(Boolean.FALSE);
    }

    protected void addBasePropertyDefinitions() {
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.OBJECT_ID, "Object ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.OBJECT_TYPE_ID, "Type ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.ONCREATE, false, true, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.BASE_TYPE_ID, "Base Type ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        if (cmisVersion != CmisVersion.CMIS_1_0) {
            t.addPropertyDefinition(newPropertyDefinition(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, "Secondary Type IDs",
                    PropertyType.ID, Cardinality.MULTI, Updatability.READONLY, false, false, false, false));
        }
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.NAME, "Name", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READWRITE, false, true, true, true));
        if (cmisVersion != CmisVersion.CMIS_1_0) {
            t.addPropertyDefinition(newPropertyDefinition(PropertyIds.DESCRIPTION, "Description", PropertyType.STRING,
                    Cardinality.SINGLE, Updatability.READWRITE, false, false, true, true));
        }
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CREATED_BY, "Created By", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CREATION_DATE, "Creation Date",
                PropertyType.DATETIME, Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.LAST_MODIFIED_BY, "Last Modified By",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.LAST_MODIFICATION_DATE, "Last Modification Date",
                PropertyType.DATETIME, Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CHANGE_TOKEN, "Change Token", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));

        // Nuxeo system properties
        t.addPropertyDefinition(newPropertyDefinition(NX_LIFECYCLE_STATE, "Lifecycle State", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(NX_FACETS, "Facets", PropertyType.ID, Cardinality.MULTI,
                Updatability.READONLY, false, false, true, false));
        t.addPropertyDefinition(newPropertyDefinition(NX_PARENT_ID, "Nuxeo Parent ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(NX_PATH_SEGMENT, "Path Segment", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(NX_POS, "Position", PropertyType.INTEGER, Cardinality.SINGLE,
                Updatability.READONLY, false, false, true, true));
    }

    protected static void addFolderPropertyDefinitions(FolderTypeDefinitionImpl t) {
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.PARENT_ID, "Parent ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.PATH, "Path", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS,
                "Allowed Child Object Type IDs", PropertyType.ID, Cardinality.MULTI, Updatability.READONLY, false,
                false, false, false));
    }

    protected static void addRelationshipPropertyDefinitions(RelationshipTypeDefinitionImpl t) {
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.SOURCE_ID, "Source ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READWRITE, false, true, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.TARGET_ID, "Target ID", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READWRITE, false, true, true, true));
    }

    protected void addDocumentPropertyDefinitions(DocumentTypeDefinitionImpl t) {
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.IS_IMMUTABLE, "Is Immutable", PropertyType.BOOLEAN,
                Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.IS_LATEST_VERSION, "Is Latest Version",
                PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.IS_MAJOR_VERSION, "Is Major Version",
                PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.IS_LATEST_MAJOR_VERSION, "Is Latest Major Version",
                PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.VERSION_LABEL, "Version Label", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.VERSION_SERIES_ID, "Version Series ID",
                PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT,
                "Is Version Series Checked Out", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY,
                false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY,
                "Version Series Checked Out By", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false,
                false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID,
                "Version Series Checked Out ID", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, false,
                false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CHECKIN_COMMENT, "Checkin Comment",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        // mandatory properties even when content stream not allowed
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CONTENT_STREAM_LENGTH, "Content Stream Length",
                PropertyType.INTEGER, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CONTENT_STREAM_MIME_TYPE, "MIME Type",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CONTENT_STREAM_FILE_NAME, "Filename",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CONTENT_STREAM_ID, "Content Stream ID",
                PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(PropertyIds.CONTENT_STREAM_HASH, "Content Stream Hashes",
                PropertyType.STRING, Cardinality.MULTI, Updatability.READONLY, false, false, false, false));
        // Nuxeo system properties
        // TODO: make digest queryable at some point
        t.addPropertyDefinition(newPropertyDefinition(NX_DIGEST, "Content Stream Digest", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        t.addPropertyDefinition(newPropertyDefinition(NX_ISVERSION, "Is Version", PropertyType.BOOLEAN,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(NX_ISCHECKEDIN, "Is Checked In PWC", PropertyType.BOOLEAN,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        t.addPropertyDefinition(newPropertyDefinition(NX_ISTRASHED, "Is Trashed", PropertyType.BOOLEAN,
                Cardinality.SINGLE, Updatability.READONLY, false, false, true, true));
        if (cmisVersion != CmisVersion.CMIS_1_0) {
            t.addPropertyDefinition(newPropertyDefinition(PropertyIds.IS_PRIVATE_WORKING_COPY, "Is PWC",
                    PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false, false, false, false));
        }
    }

    protected static PropertyDefinition<?> newPropertyDefinition(String id, String displayName,
            PropertyType propertyType, Cardinality cardinality, Updatability updatability, boolean inherited,
            boolean required, boolean queryable, boolean orderable) {
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
        p.setIsOrderable(Boolean.valueOf(orderable));
        return p;
    }

    // TODO update BlobHolderAdapterService to be able to do this
    // without constructing a fake document
    protected static boolean supportsBlobHolder(DocumentType documentType) {
        DocumentModel doc = new DocumentModelImpl(null, documentType.getName(), null, new Path("/"), null, null, null,
                documentType.getSchemaNames(), documentType.getFacets(), null, "default");
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

    public static BaseTypeId getBaseTypeId(DocumentType type) {
        if (type.isFolder()) {
            return BaseTypeId.CMIS_FOLDER;
        }
        DocumentType t = type;
        do {
            if (NUXEO_RELATION.equals(t.getName())) {
                return BaseTypeId.CMIS_RELATIONSHIP;
            }
            t = (DocumentType) t.getSuperType();
        } while (t != null);
        return BaseTypeId.CMIS_DOCUMENT;
    }

    public static BaseTypeId getBaseTypeId(DocumentModel doc) {
        return getBaseTypeId(doc.getDocumentType());
    }

}

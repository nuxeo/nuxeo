/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.BaseType;
import org.apache.chemistry.Choice;
import org.apache.chemistry.ContentStreamPresence;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.PropertyType;
import org.apache.chemistry.Type;
import org.apache.chemistry.Updatability;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class NuxeoType implements Type {

    private static final Log log = LogFactory.getLog(NuxeoType.class);

    public static final String NX_DC_CREATED = "dc:created";

    public static final String NX_DC_CREATOR = "dc:creator";

    public static final String NX_DC_MODIFIED = "dc:modified";

    private static final NuxeoPropertyDefinition PROP_ID = new NuxeoPropertyDefinition(
            Property.ID, "def:id", "Id", "", false, PropertyType.ID, false,
            null, false, true, null, Updatability.READ_ONLY, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_URI = new NuxeoPropertyDefinition(
            Property.URI, "def:uri", "URI", "", false, PropertyType.URI, false,
            null, false, false, null, Updatability.READ_ONLY, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_TYPE_ID = new NuxeoPropertyDefinition(
            Property.TYPE_ID, "def:typeid", "Type ID", "", false,
            PropertyType.ID, false, null, false, true, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CREATED_BY = new NuxeoPropertyDefinition(
            Property.CREATED_BY, "def:createdby", "Created By", "", false,
            PropertyType.STRING, false, null, false, true, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CREATION_DATE = new NuxeoPropertyDefinition(
            Property.CREATION_DATE, "def:creationdate", "Creation Date", "",
            false, PropertyType.DATETIME, false, null, false, true, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_LAST_MODIFIED_BY = new NuxeoPropertyDefinition(
            Property.LAST_MODIFIED_BY, "def:lastmodifiedby",
            "Last Modified By", "", false, PropertyType.STRING, false, null,
            false, true, null, Updatability.READ_ONLY, true, true, 0, null,
            null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_LAST_MODIFICATION_DATE = new NuxeoPropertyDefinition(
            Property.LAST_MODIFICATION_DATE, "def:lastmodificationdate",
            "Last Modification Date", "", false, PropertyType.DATETIME, false,
            null, false, true, null, Updatability.READ_ONLY, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CHANGE_TOKEN = new NuxeoPropertyDefinition(
            Property.CHANGE_TOKEN, "def:changetoken", "Change Token", "",
            false, PropertyType.STRING, false, null, false, false, null,
            Updatability.READ_WRITE, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_NAME = new NuxeoPropertyDefinition(
            Property.NAME, "def:name", "Name", "", false, PropertyType.STRING,
            false, null, false, true, null, Updatability.READ_WRITE, true,
            true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_IS_LATEST_VERSION = new NuxeoPropertyDefinition(
            Property.IS_LATEST_VERSION, "def:islatestversion",
            "Is Latest Version", "", false, PropertyType.BOOLEAN, false, null,
            false, true, null, Updatability.READ_ONLY, true, true, 0, null,
            null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_IS_MAJOR_VERSION = new NuxeoPropertyDefinition(
            Property.IS_MAJOR_VERSION, "def:ismajorversion",
            "Is Major Version", "", false, PropertyType.BOOLEAN, false, null,
            false, false, null, Updatability.READ_ONLY, true, true, 0, null,
            null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_IS_LATEST_MAJOR_VERSION = new NuxeoPropertyDefinition(
            Property.IS_LATEST_MAJOR_VERSION, "def:islatestmajorversion",
            "Is Latest Major Version", "", false, PropertyType.BOOLEAN, false,
            null, false, true, null, Updatability.READ_ONLY, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_VERSION_LABEL = new NuxeoPropertyDefinition(
            Property.VERSION_LABEL, "def:versionlabel", "Version Label", "",
            false, PropertyType.STRING, false, null, false, true, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_VERSION_SERIES_ID = new NuxeoPropertyDefinition(
            Property.VERSION_SERIES_ID, "def:versionseriesid",
            "Version Series ID", "", false, PropertyType.ID, false, null,
            false, true, null, Updatability.READ_ONLY, true, true, 0, null,
            null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_IS_VERSION_SERIES_CHECKED_OUT = new NuxeoPropertyDefinition(
            Property.IS_VERSION_SERIES_CHECKED_OUT,
            "def:isversionseriescheckedout", "Is Version Series Checked Out",
            "", false, PropertyType.BOOLEAN, false, null, false, true, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_VERSION_SERIES_CHECKED_OUT_BY = new NuxeoPropertyDefinition(
            Property.VERSION_SERIES_CHECKED_OUT_BY,
            "def:versionseriescheckedoutby", "Version Series Checked Out By",
            "", false, PropertyType.STRING, false, null, false, false, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_VERSION_SERIES_CHECKED_OUT_ID = new NuxeoPropertyDefinition(
            Property.VERSION_SERIES_CHECKED_OUT_ID,
            "def:versionseriescheckedoutid", "Version Series Checked Out Id",
            "", false, PropertyType.ID, false, null, false, false, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CHECKIN_COMMENT = new NuxeoPropertyDefinition(
            Property.CHECKIN_COMMENT, "def:checkincomment", "Checkin Comment",
            "", false, PropertyType.STRING, false, null, false, false, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CONTENT_STREAM_ALLOWED = new NuxeoPropertyDefinition(
            Property.CONTENT_STREAM_ALLOWED, "def:contentstreamallowed",
            "Content Stream Allowed", "", false, PropertyType.STRING, false,
            null, false, true, null, Updatability.READ_ONLY, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CONTENT_STREAM_LENGTH = new NuxeoPropertyDefinition(
            Property.CONTENT_STREAM_LENGTH, "def:contentstreamlength",
            "Content Stream Length", "", false, PropertyType.INTEGER, false,
            null, false, false, null, Updatability.READ_ONLY, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CONTENT_STREAM_MIME_TYPE = new NuxeoPropertyDefinition(
            Property.CONTENT_STREAM_MIME_TYPE, "def:contentstreammimetype",
            "Content Stream MIME Type", "", false, PropertyType.STRING, false,
            null, false, false, null, Updatability.READ_ONLY, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CONTENT_STREAM_FILENAME = new NuxeoPropertyDefinition(
            Property.CONTENT_STREAM_FILENAME, "def:contentstreamfilename",
            "Content Stream Filename", "", false, PropertyType.STRING, false,
            null, false, false, null, Updatability.READ_WRITE, true, true, 0,
            null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_CONTENT_STREAM_URI = new NuxeoPropertyDefinition(
            Property.CONTENT_STREAM_URI, "def:contentstreamuri",
            "Content Stream URI", "", false, PropertyType.URI, false, null,
            false, false, null, Updatability.READ_ONLY, true, true, 0, null,
            null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_PARENT_ID = new NuxeoPropertyDefinition(
            Property.PARENT_ID, "def:parentid", "Parent Id", "", false,
            PropertyType.ID, false, null, false, true, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_ALLOWED_CHILD_OBJECT_TYPE_IDS = new NuxeoPropertyDefinition(
            Property.ALLOWED_CHILD_OBJECT_TYPE_IDS,
            "def:allowedchildobjecttypeids", "Allowed Child Object Type Ids",
            "", false, PropertyType.ID, true, null, false, false, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_SOURCE_ID = new NuxeoPropertyDefinition(
            Property.SOURCE_ID, "def:sourceid", "Source Id", "", false,
            PropertyType.ID, false, null, false, true, null,
            Updatability.READ_WRITE, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_TARGET_ID = new NuxeoPropertyDefinition(
            Property.TARGET_ID, "def:targetid", "Target Id", "", false,
            PropertyType.ID, false, null, false, true, null,
            Updatability.READ_WRITE, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_POLICY_NAME = new NuxeoPropertyDefinition(
            Property.POLICY_NAME, "def:policyname", "Policy Name", "", false,
            PropertyType.STRING, false, null, false, true, null,
            Updatability.READ_ONLY, true, true, 0, null, null, -1, null, null);

    private static final NuxeoPropertyDefinition PROP_POLICY_TEXT = new NuxeoPropertyDefinition(
            Property.POLICY_TEXT, "def:policytext", "Policy Text", "", false,
            PropertyType.STRING, false, null, false, true, null,
            Updatability.READ_WRITE, true, true, 0, null, null, -1, null, null);

    private final DocumentType documentType;

    private final Map<String, PropertyDefinition> propertyDefinitions;

    private final ContentStreamPresence contentStreamAllowed;

    public NuxeoType(DocumentType documentType) {
        this.documentType = documentType;

        Map<String, PropertyDefinition> map = new HashMap<String, PropertyDefinition>();
        for (PropertyDefinition def : getBasePropertyDefinitions(getBaseType())) {
            String name = def.getName();
            if (map.containsKey(name)) { // XXX debug
                throw new RuntimeException(
                        "Property already defined for name: " + name);
            }
            map.put(name, def);
        }

        boolean hasFileSchema = false;
        for (Schema schema : documentType.getSchemas()) {
            if ("file".equals(schema.getName())) {
                hasFileSchema = true;
            }
            for (Field field : schema.getFields()) {
                String prefixedName = field.getName().getPrefixedName();

                String name;
                boolean inherited = false;
                boolean multiValued;
                List<Choice> choices = null;
                boolean openChoice = false;
                boolean required = false;
                Serializable defaultValue = null;
                boolean queryable = true;
                boolean orderable = true;

                PropertyType cmisType = PropertyType.STRING;

                org.nuxeo.ecm.core.schema.types.Type fieldType = field.getType();
                if (fieldType.isComplexType()) {
                    // complex type
                    log.info("Chemistry: ignoring complex type: "
                            + schema.getName() + '/' + field.getName());
                    continue;
                } else {
                    if (fieldType.isListType()) {
                        org.nuxeo.ecm.core.schema.types.Type listFieldType = ((ListType) fieldType).getFieldType();
                        if (!listFieldType.isSimpleType()) {
                            // complex list
                            log.info("Chemistry: ignoring complex list: "
                                    + schema.getName() + '/' + field.getName());
                            continue;
                        } else {
                            // Array: use a collection table
                            multiValued = true;
                            cmisType = getPropertType((SimpleType)listFieldType);
                        }
                    } else {
                        // primitive type
                        multiValued = false;
                        cmisType = getPropertType((SimpleType)fieldType);
                    }
                }
                // ad-hoc mappings
                if (NX_DC_CREATED.equals(prefixedName)
                        || NX_DC_CREATOR.equals(prefixedName)
                        || NX_DC_MODIFIED.equals(prefixedName)) {
                    // mapped to standard CMIS properties
                    continue;
                }
                name = prefixedName;

                PropertyDefinition def = new NuxeoPropertyDefinition(name,
                        "def:nx:" + name, name, "", inherited,
                        cmisType, multiValued, choices, openChoice,
                        required, defaultValue, Updatability.READ_WRITE,
                        queryable, orderable, 0, null, null, -1, null, null);
                if (map.containsKey(name)) {
                    throw new RuntimeException(
                            "Property already defined for name: " + name);
                }
                map.put(name, def);
            }
        }
        propertyDefinitions = map;
        contentStreamAllowed = BaseType.DOCUMENT.equals(getBaseType())
                && hasFileSchema ? ContentStreamPresence.ALLOWED
                : ContentStreamPresence.NOT_ALLOWED;

    }

    protected PropertyType getPropertType(org.nuxeo.ecm.core.schema.types.SimpleType type) {
        org.nuxeo.ecm.core.schema.types.SimpleType primitive = type.getPrimitiveType();
        if (primitive == StringType.INSTANCE) {
            return PropertyType.STRING;
        } else if (primitive == BooleanType.INSTANCE) {
            return PropertyType.BOOLEAN;
        } else if (primitive == DateType.INSTANCE) {
            return PropertyType.DATETIME;
        } else if (primitive == LongType.INSTANCE) {
            return PropertyType.INTEGER;
        }
        return PropertyType.STRING;
    }

    private List<NuxeoPropertyDefinition> getBasePropertyDefinitions(
            BaseType baseType) {
        List<NuxeoPropertyDefinition> defs = new ArrayList<NuxeoPropertyDefinition>(
                Arrays.asList( //
                        PROP_ID, //
                        PROP_URI, //
                        PROP_TYPE_ID, //
                        PROP_CREATED_BY,//
                        PROP_CREATION_DATE, //
                        PROP_LAST_MODIFIED_BY,//
                        PROP_LAST_MODIFICATION_DATE, //
                        PROP_CHANGE_TOKEN));
        switch (baseType) {
        case DOCUMENT:
            defs.addAll(Arrays.asList( //
                    PROP_NAME, //
                    PROP_IS_LATEST_VERSION, //
                    PROP_IS_MAJOR_VERSION, //
                    PROP_IS_LATEST_MAJOR_VERSION, //
                    PROP_VERSION_LABEL, //
                    PROP_VERSION_SERIES_ID, //
                    PROP_IS_VERSION_SERIES_CHECKED_OUT, //
                    PROP_VERSION_SERIES_CHECKED_OUT_BY, //
                    PROP_VERSION_SERIES_CHECKED_OUT_ID, //
                    PROP_CHECKIN_COMMENT, //
                    PROP_CONTENT_STREAM_ALLOWED, //
                    PROP_CONTENT_STREAM_LENGTH, //
                    PROP_CONTENT_STREAM_MIME_TYPE, //
                    PROP_CONTENT_STREAM_FILENAME, //
                    PROP_CONTENT_STREAM_URI));
            break;
        case FOLDER:
            defs.addAll(Arrays.asList( //
                    PROP_NAME, //
                    PROP_PARENT_ID, //
                    PROP_ALLOWED_CHILD_OBJECT_TYPE_IDS));
            break;
        case RELATIONSHIP:
            defs.addAll(Arrays.asList( //
                    PROP_SOURCE_ID, //
                    PROP_TARGET_ID));
            break;
        case POLICY:
            defs.addAll(Arrays.asList( //
                    PROP_POLICY_NAME, //
                    PROP_POLICY_TEXT));
            break;
        }
        return defs;
    }

    public String getId() {
        return documentType.getName();
    }

    public String getQueryName() {
        return getId();
    }

    public String getDisplayName() {
        return getId();
    }

    public String getParentId() {
        String name = documentType.getName();
        if ("Document".equals(name) || "Folder".equals(name)) {
            return null;
        }
        org.nuxeo.ecm.core.schema.types.Type superType = documentType.getSuperType();
        return superType == null ? null : superType.getName();
    }

    public BaseType getBaseType() {
        if (documentType.isFolder()) {
            return BaseType.FOLDER;
        }
        return BaseType.DOCUMENT;
    }

    public String getBaseTypeQueryName() {
        switch (getBaseType()) {
        case DOCUMENT:
            return "Document";
        case FOLDER:
            return "Folder";
        case POLICY:
            return "Policy";
        case RELATIONSHIP:
            return "Relationship";
        }
        throw new UnsupportedOperationException(getBaseType().toString());
    }

    public String getDescription() {
        return documentType.getName();
    }

    public boolean isCreatable() {
        return true;
    }

    public boolean isQueryable() {
        return true;
    }

    public boolean isControllable() {
        return true;
    }

    public boolean isIncludedInSuperTypeQuery() {
        return true;
    }

    public boolean isFileable() {
        return true;
    }

    public boolean isVersionable() {
        return BaseType.DOCUMENT.equals(getBaseType());
    }

    public ContentStreamPresence getContentStreamAllowed() {
        return contentStreamAllowed;
    }

    public String[] getAllowedSourceTypes() {
        return null;
    }

    public String[] getAllowedTargetTypes() {
        return null;
    }

    public Collection<PropertyDefinition> getPropertyDefinitions() {
        return Collections.unmodifiableCollection(propertyDefinitions.values());
    }

    public PropertyDefinition getPropertyDefinition(String name) {
        return propertyDefinitions.get(name);
    }

}

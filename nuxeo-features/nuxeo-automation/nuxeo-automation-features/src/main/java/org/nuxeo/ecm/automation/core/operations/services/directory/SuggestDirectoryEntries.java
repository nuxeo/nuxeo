/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.automation.core.operations.services.directory;

import java.io.IOException;
import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.features.SuggestConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter;

/**
 * SuggestDirectoryEntries Operation
 *
 * @since 5.7.3
 */
@Operation(id = SuggestDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Get suggested directory entries", description = "Get the entries suggestions of a directory. This is returning a blob containing a serialized JSON array. Prefix parameter is used to filter the entries.", addToStudio = false)
public class SuggestDirectoryEntries {

    /**
     * @since 5.9.3
     */
    Collator collator;

    /**
     * Convenient class to build JSON serialization of results.
     *
     * @since 5.7.2
     */
    private class JSONAdapter implements Comparable<JSONAdapter> {

        private final Map<String, JSONAdapter> children;

        private final Session session;

        private final Schema schema;

        private boolean isRoot = false;

        private Boolean isLeaf = null;

        private Map<String, Object> obj;

        public JSONAdapter(Session session, Schema schema) {
            this.session = session;
            this.schema = schema;
            this.children = new HashMap<>();
            // We are the root node
            this.isRoot = true;
        }

        public JSONAdapter(Session session, Schema schema, DocumentModel entry) throws PropertyException {
            this(session, schema);
            // Carry entry, not root
            isRoot = false;
            // build JSON object for this entry
            obj = new LinkedHashMap<>();
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Field field : schema.getFields()) {
                QName fieldName = field.getName();
                String key = fieldName.getLocalName();
                Serializable value = entry.getPropertyValue(fieldName.getPrefixedName());
                if (value != null) {
                    if (label.equals(key)) {
                        if (localize && !dbl10n) {
                            // translations are in messages*.properties files
                            value = translate(value.toString());
                        }
                        obj.put(SuggestConstants.LABEL, value);
                    }
                    obj.put(key, value);
                    properties.put(key, value);
                }
            }
            if (displayObsoleteEntries) {
                if (obj.containsKey(SuggestConstants.OBSOLETE_FIELD_ID)
                        && Integer.parseInt(obj.get(SuggestConstants.OBSOLETE_FIELD_ID).toString()) > 0) {
                    obj.put(SuggestConstants.WARN_MESSAGE_LABEL, getObsoleteWarningMessage());
                }
            }
            obj.put("directoryName", directoryName);
            obj.put("properties", properties);
            obj.put(MarshallingConstants.ENTITY_FIELD_NAME, DirectoryEntryJsonWriter.ENTITY_TYPE);
        }

        @Override
        public int compareTo(JSONAdapter other) {
            if (other != null) {
                int i = this.getOrder() - other.getOrder();
                if (i != 0) {
                    return i;
                } else {
                    return getCollator().compare(this.getLabel(), other.getLabel());
                }
            } else {
                return -1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            JSONAdapter other = (JSONAdapter) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (this.obj == null) {
                if (other.obj != null) {
                    return false;
                }
            } else if (!this.obj.equals(other.obj)) {
                return false;
            }
            return true;
        }

        public List<Map<String, Object>> getChildrenJSONArray() {
            List<Map<String, Object>> result = new ArrayList<>();
            for (JSONAdapter ja : getSortedChildren()) {
                // When serializing in JSON, we are now able to COMPUTED_ID
                // which is the chained path of the entry (i.e absolute path
                // considering its ancestor)
                ja.getObj().put(SuggestConstants.COMPUTED_ID,
                        (!isRoot ? (getComputedId() + keySeparator) : "") + ja.getId());
                ja.getObj().put(SuggestConstants.ABSOLUTE_LABEL,
                        (!isRoot ? (getAbsoluteLabel() + absoluteLabelSeparator) : "") + ja.getLabel());
                result.add(ja.toJSONObject());
            }
            return result;
        }

        public String getComputedId() {
            return isRoot ? null : obj.getOrDefault(SuggestConstants.COMPUTED_ID, "").toString();
        }

        public String getId() {
            return isRoot ? null : obj.getOrDefault(SuggestConstants.ID, "").toString();
        }

        public String getLabel() {
            return isRoot ? null : obj.getOrDefault(SuggestConstants.LABEL, "").toString();
        }

        public String getAbsoluteLabel() {
            return isRoot ? null : obj.getOrDefault(SuggestConstants.ABSOLUTE_LABEL, "").toString();
        }

        public Map<String, Object> getObj() {
            return obj;
        }

        public int getOrder() {
            return isRoot ? -1
                    : Integer.parseInt(obj.getOrDefault(SuggestConstants.DIRECTORY_ORDER_FIELD_NAME, "0").toString());
        }

        private SuggestDirectoryEntries getOuterType() {
            return SuggestDirectoryEntries.this;
        }

        public String getParentId() {
            return isRoot ? null : obj.getOrDefault(SuggestConstants.PARENT_FIELD_ID, "").toString();
        }

        public List<JSONAdapter> getSortedChildren() {
            if (children == null) {
                return null;
            }
            List<JSONAdapter> result = new ArrayList<>(children.values());
            Collections.sort(result);
            return result;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((obj == null) ? 0 : obj.hashCode());
            return result;
        }

        /**
         * Does the associated vocabulary / directory entry have child entries.
         *
         * @return true if it has children
         * @since 5.7.2
         */
        public boolean isLeaf() {
            if (isLeaf == null) {
                if (isChained) {
                    String id = getId();
                    if (id != null) {
                        Map<String, Serializable> filter = Collections.singletonMap(SuggestConstants.PARENT_FIELD_ID,
                                getId());
                        try {
                            isLeaf = session.query(filter, Collections.emptySet(), Collections.emptyMap(), false, 1, -1)
                                            .isEmpty();
                        } catch (DirectoryException ce) {
                            log.error("Could not retrieve children of entry", ce);
                            isLeaf = true;
                        }
                    } else {
                        isLeaf = true;
                    }
                } else {
                    isLeaf = true;
                }
            }
            return isLeaf;
        }

        public boolean isObsolete() {
            int obsoleteFieldId = Integer.parseInt(
                    obj.getOrDefault(SuggestConstants.OBSOLETE_FIELD_ID, "0").toString());
            return isRoot ? false : obsoleteFieldId > 0;
        }

        private void mergeJsonAdapter(JSONAdapter branch) {
            JSONAdapter found = children.get(branch.getId());
            if (found != null) {
                for (JSONAdapter branchChild : branch.children.values()) {
                    found.mergeJsonAdapter(branchChild);
                }
            } else {
                children.put(branch.getId(), branch);
            }
        }

        public JSONAdapter push(final JSONAdapter newEntry) throws PropertyException {
            String parentIdOfNewEntry = newEntry.getParentId();
            if (parentIdOfNewEntry != null && !parentIdOfNewEntry.isEmpty()) {
                // The given adapter has a parent which could already be in my
                // descendants
                if (parentIdOfNewEntry.equals(this.getId())) {
                    // this is the parent. We must insert the given adapter
                    // here. We merge all its
                    // descendants
                    mergeJsonAdapter(newEntry);
                    return this;
                } else {
                    // I am not the parent, let's check if I could be the
                    // parent
                    // of one the ancestor.
                    final String parentId = newEntry.getParentId();
                    DocumentModel parent = session.getEntry(parentId);
                    if (parent == null) {
                        if (log.isInfoEnabled()) {
                            log.info(String.format("parent %s not found for entry %s", parentId, newEntry.getId()));
                        }
                        mergeJsonAdapter(newEntry);
                        return this;
                    } else {
                        return push(new JSONAdapter(session, schema, parent).push(newEntry));
                    }
                }
            } else {
                // The given adapter has no parent, I can merge it in my
                // descendants.
                mergeJsonAdapter(newEntry);
                return this;
            }
        }

        private Map<String, Object> toJSONObject() {
            if (isLeaf()) {
                return getObj();
            } else {
                // This entry has sub entries in the directory.
                // Ruled by Select2: an optionGroup is selectable or not
                // whether
                // we provide an Id or not in the JSON object.
                if (canSelectParent) {
                    // Make it selectable, keep as it is
                    Map<String, Object> obj = getObj();
                    obj.put("children", getChildrenJSONArray());
                    return obj;
                } else {
                    // We don't want it to be selectable, we just serialize the
                    // label
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put(SuggestConstants.LABEL, getLabel());
                    obj.put("children", getChildrenJSONArray());
                    return obj;
                }
            }
        }

        @Override
        public String toString() {
            return String.valueOf(obj);
        }

    }

    private static final Log log = LogFactory.getLog(SuggestDirectoryEntries.class);

    public static final String ID = "Directory.SuggestEntries";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "localize", required = false)
    protected boolean localize;

    @Param(name = "lang", required = false)
    protected String lang;

    @Param(name = "searchTerm", alias = "prefix", required = false)
    protected String prefix;

    @Param(name = "labelFieldName", required = false)
    protected String labelFieldName = SuggestConstants.DIRECTORY_DEFAULT_LABEL_COL_NAME;

    @Param(name = "dbl10n", required = false)
    protected boolean dbl10n = false;

    @Param(name = "canSelectParent", required = false)
    protected boolean canSelectParent = false;

    @Param(name = "filterParent", required = false)
    protected boolean filterParent = false;

    @Param(name = "keySeparator", required = false)
    protected String keySeparator = SuggestConstants.DEFAULT_KEY_SEPARATOR;

    @Param(name = "displayObsoleteEntries", required = false)
    protected boolean displayObsoleteEntries = false;

    /**
     * @since 10.2
     */
    @Param(name = "filters", required = false)
    protected Properties filters = new Properties();

    /**
     * @since 8.2
     */
    @Param(name = "limit", required = false)
    protected int limit = -1;

    /**
     * Fetch mode. If not contains, then starts with.
     *
     * @since 5.9.2
     */
    @Param(name = "contains", required = false)
    protected boolean contains = false;

    /**
     * Choose if sort is case sensitive
     *
     * @since 5.9.3
     */
    @Param(name = "caseSensitive", required = false)
    protected boolean caseSensitive = false;

    /**
     * Separator to display absolute label
     *
     * @since 5.9.2
     */
    @Param(name = "absoluteLabelSeparator", required = false)
    protected String absoluteLabelSeparator = "/";

    private String label = null;

    private boolean isChained = false;

    private String obsoleteWarningMessage = null;

    protected String getLang() {
        if (lang == null) {
            lang = (String) ctx.get("lang");
            if (lang == null) {
                lang = SuggestConstants.DEFAULT_LANG;
            }
        }
        return lang;
    }

    protected Locale getLocale() {
        return new Locale(getLang());
    }

    /**
     * @since 5.9.3
     */
    protected Collator getCollator() {
        if (collator == null) {
            collator = Collator.getInstance(getLocale());
            if (caseSensitive) {
                collator.setStrength(Collator.TERTIARY);
            } else {
                collator.setStrength(Collator.SECONDARY);
            }
        }
        return collator;
    }

    protected String getObsoleteWarningMessage() {
        if (obsoleteWarningMessage == null) {
            obsoleteWarningMessage = I18NUtils.getMessageString("messages", "obsolete", new Object[0], getLocale());
        }
        return obsoleteWarningMessage;
    }

    @OperationMethod
    public Blob run() throws IOException {
        Directory directory = directoryService.getDirectory(directoryName);
        if (directory == null) {
            log.error("Could not find directory with name " + directoryName);
            return null;
        }
        try (Session session = directory.getSession()) {
            String schemaName = directory.getSchema();
            Schema schema = schemaManager.getSchema(schemaName);

            Field parentField = schema.getField(SuggestConstants.PARENT_FIELD_ID);
            isChained = parentField != null;

            String parentDirectory = directory.getParentDirectory();
            if (parentDirectory == null || parentDirectory.isEmpty() || parentDirectory.equals(directoryName)) {
                parentDirectory = null;
            }

            boolean postFilter = true;

            label = SuggestConstants.getLabelFieldName(schema, dbl10n, labelFieldName, getLang());

            Map<String, Serializable> filter = new HashMap<>();
            if (!displayObsoleteEntries) {
                // Exclude obsolete
                filter.put(SuggestConstants.OBSOLETE_FIELD_ID, Long.valueOf(0));
            }
            Set<String> fullText = new TreeSet<>();
            if (dbl10n || !localize) {
                postFilter = false;
                // do the filtering at directory level
                if (prefix != null && !prefix.isEmpty()) {
                    // filter.put(directory.getIdField(), prefix);
                    String computedPrefix = prefix;
                    if (contains) {
                        computedPrefix = '%' + computedPrefix;
                    }
                    filter.put(label, computedPrefix);
                    fullText.add(label);
                }
            }

            for (Map.Entry<String,String> entry : filters.entrySet()) {
                filter.put(entry.getKey(), entry.getValue());
            }

            // when post filtering we need to get all entries
            DocumentModelList entries = session.query(filter, fullText, Collections.emptyMap(), false,
                    postFilter ? -1 : limit, -1);

            JSONAdapter jsonAdapter = new JSONAdapter(session, schema);

            for (DocumentModel entry : entries) {
                JSONAdapter adapter = new JSONAdapter(session, schema, entry);
                if (!filterParent && isChained && parentDirectory == null) {
                    if (!adapter.isLeaf()) {
                        continue;
                    }
                }

                if (prefix != null && !prefix.isEmpty() && postFilter) {
                    if (contains) {
                        if (!adapter.getLabel().toLowerCase().contains(prefix.toLowerCase())) {
                            continue;
                        }
                    } else {
                        if (!adapter.getLabel().toLowerCase().startsWith(prefix.toLowerCase())) {
                            continue;
                        }
                    }
                }

                jsonAdapter.push(adapter);

            }
            return Blobs.createJSONBlobFromValue(jsonAdapter.getChildrenJSONArray());
        }
    }

    protected String translate(final String key) {
        if (key == null) {
            return "";
        }
        return I18NUtils.getMessageString("messages", key, new Object[0], getLocale());
    }

}

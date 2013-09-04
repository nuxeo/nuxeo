/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.select2.automation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;

/**
 * SuggestDirectoryEntries Operation
 *
 * @since 5.7.3
 */
@Operation(id = SuggestDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Get directory entries", description = "Get the entries of a directory. This is returning a blob containing a serialized JSON array. The input document, if specified, is used as a context for a potential local configuration of the directory.")
public class SuggestDirectoryEntries {

    /**
     * Convenient class to build JSON serialization of results.
     *
     * @since 5.7.2
     */
    private class JSONAdapter {

        private final Map<String, JSONAdapter> children;

        private final Session session;

        private final Schema schema;

        private boolean isRoot = false;

        private Boolean isLeaf = null;

        private JSONObject obj;

        public JSONAdapter(Session session, Schema schema) {
            this.session = session;
            this.schema = schema;
            // Use TreeMap that entries are naturally sorted
            children = new TreeMap<String, JSONAdapter>();
            // We are the root node
            this.isRoot = true;
        }

        public JSONAdapter(Session session, Schema schema, DocumentModel entry)
                throws PropertyException, ClientException {
            this(session, schema);
            // Carry entry, not root
            isRoot = false;
            // build JSON object for this entry
            obj = new JSONObject();
            for (Field field : schema.getFields()) {
                QName fieldName = field.getName();
                String key = fieldName.getLocalName();
                Serializable value = entry.getPropertyValue(fieldName.getPrefixedName());
                if (label.equals(key)) {
                    if (translateLabels && !dbl10n) {
                        // translations are in messages*.properties files
                        value = translate(value.toString());
                    }
                    obj.element(Select2Common.LABEL, value);
                } else {
                    obj.element(key, value);
                }
            }
            if (obsolete) {
                if (obj.containsKey(Select2Common.OBSOLETE_FIELD_ID)
                        && obj.getInt(Select2Common.OBSOLETE_FIELD_ID) > 0) {
                    obj.element(Select2Common.WARN_MESSAGE_LABEL, getObsoleteWarningMessage());
                }
            }
        }

        public Map<String, JSONAdapter> getChildren() {
            return children;
        }

        public JSONArray getChildrenJSONArray() {
            JSONArray result = new JSONArray();
            for (JSONAdapter ja : children.values()) {
                // When serializing in JSON, we are now able to COMPUTED_ID
                // which is the chained path of the entry (i.e absolute path
                // considering its ancestor)
                ja.getObj().element(
                        Select2Common.COMPUTED_ID,
                        (!isRoot ? (getComputedId() + keySeparator) : "")
                                + ja.getId());
                result.add(ja.toJSONObject());
            }
            return result;
        }

        public String getComputedId() {
            return isRoot ? null : (String) obj.get(Select2Common.COMPUTED_ID);
        }

        public String getId() {
            return isRoot ? null : (String) obj.get(Select2Common.ID);
        }

        public String getLabel() {
            return isRoot ? null : (String) obj.get(Select2Common.LABEL);
        }

        public JSONObject getObj() {
            return obj;
        }

        public String getParentId() {
            return isRoot ? null
                    : (String) obj.get(Select2Common.PARENT_FIELD_ID);
        }

        /**
         * Does the associated vocabulary / directory entry have child entries.
         *
         * @return true if it has children
         *
         * @since 5.7.2
         */
        public boolean isLeaf() {
            if (isLeaf == null) {
                if (isChained) {
                    String id = getId();
                    if (id != null) {
                        Map<String, Serializable> filter = new HashMap<String, Serializable>();
                        filter.put(Select2Common.PARENT_FIELD_ID, getId());
                        try {
                            isLeaf = session.query(filter).isEmpty();
                        } catch (ClientException ce) {
                            log.error("Could not retrieve children of entry",
                                    ce);
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
            return isRoot ? false
                    : obj.getInt(Select2Common.OBSOLETE_FIELD_ID) > 0;
        }

        private void mergeJsonAdapter(JSONAdapter branch) {
            JSONAdapter found = getChildren().get(branch.getLabel());
            if (found != null) {
                // I already have the given the adapter as child, let's merge
                // all its children.
                for (JSONAdapter branchChild : branch.getChildren().values()) {
                    found.mergeJsonAdapter(branchChild);
                }
            } else {
                // First time I see this adapter, I adopt it.
                // We use label as key, this way display will be alphabetically
                // sorted
                getChildren().put(branch.getLabel(), branch);
            }
        }

        public JSONAdapter push(final JSONAdapter newEntry)
                throws PropertyException, ClientException {
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
                    // I am not the parent, let's check if I could be the parent
                    // of one the ancestor.
                    DocumentModel parent = session.getEntry(newEntry.getParentId());
                    return push(new JSONAdapter(session, schema, parent).push(newEntry));
                }
            } else {
                // The given adapter has no parent, I can merge it in my
                // descendants.
                mergeJsonAdapter(newEntry);
                return this;
            }
        }

        private JSONObject toJSONObject() {
            if (isLeaf()) {
                return getObj();
            } else {
                // This entry has sub entries in the directory.
                // Ruled by Select2: an optionGroup is selectable or not whether
                // we provide an Id or not in the JSON object.
                if (canSelectParent) {
                    // Make it selectable, keep as it is
                    return getObj().element("children", getChildrenJSONArray());
                } else {
                    // We don't want it to be selectable, we just serialize the
                    // label
                    return new JSONObject().element(Select2Common.LABEL,
                            getLabel()).element("children",
                            getChildrenJSONArray());
                }
            }
        }

        public String toString() {
            return obj != null ? obj.toString() : null;
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

    @Param(name = "translateLabels", required = false)
    protected boolean translateLabels;

    @Param(name = "lang", required = false)
    protected String lang;

    @Param(name = "prefix", required = false)
    protected String prefix;

    @Param(name = "labelFieldName", required = false)
    protected String labelFieldName = Select2Common.LABEL;

    @Param(name = "dbl10n", required = false)
    protected boolean dbl10n = false;

    @Param(name = "canSelectParent", required = false)
    protected boolean canSelectParent = false;

    @Param(name = "filterParent", required = false)
    protected boolean filterParent = false;

    @Param(name = "keySeparator", required = false)
    protected String keySeparator = Select2Common.DEFAULT_KEY_SEPARATOR;

    @Param(name = "obsolete", required = false)
    protected boolean obsolete = false;

    private String label = null;

    private boolean isChained = false;

    private String obsoleteWarningMessage = null;

    protected String getLang() {
        if (lang == null) {
            lang = (String) ctx.get("lang");
            if (lang == null) {
                lang = "en";
            }
        }
        return lang;
    }

    protected Locale getLocale() {
        return new Locale(getLang());
    }

    protected String getObsoleteWarningMessage() {
        if (obsoleteWarningMessage == null) {
            obsoleteWarningMessage = I18NUtils.getMessageString("messages",
                    "obsolete", new Object[0], getLocale());
        }
        return obsoleteWarningMessage;
    }

    @OperationMethod
    public Blob run() throws Exception {
        Directory directory = directoryService.getDirectory(directoryName);
        if (directory == null) {
            log.error("Could not find directory with name " + directoryName);
            return null;
        }
        Session session = null;
        try {
            session = directory.getSession();
            String schemaName = directory.getSchema();
            Schema schema = schemaManager.getSchema(schemaName);

            Field parentField = schema.getField(Select2Common.PARENT_FIELD_ID);
            isChained = parentField != null;

            String parentDirectory = directory.getParentDirectory();
            if (parentDirectory == null || parentDirectory.isEmpty()
                    || parentDirectory.equals(directoryName)) {
                parentDirectory = null;
            }

            DocumentModelList entries = null;
            boolean postFilter = true;

            label = Select2Common.getLabelFieldName(schema, dbl10n,
                    labelFieldName, getLang());

            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            if (!obsolete) {
                filter.put(Select2Common.OBSOLETE_FIELD_ID, Long.valueOf(0));
            }
            Set<String> fullText = new TreeSet<String>();
            if (dbl10n && !translateLabels) {
                postFilter = false;
                // do the filtering at directory level
                if (prefix != null && !prefix.isEmpty()) {
                    // filter.put(directory.getIdField(), prefix);
                    filter.put(label, prefix);
                    fullText.add(label);
                }
                if (filter.isEmpty()) {
                    // No filtering and we want the obsolete. We take all the
                    // entries
                    entries = session.getEntries();
                } else {
                    // We at least filter with prefix or/and exclude the
                    // obsolete
                    entries = session.query(filter, fullText);
                }
            } else {
                // Labels are translated in properties file, we have to post
                // filter manually on all the entries
                if (filter.isEmpty()) {
                    // We want the obsolete. We take all the entries
                    entries = session.getEntries();
                } else {
                    // We want to exclude the obsolete
                    entries = session.query(filter);
                }
            }

            JSONAdapter jsonAdapter = new JSONAdapter(session, schema);

            for (DocumentModel entry : entries) {
                JSONAdapter adapter = new JSONAdapter(session, schema, entry);
                if (!filterParent && isChained && parentDirectory == null) {
                    if (!adapter.isLeaf())
                        continue;
                }

                if (prefix != null && !prefix.isEmpty() && postFilter) {
                    if (!adapter.getLabel().toLowerCase().startsWith(
                            prefix.toLowerCase())) {
                        continue;
                    }
                }

                jsonAdapter.push(adapter);

            }
            return new StringBlob(
                    jsonAdapter.getChildrenJSONArray().toString(),
                    "application/json");
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (ClientException ce) {
                log.error("Could not close directory session", ce);
            }
        }
    }

    protected String translate(final String key) {
        if (key == null) {
            return "";
        }
        return I18NUtils.getMessageString("messages", key, new Object[0],
                getLocale());
    }

}

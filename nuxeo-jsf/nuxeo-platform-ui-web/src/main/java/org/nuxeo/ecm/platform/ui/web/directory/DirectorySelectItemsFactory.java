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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.PropertyNotFoundException;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemsFactory;
import org.nuxeo.ecm.platform.ui.web.component.VariableManager;

/**
 * @since 6.0
 */
public abstract class DirectorySelectItemsFactory extends SelectItemsFactory {

    private static final Log log = LogFactory.getLog(DirectorySelectItemsFactory.class);

    @Override
    protected abstract String getVar();

    protected abstract String getDirectoryName();

    protected abstract String getFilter();

    protected abstract boolean isDisplayObsoleteEntries();

    /**
     * @since 9.1
     */
    protected abstract boolean isNotDisplayDefaultOption();

    protected abstract DirectorySelectItem createSelectItem(String label, Long ordering);

    protected abstract String[] retrieveSelectEntryId();

    protected abstract Object retrieveItemLabel();

    protected abstract String retrieveLabelFromEntry(DocumentModel directoryEntry);

    protected abstract Long retrieveOrderingFromEntry(DocumentModel directoryEntry);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<DirectorySelectItem> createDirectorySelectItems(Object value, String separator) {
        Object varValue = saveRequestMapVarValue();
        try {
            // build select items
            List<DirectorySelectItem> items = new ArrayList<DirectorySelectItem>();
            String dirName = getDirectoryName();
            if (StringUtils.isBlank(dirName)) {
                items.add(new DirectorySelectItem("", "ERROR: mising directoryName property "
                        + "configuration on widget"));
            } else {
                try (Session directorySession = DirectorySelectItemFactory.getDirectorySession(dirName)) {
                    if (directorySession != null) {
                        if (value instanceof ListDataModel) {
                            ListDataModel ldm = (ListDataModel) value;
                            List<Object> entries = (List) ldm.getWrappedData();
                            for (Object entry : entries) {
                                DirectorySelectItem res = createSelectItemFrom(directorySession, separator, entry);
                                if (res != null) {
                                    items.add(res);
                                }
                            }
                        } else if (value instanceof Collection) {
                            Collection<Object> collection = (Collection<Object>) value;
                            for (Object entry : collection) {
                                DirectorySelectItem res = createSelectItemFrom(directorySession, separator, entry);
                                if (res != null) {
                                    items.add(res);
                                }
                            }
                        } else if (value instanceof Object[]) {
                            Object[] entries = (Object[]) value;
                            for (Object entry : entries) {
                                DirectorySelectItem res = createSelectItemFrom(directorySession, separator, entry);
                                if (res != null) {
                                    items.add(res);
                                }
                            }
                        }
                    } else {
                        items.add(new DirectorySelectItem("", String.format(
                                "ERROR: mising directorySession for directory '%s'", dirName)));
                    }
                }
            }
            return items;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    @SuppressWarnings("boxing")
    public List<DirectorySelectItem> createAllDirectorySelectItems() {
        return createAllDirectorySelectItems(ChainSelect.DEFAULT_KEY_SEPARATOR);
    }

    /**
     * @since 7.3
     */
    @SuppressWarnings("boxing")
    public List<DirectorySelectItem> createAllDirectorySelectItems(String separator) {
        Object varValue = saveRequestMapVarValue();
        try {
            List<DirectorySelectItem> items = new ArrayList<DirectorySelectItem>();
            try (Session directorySession = DirectorySelectItemFactory.getDirectorySession(getDirectoryName())) {
                if (directorySession != null) {
                    Map<String, Serializable> filter = new HashMap<String, Serializable>();
                    if (!isDisplayObsoleteEntries()) {
                        filter.put("obsolete", 0);
                    }
                    if (getFilter() != null) {
                        filter.put("parentFilter", getFilter());
                    }
                    DocumentModelList entries = directorySession.query(filter);
                    for (DocumentModel entry : entries) {
                        if (entry != null) {
                            List<DocumentModel> entryL = new ArrayList<DocumentModel>();
                            entryL.add(entry);
                            DirectorySelectItem res = createSelectItemForEntry(entry, separator, entry);
                            if (res != null) {
                                items.add(res);
                            }
                        }
                    }
                } else {
                    log.error("No session provided for directory, returning empty selection");
                }
            }
            return items;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected String[] retrieveEntryIdFrom(Object item) {
        Object varValue = saveRequestMapVarValue();
        try {
            putIteratorToRequestParam(item);
            String[] id = retrieveSelectEntryId();
            removeIteratorFromRequestParam();
            return id;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected DirectorySelectItem createSelectItemForEntry(Object itemValue, DocumentModel ... entries) {
        return createSelectItemForEntry(itemValue, ChainSelect.DEFAULT_KEY_SEPARATOR, entries);
    }

    /**
     * @since 7.4
     */
    protected DirectorySelectItem createSelectItemForEntry(Object itemValue, String separator, DocumentModel ... entries) {
        return createSelectItemForEntry(itemValue, separator, null, entries);
    }

    /**
     * @since 7.3
     */
    protected DirectorySelectItem createSelectItemForEntry(Object itemValue, String separator, String[] defaultLabels, DocumentModel ... entries) {
        if (defaultLabels != null && (entries.length != defaultLabels.length)) {
            throw new IllegalArgumentException("entryIds  must be the same size that entries");
        }
        String var = getVar();
        String varEntry = var + "Entry";
        Object varEntryExisting = VariableManager.saveRequestMapVarValue(varEntry);

        DirectorySelectItem selectItem = null;
        try {
            VariableManager.putVariableToRequestParam(var, itemValue);
            VariableManager.putVariableToRequestParam(varEntry, entries[entries.length - 1]);
            String label = "";
            for (int i = 0; i < entries.length; i++) {
                final DocumentModel entry = entries[i];
                if (label.length() != 0) {
                    label += separator;
                }
                if (entry == null && defaultLabels != null) {
                    label += defaultLabels[i];
                } else {
                    label += retrieveLabelFromEntry(entry);
                }
            }
            Long ordering = retrieveOrderingFromEntry(entries[entries.length - 1]);
            selectItem = createSelectItem(label, ordering);
            removeIteratorFromRequestParam();
            VariableManager.removeVariableFromRequestParam(var);
            VariableManager.removeVariableFromRequestParam(varEntry);
            if (selectItem != null) {
                return selectItem;
            } else if (itemValue instanceof DirectorySelectItem) {
                // maybe lookup was not necessary
                return (DirectorySelectItem) itemValue;
            }
            return selectItem;
        } catch (PropertyNotFoundException e) {
            if (itemValue instanceof DirectorySelectItem) {
                // maybe lookup was not necessary
                return (DirectorySelectItem) itemValue;
            } else {
                throw e;
            }
        } finally {
            VariableManager.restoreRequestMapVarValue(varEntry, varEntryExisting);
        }

    }

    protected DirectorySelectItem createSelectItemFrom(Session session, Object entry) {
        return createSelectItemFrom(session, ChainSelect.DEFAULT_KEY_SEPARATOR, entry);
    }

    /**
     * @since 7.3
     */
    protected DirectorySelectItem createSelectItemFrom(Session session, String separator, Object entry) {
        String[] entryIds;
        if (entry instanceof String) {
            entryIds = new String[] {(String) entry};
        } else {
            // first resolve entry id to be able to lookup
            // corresponding doc entry
            entryIds = retrieveEntryIdFrom(entry);
        }
        if (entryIds == null || entryIds.length == 0) {
            return null;
        }
        try {
            DocumentModel[] docEntries = new DocumentModel[entryIds.length];
            int i = 0;
            for (String entryId : entryIds) {
                DocumentModel docEntry = session.getEntry(entryId);
                docEntries[i] = docEntry;
                i++;
            }
            if (docEntries == null || docEntries.length == 0) {
                putIteratorToRequestParam(entry);
                Object labelObject = retrieveItemLabel();
                String label = labelObject == null ? null : String.valueOf(labelObject);
                if (StringUtils.isBlank(label) && entry != null) {
                    label = entry.toString();
                }
                DirectorySelectItem item = createSelectItem(label, Long.valueOf(0L));
                removeIteratorFromRequestParam();
                return item;
            }
            return createSelectItemForEntry(entry, separator, entryIds, docEntries);
        } catch (DirectoryException e) {
        }
        return null;
    }

    @Override
    public SelectItem createSelectItem() {
        throw new IllegalArgumentException("Use createSelectDirectoryItems instead");
    }

}

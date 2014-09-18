/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemsFactory;
import org.nuxeo.ecm.platform.ui.web.component.VariableManager;

/**
 * @since 5.9.6
 */
public abstract class DirectorySelectItemsFactory extends SelectItemsFactory {

    private static final Log log = LogFactory.getLog(DirectorySelectItemsFactory.class);

    protected abstract String getVar();

    protected abstract String getDirectoryName();

    protected abstract String getFilter();

    protected abstract boolean isDisplayObsoleteEntries();

    protected abstract DirectorySelectItem createSelectItem(String label,
            Long ordering);

    protected abstract String retrieveSelectEntryId();

    protected abstract String retrieveLabelFromEntry(
            DocumentModel directoryEntry);

    protected abstract Long retrieveOrderingFromEntry(
            DocumentModel directoryEntry);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<DirectorySelectItem> createDirectorySelectItems(Object value) {
        Object varValue = saveRequestMapVarValue();
        try {
            // build select items
            List<DirectorySelectItem> items = new ArrayList<DirectorySelectItem>();
            String dirName = getDirectoryName();
            if (StringUtils.isBlank(dirName)) {
                items.add(new DirectorySelectItem("",
                        "ERROR: mising directoryName property "
                                + "configuration on widget"));
            } else {
                Session directorySession = DirectorySelectItemFactory.getDirectorySession(dirName);
                if (directorySession != null) {
                    if (value instanceof ListDataModel) {
                        ListDataModel ldm = (ListDataModel) value;
                        List<Object> entries = (List) ldm.getWrappedData();
                        for (Object entry : entries) {
                            DirectorySelectItem res = createSelectItemFrom(
                                    directorySession, entry);
                            if (res != null) {
                                items.add(res);
                            }
                        }
                    } else if (value instanceof Collection) {
                        Collection<Object> collection = (Collection<Object>) value;
                        for (Object entry : collection) {
                            DirectorySelectItem res = createSelectItemFrom(
                                    directorySession, entry);
                            if (res != null) {
                                items.add(res);
                            }
                        }
                    } else if (value instanceof Object[]) {
                        Object[] entries = (Object[]) value;
                        for (Object entry : entries) {
                            DirectorySelectItem res = createSelectItemFrom(
                                    directorySession, entry);
                            if (res != null) {
                                items.add(res);
                            }
                        }
                    }
                } else {
                    items.add(new DirectorySelectItem(
                            "",
                            String.format(
                                    "ERROR: mising directorySession for directory '%s'",
                                    dirName)));
                }
                DirectorySelectItemFactory.closeDirectorySession(directorySession);
            }
            return items;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    @SuppressWarnings("boxing")
    public List<DirectorySelectItem> createAllDirectorySelectItems() {
        Object varValue = saveRequestMapVarValue();
        try {
            List<DirectorySelectItem> items = new ArrayList<DirectorySelectItem>();
            Session directorySession = DirectorySelectItemFactory.getDirectorySession(getDirectoryName());
            if (directorySession != null) {
                try {
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
                            DirectorySelectItem res = createSelectItemForEntry(
                                    entry, entry);
                            if (res != null) {
                                items.add(res);
                            }
                        }
                    }
                } catch (ClientException e) {
                    log.error(e, e);
                }
            } else {
                log.error("No session provided for directory, returning empty selection");
            }
            DirectorySelectItemFactory.closeDirectorySession(directorySession);
            return items;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected String retrieveEntryIdFrom(Object item) {
        Object varValue = saveRequestMapVarValue();
        try {
            putIteratorToRequestParam(item);
            String id = retrieveSelectEntryId();
            removeIteratorFromRequestParam();
            return id;
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected DirectorySelectItem createSelectItemForEntry(Object itemValue,
            DocumentModel entry) {
        String var = getVar();
        String varEntry = var + "Entry";
        Object varEntryExisting = VariableManager.saveRequestMapVarValue(varEntry);
        try {
            VariableManager.putVariableToRequestParam(var, itemValue);
            VariableManager.putVariableToRequestParam(varEntry, entry);
            String label = retrieveLabelFromEntry(entry);
            Long ordering = retrieveOrderingFromEntry(entry);
            DirectorySelectItem selectItem = createSelectItem(label, ordering);
            removeIteratorFromRequestParam();
            VariableManager.removeVariableFromRequestParam(var);
            VariableManager.removeVariableFromRequestParam(varEntry);
            return selectItem;
        } finally {
            VariableManager.restoreRequestMapVarValue(varEntry,
                    varEntryExisting);
        }

    }

    protected DirectorySelectItem createSelectItemFrom(Session session,
            Object entry) {
        String entryId;
        if (entry instanceof String) {
            entryId = (String) entry;
        } else {
            // first resolve entry id to be able to lookup
            // corresponding doc entry
            entryId = retrieveEntryIdFrom(entry);
        }
        if (StringUtils.isBlank(entryId)) {
            return null;
        }
        try {
            DocumentModel docEntry = session.getEntry(entryId);
            if (docEntry == null) {
                return new DirectorySelectItem(entryId, entryId, 0L, false,
                        false);
            }
            return createSelectItemForEntry(entry, docEntry);
        } catch (DirectoryException e) {
        }
        return null;
    }

    @Override
    public SelectItem createSelectItem() {
        throw new IllegalArgumentException(
                "Use createSelectDirectoryItems instead");
    }

}
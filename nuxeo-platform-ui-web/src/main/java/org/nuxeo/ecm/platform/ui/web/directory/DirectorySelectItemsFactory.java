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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.ListDataModel;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemsFactory;

/**
 * @since 5.9.6
 */
public abstract class DirectorySelectItemsFactory extends SelectItemsFactory {

    private static final Log log = LogFactory.getLog(DirectorySelectItemsFactory.class);

    protected abstract String retrieveSelectEntryId();

    protected abstract DirectorySelectItem createSelectItem();

    protected abstract String getDirectoryName();

    protected abstract String getFilter();

    protected abstract boolean isDisplayObsoleteEntries();

    @SuppressWarnings({ "unchecked", "rawtypes", "boxing" })
    public DirectorySelectItem[] createSelectItems(Object value) {
        Object varValue = saveRequestMapVarValue();
        try {
            // build select items
            List<DirectorySelectItem> items = new ArrayList<DirectorySelectItem>();
            Session directorySession = DirectorySelectItemFactory.getDirectorySession(getDirectoryName());
            if (directorySession != null) {
                if (value instanceof ListDataModel) {
                    ListDataModel ldm = (ListDataModel) value;
                    List<Object> entries = (List) ldm.getWrappedData();
                    for (Object entry : entries) {
                        DirectorySelectItem[] res = createSelectItemsFrom(
                                directorySession, entry);
                        if (res != null) {
                            items.addAll(Arrays.asList(res));
                        }
                    }
                } else if (value instanceof Collection) {
                    Collection<Object> collection = (Collection<Object>) value;
                    for (Object entry : collection) {
                        DirectorySelectItem[] res = createSelectItemsFrom(
                                directorySession, entry);
                        if (res != null) {
                            items.addAll(Arrays.asList(res));
                        }
                    }
                } else if (value instanceof Object[]) {
                    Object[] entries = (Object[]) value;
                    for (Object entry : entries) {
                        DirectorySelectItem[] res = createSelectItemsFrom(
                                directorySession, entry);
                        if (res != null) {
                            items.addAll(Arrays.asList(res));
                        }
                    }
                }
            } else {
                log.error("No session provided for directory, returning empty selection");
            }
            DirectorySelectItemFactory.closeDirectorySession(directorySession);
            String ordering = getOrdering();
            Boolean caseSensitive = isCaseSensitive();
            if (!StringUtils.isBlank(ordering)) {
                Collections.sort(items, new DirectorySelectItemComparator(
                        ordering, caseSensitive));
            }
            return items.toArray(new DirectorySelectItem[] {});
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    @SuppressWarnings("boxing")
    public DirectorySelectItem[] createAllSelectItems() {
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
                            DirectorySelectItem[] res = createSelectItemsFrom(entry);
                            if (res != null) {
                                items.addAll(Arrays.asList(res));
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
            String ordering = getOrdering();
            Boolean caseSensitive = isCaseSensitive();
            if (!StringUtils.isBlank(ordering)) {
                Collections.sort(items, new DirectorySelectItemComparator(
                        ordering, caseSensitive));
            }
            return items.toArray(new DirectorySelectItem[] {});
        } finally {
            restoreRequestMapVarValue(varValue);
        }
    }

    protected String retrieveEntryIdFrom(Object item) {
        putIteratorToRequestParam(item);
        String id = retrieveSelectEntryId();
        removeIteratorFromRequestParam();
        return id;
    }

    protected DirectorySelectItem[] createSelectItemsFrom(Session session,
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
            return createSelectItemsFrom(docEntry);
        } catch (DirectoryException e) {
        }
        return null;
    }

    protected DirectorySelectItem[] createSelectItemsFrom(Object item) {
        putIteratorToRequestParam(item);
        DirectorySelectItem selectItem = createSelectItem();
        removeIteratorFromRequestParam();
        if (selectItem != null) {
            return new DirectorySelectItem[] { selectItem };
        }
        return null;
    }

}
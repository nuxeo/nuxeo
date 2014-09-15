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
import javax.faces.model.SelectItemGroup;

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

    protected abstract DirectorySelectItem createSelectItem();

    protected abstract String getDirectoryName();

    protected abstract String getFilter();

    protected abstract boolean isDisplayObsoleteEntries();

    @SuppressWarnings({ "unchecked", "rawtypes", "boxing" })
    public DirectorySelectItem[] createSelectItems(Object value) {
        if (value instanceof DirectorySelectItem[]) {
            return (DirectorySelectItem[]) value;
        }
        Object varValue = saveRequestMapVarValue();
        try {
            // build select items
            List<DirectorySelectItem> items = new ArrayList<DirectorySelectItem>();
            Session directorySession = DirectorySelectItemFactory.getDirectorySession(getDirectoryName());
            if (directorySession != null) {
                if (value instanceof ListDataModel) {
                    ListDataModel ldm = (ListDataModel) value;
                    List<String> entryIds = (List) ldm.getWrappedData();
                    DocumentModel entry = null;
                    for (String entryId : entryIds) {
                        try {
                            entry = directorySession.getEntry(entryId);
                            if (entry != null) {
                                DirectorySelectItem[] res = createSelectItemsFrom(entry);
                                if (res != null) {
                                    items.addAll(Arrays.asList(res));
                                }
                            }
                        } catch (DirectoryException e) {
                        }
                    }
                } else if (value instanceof Collection) {
                    Collection<Object> collection = (Collection<Object>) value;
                    DocumentModel entry;
                    for (Object currentItem : collection) {
                        if (currentItem instanceof SelectItemGroup) {
                            DirectorySelectItem[] res = createSelectItemsFrom(currentItem);
                            if (res != null) {
                                items.addAll(Arrays.asList(res));
                            }
                        } else if (currentItem instanceof String) {
                            try {
                                entry = directorySession.getEntry((String) currentItem);
                                DirectorySelectItem[] res = createSelectItemsFrom(entry);
                                if (res != null) {
                                    items.addAll(Arrays.asList(res));
                                }
                            } catch (DirectoryException e) {
                            }
                        }
                    }
                } else if (value instanceof String[]) {
                    String[] entryIds = (String[]) value;
                    DocumentModel entry = null;
                    for (String entryId : entryIds) {
                        try {
                            entry = directorySession.getEntry(entryId);
                            DirectorySelectItem[] res = createSelectItemsFrom(entry);
                            if (res != null) {
                                items.addAll(Arrays.asList(res));
                            }
                        } catch (DirectoryException e) {
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

    protected DirectorySelectItem[] createSelectItemsFrom(Object item) {
        if (item instanceof DirectorySelectItem) {
            return new DirectorySelectItem[] { (DirectorySelectItem) item };
        } else {
            putIteratorToRequestParam(item);
            DirectorySelectItem selectItem = createSelectItem();
            removeIteratorFromRequestParam();
            if (selectItem != null) {
                return new DirectorySelectItem[] { selectItem };
            }
        }
        return null;
    }

}
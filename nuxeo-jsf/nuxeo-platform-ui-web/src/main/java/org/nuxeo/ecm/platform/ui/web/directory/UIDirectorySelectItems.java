/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: UIDirectorySelectItems.java 29556 2008-01-23 00:59:39Z jcarsique $
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

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;

/**
 * Component that deals with a list of select items from a directory.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIDirectorySelectItems extends UISelectItems {

    private static final Log log = LogFactory.getLog(UIDirectorySelectItems.class);

    public static final String COMPONENT_TYPE = UIDirectorySelectItems.class.getName();

    protected String directoryName;

    protected SelectItem[] allValues;

    protected Boolean displayAll;

    protected Boolean displayObsoleteEntries;

    // setters & getters

    public String getDirectoryName() {
        if (directoryName != null) {
            return directoryName;
        }
        ValueExpression ve = getValueExpression("directoryName");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return null;
        }
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public Boolean getDisplayAll() {
        if (displayAll != null) {
            return displayAll;
        }
        ValueExpression ve = getValueExpression("displayAll");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return true;
        }
    }

    public void setDisplayAll(Boolean displayAll) {
        this.displayAll = displayAll;
    }

    public Boolean getDisplayObsoleteEntries() {
        if (displayObsoleteEntries != null) {
            return displayObsoleteEntries;
        }
        ValueExpression ve = getValueExpression("displayObsoleteEntries");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return false;
        }
    }

    public void setDisplayObsoleteEntries(Boolean displayObsoleteEntries) {
        this.displayObsoleteEntries = displayObsoleteEntries;
    }

    protected Session getDirectorySession() {
        String dirName = getDirectoryName();
        Session directorySession = null;
        if (dirName != null) {
            try {
                DirectoryService service = DirectoryHelper.getDirectoryService();
                directorySession = service.open(dirName);
            } catch (Exception e) {
                log.error(String.format("Error when retrieving directory %s",
                        dirName), e);
            }
        }
        return directorySession;
    }

    protected void closeDirectorySession(Session directorySession) {
        if (directorySession != null) {
            try {
                directorySession.close();
            } catch (DirectoryException e) {
            }
        }
    }

    @Override
    public Object getValue() {
        Boolean showAll = getDisplayAll();
        if (showAll) {
            if (allValues == null) {
                allValues = createAllSelectItems();
            }
            return allValues;
        } else {
            Object value = super.getValue();
            return createSelectItems(value);
        }
    }

    /**
     * Builds the selection map using values as entry ids.
     * <p />
     * Supports {@link ListDataModel}, {@link Collection} and String[].
     */
    @Override
    @SuppressWarnings("unchecked")
    protected SelectItem[] createSelectItems(Object value) {
        if (value instanceof SelectItem[]) {
            return (SelectItem[]) value;
        }
        // build select items
        List<SelectItem> items = new ArrayList<SelectItem>();
        Session directorySession = getDirectorySession();
        if (directorySession != null) {
            if (value instanceof ListDataModel) {
                ListDataModel ldm = (ListDataModel) value;
                List<String> entryIds = (List) ldm.getWrappedData();
                DocumentModel entry = null;
                for (String entryId : entryIds) {
                    try {
                        entry = directorySession.getEntry(entryId);
                        if (entry != null) {
                            putIteratorToRequestParam(entry);
                            SelectItem selectItem = createSelectItem();
                            removeIteratorFromRequestParam();
                            if (selectItem != null) {
                                items.add(selectItem);
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
                        SelectItemGroup itemGroup = (SelectItemGroup) currentItem;
                        SelectItem[] itemsFromGroup = itemGroup.getSelectItems();
                        items.addAll(Arrays.asList(itemsFromGroup));
                    } else if (currentItem instanceof String) {
                        try {
                            entry = directorySession.getEntry((String) currentItem);
                            if (entry != null) {
                                putIteratorToRequestParam(entry);
                                SelectItem selectItem = createSelectItem();
                                removeIteratorFromRequestParam();
                                if (selectItem != null) {
                                    items.add(selectItem);
                                }
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
                        if (entry != null) {
                            putIteratorToRequestParam(entry);
                            SelectItem selectItem = createSelectItem();
                            removeIteratorFromRequestParam();
                            if (selectItem != null) {
                                items.add(selectItem);
                            }
                        }
                    } catch (DirectoryException e) {
                    }
                }
            }
        } else {
            log.error("No session provided for directory, returning empty selection");
        }
        closeDirectorySession(directorySession);
        String ordering = getOrdering();
        Boolean caseSensitive = getCaseSensitive();
        if (ordering != null && !"".equals(ordering)) {
            Collections.sort(items, new SelectItemComparator(ordering,
                    caseSensitive));
        }
        return items.toArray(new SelectItem[] {});
    }

    protected SelectItem[] createAllSelectItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        Session directorySession = getDirectorySession();
        if (directorySession != null) {
            try {
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                if (!getDisplayObsoleteEntries()) {
                    filter.put("obsolete", 0);
                }
                DocumentModelList entries = directorySession.query(filter);
                for (DocumentModel entry : entries) {
                    if (entry != null) {
                        putIteratorToRequestParam(entry);
                        SelectItem selectItem = createSelectItem();
                        removeIteratorFromRequestParam();
                        if (selectItem != null) {
                            items.add(selectItem);
                        }
                    }
                }
            } catch (ClientException e) {
            }
        } else {
            log.error("No session provided for directory, returning empty selection");
        }
        closeDirectorySession(directorySession);
        String ordering = getOrdering();
        Boolean caseSensitive = getCaseSensitive();
        if (ordering != null && !"".equals(ordering)) {
            Collections.sort(items, new SelectItemComparator(ordering,
                    caseSensitive));
        }
        return items.toArray(new SelectItem[] {});
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[5];
        values[0] = super.saveState(context);
        values[1] = directoryName;
        values[2] = displayAll;
        values[3] = allValues;
        values[4] = displayObsoleteEntries;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        directoryName = (String) values[1];
        displayAll = (Boolean) values[2];
        allValues = (SelectItem[]) values[3];
        displayObsoleteEntries = (Boolean) values[4];
    }

}

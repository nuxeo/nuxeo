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

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;

/**
 * Component that deals with a list of select items from a directory.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIDirectorySelectItems extends UISelectItems {

    public static final String COMPONENT_TYPE = UIDirectorySelectItems.class.getName();

    enum PropertyKeys {
        directoryName, allValues, displayAll, displayObsoleteEntries
    }

    // setters & getters

    public String getDirectoryName() {
        return (String) getStateHelper().eval(PropertyKeys.directoryName);
    }

    public void setDirectoryName(String directoryName) {
        getStateHelper().put(PropertyKeys.directoryName, directoryName);
    }

    public SelectItem[] getAllValues() {
        return (SelectItem[]) getStateHelper().eval(PropertyKeys.allValues);
    }

    public void setAllValues(SelectItem[] allValues) {
        getStateHelper().put(PropertyKeys.allValues, allValues);
    }

    public Boolean getDisplayAll() {
        return (Boolean) getStateHelper().eval(PropertyKeys.displayAll,
                Boolean.TRUE);
    }

    public void setDisplayAll(Boolean displayAll) {
        getStateHelper().put(PropertyKeys.displayAll, displayAll);
    }

    public Boolean getDisplayObsoleteEntries() {
        return (Boolean) getStateHelper().eval(
                PropertyKeys.displayObsoleteEntries, Boolean.FALSE);
    }

    public void setDisplayObsoleteEntries(Boolean displayObsoleteEntries) {
        getStateHelper().put(PropertyKeys.displayObsoleteEntries,
                displayObsoleteEntries);
    }

    @Override
    public Object getValue() {
        Boolean showAll = getDisplayAll();
        DirectorySelectItemsFactory f = new DirectorySelectItemsFactory() {

            @Override
            protected String getVar() {
                return UIDirectorySelectItems.this.getVar();
            }

            @Override
            protected SelectItem createSelectItem() {
                return UIDirectorySelectItems.this.createSelectItem();
            }

            @Override
            protected String getOrdering() {
                return UIDirectorySelectItems.this.getOrdering();
            }

            @Override
            protected boolean isCaseSensitive() {
                return UIDirectorySelectItems.this.isCaseSensitive();
            }

            @Override
            protected Boolean getDisplayObsoleteEntries() {
                return UIDirectorySelectItems.this.getDisplayObsoleteEntries();
            }

            @Override
            protected String getDirectoryName() {
                return UIDirectorySelectItems.this.getDirectoryName();
            }

        };

        if (Boolean.TRUE.equals(showAll)) {
            setAllValues(f.createAllSelectItems());
            return getAllValues();
        } else {
            Object value = super.getValue();
            return f.createSelectItems(value);
        }
    }

}

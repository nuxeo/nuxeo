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

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;

/**
 * Component that deals with a list of select items from a directory.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIDirectorySelectItems extends UISelectItems {

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
                return Boolean.valueOf(!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return Boolean.TRUE;
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
                return Boolean.valueOf(!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return Boolean.FALSE;
        }
    }

    public void setDisplayObsoleteEntries(Boolean displayObsoleteEntries) {
        this.displayObsoleteEntries = displayObsoleteEntries;
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
            protected Boolean getCaseSensitive() {
                return UIDirectorySelectItems.this.getCaseSensitive();
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
            if (allValues == null) {
                allValues = f.createAllSelectItems();
            }
            return allValues;
        } else {
            Object value = super.getValue();
            return f.createSelectItems(value);
        }
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

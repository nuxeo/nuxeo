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
 * $Id: UIDirectorySelectItem.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.nuxeo.ecm.platform.ui.web.component.UISelectItem;

/**
 * Component that deals with a select item from a directory.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIDirectorySelectItem extends UISelectItem {

    public static final String COMPONENT_TYPE = UIDirectorySelectItem.class.getName();

    protected String directoryName;

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

    @Override
    public Object getValue() {
        Object value = super.getValue();
        return new DirectorySelectItemFactory() {

            @Override
            protected String getVar() {
                return UIDirectorySelectItem.this.getVar();
            }

            @Override
            protected SelectItem createSelectItem() {
                return UIDirectorySelectItem.this.createSelectItem();
            }

            @Override
            protected String getDirectoryName() {
                return UIDirectorySelectItem.this.getDirectoryName();
            }
        }.createSelectItem(value);
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = directoryName;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        directoryName = (String) values[1];
    }

}

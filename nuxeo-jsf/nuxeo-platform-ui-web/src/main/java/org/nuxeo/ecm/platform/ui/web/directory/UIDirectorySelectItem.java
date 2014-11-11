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

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.platform.ui.web.component.UISelectItem;

/**
 * Component that deals with a select item from a directory.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIDirectorySelectItem extends UISelectItem {

    public static final String COMPONENT_TYPE = UIDirectorySelectItem.class.getName();

    enum PropertyKeys {
        directoryName
    }

    // setters & getters

    public String getDirectoryName() {
        return (String) getStateHelper().eval(PropertyKeys.directoryName);
    }

    public void setDirectoryName(String directoryName) {
        getStateHelper().put(PropertyKeys.directoryName, directoryName);
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

}

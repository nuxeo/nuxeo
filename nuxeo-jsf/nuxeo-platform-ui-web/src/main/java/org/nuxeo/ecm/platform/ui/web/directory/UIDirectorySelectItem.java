/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

/*
 * (C) Copyright 20019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Guillaume Renard <grenard@nuxeo.com>
 *
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.Collections;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.ui.web.component.SelectItemComparator;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;

/**
 * Component that deals with a list of select items from a directory.
 *
 * @since 11.1
 */
public class UIUserAndGroupSelectItems extends UISelectItems {

    public static final String COMPONENT_TYPE = UIUserAndGroupSelectItems.class.getName();

    public enum UserAndGroupPropertyKeys {
        itemLabel, directoryName, groupItemLabel, groupDirectoryName
    }

    // setters & getters

    @Override
    public String getItemLabel() {
        return (String) getStateHelper().eval(UserAndGroupPropertyKeys.itemLabel);
    }

    public void setItemLabel(String userLabel) {
        getStateHelper().put(UserAndGroupPropertyKeys.itemLabel, userLabel);
    }

    public String getDirectoryName() {
        return (String) getStateHelper().eval(UserAndGroupPropertyKeys.directoryName);
    }

    public void setDirectoryName(String directoryName) {
        getStateHelper().put(UserAndGroupPropertyKeys.directoryName, directoryName);
    }

    public String getGroupItemLabel() {
        return (String) getStateHelper().eval(UserAndGroupPropertyKeys.groupItemLabel);
    }

    public void setGroupItemLabel(String groupLabel) {
        getStateHelper().put(UserAndGroupPropertyKeys.groupItemLabel, groupLabel);

    }

    public String getGroupDirectoryName() {
        return (String) getStateHelper().eval(UserAndGroupPropertyKeys.groupDirectoryName);
    }

    public void setGroupDirectoryName(String directoryName) {
        getStateHelper().put(UserAndGroupPropertyKeys.groupDirectoryName, directoryName);
    }

    @Override
    public Object getValue() {
        UserAndGroupSelectItemFactory f = new UserAndGroupSelectItemFactory() {

            @Override
            protected String getVar() {
                return UIUserAndGroupSelectItems.this.getVar();
            }

            @Override
            protected SelectItem createSelectItem() {
                return UIUserAndGroupSelectItems.this.createSelectItem();
            }

            @Override
            protected String retrieveSelectEntryId() {
                return UIUserAndGroupSelectItems.this.retrieveSelectEntryId();
            }

            @Override
            public SelectItem createSelectItem(String label) {
                return UIUserAndGroupSelectItems.this.createSelectItem(label);
            }

            @Override
            protected String getItemLabel() {
                return UIUserAndGroupSelectItems.this.getItemLabel();
            }

            @Override
            protected String getDirectoryName() {
                return UIUserAndGroupSelectItems.this.getDirectoryName();
            }

            @Override
            protected String getGroupDirectoryName() {
                 return UIUserAndGroupSelectItems.this.getGroupDirectoryName();
            }

            @Override
            protected String getGroupItemLabel() {
                return UIUserAndGroupSelectItems.this.getGroupItemLabel();
            }

        };
        Object value = getStateHelper().eval(PropertyKeys.value);

        List<SelectItem> items = f.createSelectItems(value);

        String ordering = getOrdering();
        boolean caseSensitive = isCaseSensitive();
        if (!StringUtils.isBlank(ordering)) {
            Collections.sort(items, new SelectItemComparator(ordering, Boolean.valueOf(caseSensitive)));
        }

        return items.toArray(new SelectItem[0]);

    }

    protected String retrieveSelectEntryId() {
        Object itemValue = getItemValue();
        String id = itemValue != null ? itemValue.toString() : null;
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return id;
    }

    protected SelectItem createSelectItem(String label) {
        if (!isItemRendered()) {
            return null;
        }
        Object valueObject = getItemValue();
        String value = valueObject == null ? null : valueObject.toString();
        if (isDisplayIdAndLabel() && label != null) {
            label = value + getDisplayIdAndLabelSeparator() + label;
        }
        // make sure label is never blank
        if (StringUtils.isBlank(label)) {
            label = value;
        }
        String labelPrefix = getItemLabelPrefix();
        if (!StringUtils.isBlank(labelPrefix)) {
            label = labelPrefix + getItemLabelPrefixSeparator() + label;
        }
        String labelSuffix = getItemLabelSuffix();
        if (!StringUtils.isBlank(labelSuffix)) {
            label = label + getItemLabelSuffixSeparator() + labelSuffix;
        }
        return new SelectItem(value, label);
    }
}
